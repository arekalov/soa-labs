#!/usr/bin/env bash
#
# Установка и настройка отдельного экземпляра WildFly под сервис SpaceMarine.
# Выполняется НА helios.
#
# Экземпляр свой, а не общий с курсом BLPS: чужой конфиг не трогаем, порты разводим
# смещением. Исходник берём копированием — там уже лежит модуль PostgreSQL.
set -euo pipefail

SOA="$HOME/soa"
SECRETS="$SOA/secrets"
SOURCE_WILDFLY="${SOURCE_WILDFLY:-$HOME/blps/wildfly-39.0.1.Final}"
TARGET="$SOA/wildfly-spacemarine"
JDK="${JDK:-/usr/local/openjdk21}"

# Смещение двигает ВСЕ сокеты разом, включая txn-recovery (4712) и txn-status (4713).
# Про них обычно забывают при ручном подборе портов и потом ловят "Address already in use".
PORT_OFFSET="${PORT_OFFSET:-16000}"   # https -> 24443, management -> 25990

[ -d "$SOURCE_WILDFLY" ] || { echo "Не найден исходный WildFly: $SOURCE_WILDFLY" >&2; exit 1; }
[ -f "$SECRETS/soa.env" ] || { echo "Сначала выполните setup-secrets.sh" >&2; exit 1; }

# shellcheck disable=SC1091
. "$SECRETS/soa.env"

export JAVA_HOME="$JDK"
export PATH="$JDK/bin:$PATH"
# Без явного лимита JVM на helios пытается взять четверть от 128 ГБ и падает.
export JAVA_OPTS="-Xms64m -Xmx512m -Djava.net.preferIPv4Stack=true -Dfile.encoding=UTF-8"

# --- копия экземпляра -------------------------------------------------------
if [ -d "$TARGET" ]; then
  echo "== $TARGET уже существует, пропускаю копирование =="
else
  echo "== копирую WildFly в $TARGET =="
  cp -R "$SOURCE_WILDFLY" "$TARGET"
  # Чужие деплойменты и журналы нам не нужны
  rm -rf "$TARGET"/standalone/deployments/* \
         "$TARGET"/standalone/log/* \
         "$TARGET"/standalone/data/* \
         "$TARGET"/standalone/tmp/* \
         "$TARGET"/wildfly.log
fi

[ -d "$TARGET/modules/org/postgresql" ] \
  || { echo "В копии нет модуля org/postgresql" >&2; exit 1; }

# --- файл свойств для подстановки секретов ----------------------------------
# Передаётся через --properties, а не -D: значения не видны в выводе ps.
cat > "$SECRETS/wildfly.properties" <<EOF
soa.keystore.password=$SOA_KEYSTORE_PASSWORD
soa.db.user=$SOA_DB_USER
soa.db.password=$SOA_DB_PASSWORD
EOF
chmod 600 "$SECRETS/wildfly.properties"

# --- конфигурация -----------------------------------------------------------
CLI_FILE="$(mktemp)"
trap 'rm -f "$CLI_FILE"' EXIT

cat > "$CLI_FILE" <<CLI
embed-server --server-config=standalone.xml --std-out=echo

# --- чужое наследие: датасорс курса BLPS ---
if (outcome == success) of /subsystem=datasources/data-source=BlpsDS:read-resource
    /subsystem=datasources/data-source=BlpsDS:remove
end-if

# --- наш датасорс ---
if (outcome == success) of /subsystem=datasources/data-source=SpaceMarineDS:read-resource
    /subsystem=datasources/data-source=SpaceMarineDS:remove
end-if
/subsystem=datasources/data-source=SpaceMarineDS:add( \\
    jndi-name="java:jboss/datasources/SpaceMarineDS", \\
    driver-name="postgresql", \\
    connection-url="$SOA_DB_URL", \\
    user-name="\\\${soa.db.user}", \\
    password="\\\${soa.db.password}", \\
    min-pool-size=2, \\
    max-pool-size=5, \\
    validate-on-match=true, \\
    background-validation=false, \\
    blocking-timeout-wait-millis=30000, \\
    enabled=true)

# --- TLS: хранилище -> менеджер ключей -> контекст ---
if (outcome == success) of /subsystem=elytron/server-ssl-context=soaSSC:read-resource
    /subsystem=undertow/server=default-server/https-listener=https:undefine-attribute(name=ssl-context)
    /subsystem=elytron/server-ssl-context=soaSSC:remove
end-if
if (outcome == success) of /subsystem=elytron/key-manager=soaKM:read-resource
    /subsystem=elytron/key-manager=soaKM:remove
end-if
if (outcome == success) of /subsystem=elytron/key-store=soaKS:read-resource
    /subsystem=elytron/key-store=soaKS:remove
end-if

/subsystem=elytron/key-store=soaKS:add( \\
    path="$SECRETS/spacemarine.p12", \\
    type=PKCS12, \\
    credential-reference={clear-text="\\\${soa.keystore.password}"})

/subsystem=elytron/key-manager=soaKM:add( \\
    key-store=soaKS, \\
    alias-filter="spacemarine", \\
    credential-reference={clear-text="\\\${soa.keystore.password}"})

/subsystem=elytron/server-ssl-context=soaSSC:add( \\
    key-manager=soaKM, \\
    protocols=["TLSv1.3","TLSv1.2"])

/subsystem=undertow/server=default-server/https-listener=https:write-attribute( \\
    name=ssl-context, value=soaSSC)

# --- зависимости, висящие на HTTP-слушателе ---
# Коннектор remoting подключён к http-listener, а на него, в свою очередь, опирается
# удалённый доступ к EJB. Если просто удалить слушатель, подсистема ejb3 не стартует
# и утащит за собой полтора десятка сервисов — включая регистрацию веб-контекста,
# из-за чего приложение разворачивается, но отвечает 404 на все пути.
# Удалённые EJB спецификацией не предусмотрены, поэтому убираем оба.
if (outcome == success) of /subsystem=ejb3/service=remote:read-resource
    /subsystem=ejb3/service=remote:remove
end-if
if (outcome == success) of /subsystem=remoting/http-connector=http-remoting-connector:read-resource
    /subsystem=remoting/http-connector=http-remoting-connector:remove
end-if

# --- запрет HTTP: слушателя нет, значит и обслуживать некому ---
# Порядок важен: сначала слушатель, потом сокет, иначе "capability in use".
if (outcome == success) of /subsystem=undertow/server=default-server/http-listener=default:read-resource
    /subsystem=undertow/server=default-server/http-listener=default:remove
end-if
if (outcome == success) of /socket-binding-group=standard-sockets/socket-binding=http:read-resource
    /socket-binding-group=standard-sockets/socket-binding=http:remove
end-if
if (outcome == success) of /socket-binding-group=standard-sockets/socket-binding=ajp:read-resource
    /socket-binding-group=standard-sockets/socket-binding=ajp:remove
end-if

# --- освобождаем корень: там развернётся наш WAR с context-root "/" ---
if (outcome == success) of /subsystem=undertow/server=default-server/host=default-host/location=\\/:read-resource
    /subsystem=undertow/server=default-server/host=default-host/location=\\/:remove
end-if

stop-embedded-server
CLI

echo "== применяю конфигурацию =="
cd "$TARGET"
# --properties нужен и здесь, а не только при запуске: встроенный сервер разрешает
# выражения ${...} сразу, на стадии RUNTIME, и без файла свойств добавление
# key-store падает с "Cannot resolve expression".
./bin/jboss-cli.sh --properties="$SECRETS/wildfly.properties" --file="$CLI_FILE"

chmod 600 "$TARGET/standalone/configuration/standalone.xml"

echo
echo "== проверка результата =="
grep -c 'http-listener' "$TARGET/standalone/configuration/standalone.xml" \
  | sed 's/^/http-listener в конфиге (ожидается 0): /'
grep -o 'socket-binding name="https" port="[^"]*"' "$TARGET/standalone/configuration/standalone.xml" || true
grep -o 'server-ssl-context name="soaSSC"[^>]*' "$TARGET/standalone/configuration/standalone.xml" || true
grep -o 'data-source jndi-name="java:jboss/datasources/SpaceMarineDS"' "$TARGET/standalone/configuration/standalone.xml" || true
echo
echo "Порты при смещении $PORT_OFFSET: https=$((8443 + PORT_OFFSET)), management=$((9990 + PORT_OFFSET))"
