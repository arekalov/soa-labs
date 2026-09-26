#!/usr/bin/env bash
#
# Установка WildFly и настройка узла. Выполняется НА helios.
#
#   ./setup-wildfly.sh [spacemarine|starship-1|starship-2]
#
# Установка одна на все узлы, у каждого узла только свой каталог с конфигурацией
# и развёртываниями: при квоте в 1 ГиБ три полных копии по 292 МБ не поместились бы.
#
# Исходник нужен только при первой установке. Рядом с ней остаётся нетронутый
# standalone.xml: из него собираются конфигурации новых узлов, поэтому дистрибутив
# после установки можно удалить.
set -euo pipefail

SOA="$HOME/soa"
SECRETS="$SOA/secrets"
SOURCE_WILDFLY="${SOURCE_WILDFLY:-$HOME/blps/wildfly-39.0.1.Final}"
WILDFLY_HOME="$SOA/wildfly-home"
JDK="${JDK:-/usr/local/openjdk21}"

INSTANCE="${1:-spacemarine}"

# Смещение двигает ВСЕ сокеты разом, включая txn-recovery (4712) и txn-status (4713).
# Про них обычно забывают при ручном подборе портов и потом ловят "Address already in use".
#
# Порты на helios ни за кем не закреплены: прежнее смещение 16000 давало https 24443,
# и его занял чужой ЛР2 — WildFly молча стартовал без HTTPS-слушателя.
case "$INSTANCE" in
  spacemarine) DEFAULT_OFFSET=19000; DEFAULT_CERT=spacemarine; DEFAULT_DS=SpaceMarineDS ;;
  starship-1)  DEFAULT_OFFSET=19100; DEFAULT_CERT=starship;    DEFAULT_DS=StarshipDS ;;
  starship-2)  DEFAULT_OFFSET=19200; DEFAULT_CERT=starship;    DEFAULT_DS=StarshipDS ;;
  *) echo "Неизвестный узел «$INSTANCE» (spacemarine|starship-1|starship-2)" >&2; exit 1 ;;
esac

BASE_DIR="$SOA/nodes/$INSTANCE"
PORT_OFFSET="${PORT_OFFSET:-$DEFAULT_OFFSET}"
CERT="${CERT:-$DEFAULT_CERT}"
DS_NAME="${DS_NAME:-$DEFAULT_DS}"
EJB_POOL_SIZE="${EJB_POOL_SIZE:-32}"

CONSUL_URL="${CONSUL_URL:-http://127.0.0.1:27500}"
SERVICE_NAME="${SERVICE_NAME:-${INSTANCE%%-*}}"
SERVICE_HOST="${SERVICE_HOST:-helios.cs.ifmo.ru}"
SERVICE_PORT="${SERVICE_PORT:-$((8443 + PORT_OFFSET))}"

PROPERTIES="$SECRETS/wildfly-$INSTANCE.properties"

[ -d "$WILDFLY_HOME" ] || [ -d "$SOURCE_WILDFLY" ] \
  || { echo "Нет ни установки $WILDFLY_HOME, ни исходного WildFly $SOURCE_WILDFLY" >&2; exit 1; }
[ -f "$SECRETS/soa.env" ] || { echo "Сначала выполните setup-secrets.sh" >&2; exit 1; }
[ -f "$SECRETS/$CERT.p12" ] || { echo "Нет хранилища $SECRETS/$CERT.p12" >&2; exit 1; }

# shellcheck disable=SC1091
. "$SECRETS/soa.env"

# Дамп упавшей JVM — это 660 МБ при квоте 1 ГиБ: одним падением выносит весь каталог.
ulimit -c 0 2>/dev/null || true

export JAVA_HOME="$JDK"
export PATH="$JDK/bin:$PATH"
# Без явных лимитов JVM на helios берёт четверть от 128 ГБ под кучу и резервирует
# гигабайт под compressed class space — и то и другое упирается в rlimit datasize.
export JAVA_OPTS="-Xms64m -Xmx512m -XX:CompressedClassSpaceSize=64m -XX:MaxMetaspaceSize=192m"
export JAVA_OPTS="$JAVA_OPTS -Djava.net.preferIPv4Stack=true -Dfile.encoding=UTF-8"

# --- общая установка --------------------------------------------------------
if [ -d "$WILDFLY_HOME" ]; then
  echo "== установка $WILDFLY_HOME уже есть =="
else
  echo "== копирую WildFly в $WILDFLY_HOME =="
  cp -R "$SOURCE_WILDFLY" "$WILDFLY_HOME"
  rm -rf "$WILDFLY_HOME"/standalone/deployments/* \
         "$WILDFLY_HOME"/standalone/log/* \
         "$WILDFLY_HOME"/standalone/data/* \
         "$WILDFLY_HOME"/standalone/tmp/* \
         "$WILDFLY_HOME"/docs "$WILDFLY_HOME"/domain "$WILDFLY_HOME"/appclient \
         "$WILDFLY_HOME"/wildfly.log
fi

# Эталон для новых узлов держим нетронутым: если брать за основу уже настроенный
# конфиг, следующий узел унаследует чужой сертификат и датасорс.
TEMPLATE="$WILDFLY_HOME/standalone/configuration/standalone.pristine.xml"
if [ ! -f "$TEMPLATE" ]; then
  cp "$SOURCE_WILDFLY/standalone/configuration/standalone.xml" "$TEMPLATE"
fi

[ -d "$WILDFLY_HOME/modules/org/postgresql" ] \
  || { echo "В установке нет модуля org/postgresql" >&2; exit 1; }

# В копии остаётся standalone.conf курса BLPS со своим смещением портов. Запуск
# добавляет наше через -D, и в командной строке оказываются два разных значения
# одного свойства. Побеждает последнее, но полагаться на это не стоит.
if grep -q 'jboss.socket.binding.port-offset' "$WILDFLY_HOME/bin/standalone.conf"; then
  echo "== убираю чужое смещение портов из standalone.conf =="
  grep -v 'jboss.socket.binding.port-offset' "$WILDFLY_HOME/bin/standalone.conf" > "$WILDFLY_HOME/bin/standalone.conf.tmp"
  mv "$WILDFLY_HOME/bin/standalone.conf.tmp" "$WILDFLY_HOME/bin/standalone.conf"
fi

# --- каталог узла -----------------------------------------------------------
mkdir -p "$BASE_DIR"/configuration "$BASE_DIR"/deployments "$BASE_DIR"/data "$BASE_DIR"/log "$BASE_DIR"/tmp
if [ -f "$BASE_DIR/configuration/standalone.xml" ]; then
  echo "== конфигурация узла $INSTANCE уже есть =="
else
  echo "== создаю каталог узла $INSTANCE =="
  cp "$TEMPLATE" "$BASE_DIR/configuration/standalone.xml"
  cp "$WILDFLY_HOME"/standalone/configuration/*.properties "$BASE_DIR/configuration/" 2>/dev/null || true
fi

# --- файл свойств для подстановки секретов ----------------------------------
# Передаётся через --properties, а не -D: значения не видны в выводе ps.
cat > "$PROPERTIES" <<EOF
soa.keystore.password=$SOA_KEYSTORE_PASSWORD
soa.db.user=$SOA_DB_USER
soa.db.password=$SOA_DB_PASSWORD
soa.consul.url=$CONSUL_URL
soa.service.name=$SERVICE_NAME
soa.service.host=$SERVICE_HOST
soa.service.port=$SERVICE_PORT
EOF
chmod 600 "$PROPERTIES"

# --- конфигурация -----------------------------------------------------------
CLI_FILE="$(mktemp)"
trap 'rm -f "$CLI_FILE"' EXIT

cat > "$CLI_FILE" <<CLI
embed-server --server-config=standalone.xml --std-out=echo

if (outcome == success) of /subsystem=datasources/data-source=BlpsDS:read-resource
    /subsystem=datasources/data-source=BlpsDS:remove
end-if

if (outcome == success) of /subsystem=datasources/data-source=$DS_NAME:read-resource
    /subsystem=datasources/data-source=$DS_NAME:remove
end-if
/subsystem=datasources/data-source=$DS_NAME:add( \\
    jndi-name="java:jboss/datasources/$DS_NAME", \\
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
# Только добавление недостающего. Снос не годится: у https-listener атрибут
# ssl-context обязателен, undefine-attribute отвечает "ssl-context or
# alternative(s) [security-realm] is required" и роняет повторный запуск.
if (outcome == success) of /subsystem=elytron/key-store=soaKS:read-resource
    echo "key-store soaKS уже настроен"
else
    /subsystem=elytron/key-store=soaKS:add(path="$SECRETS/$CERT.p12", type=PKCS12, credential-reference={clear-text="\\\${soa.keystore.password}"})
end-if

if (outcome == success) of /subsystem=elytron/key-manager=soaKM:read-resource
    echo "key-manager soaKM уже настроен"
else
    /subsystem=elytron/key-manager=soaKM:add(key-store=soaKS, alias-filter="$CERT", credential-reference={clear-text="\\\${soa.keystore.password}"})
end-if

if (outcome == success) of /subsystem=elytron/server-ssl-context=soaSSC:read-resource
    echo "ssl-context soaSSC уже настроен"
else
    /subsystem=elytron/server-ssl-context=soaSSC:add(key-manager=soaKM, protocols=["TLSv1.3","TLSv1.2"])
end-if

/subsystem=undertow/server=default-server/https-listener=https:write-attribute( \\
    name=ssl-context, value=soaSSC)

# --- удалённый доступ к EJB ---
# Коннектор remoting из поставки висит на http-listener, а его мы удаляем ниже: HTTP запрещён.
# Пересоздаём коннектор на https-listener — апгрейд протокола работает и поверх TLS.
# Без службы remote вид @Remote не поднимается и всё развёртывание падает с org.wildfly.ejb.remote.
if (outcome == success) of /subsystem=ejb3/service=remote:read-resource
    /subsystem=ejb3/service=remote:remove
end-if
if (outcome == success) of /subsystem=remoting/http-connector=http-remoting-connector:read-resource
    /subsystem=remoting/http-connector=http-remoting-connector:remove
end-if
if (outcome == success) of /subsystem=remoting/http-connector=https-remoting-connector:read-resource
    /subsystem=remoting/http-connector=https-remoting-connector:remove
end-if
/subsystem=remoting/http-connector=https-remoting-connector:add( \\
    connector-ref=https, \\
    sasl-authentication-factory=application-sasl-authentication)
/subsystem=ejb3/service=remote:add( \\
    connectors=[https-remoting-connector], \\
    thread-pool-name=default)

# --- размеры пулов потоков ---
# Два WildFly на helios не уживаются: второй падает с "Failed to start the native thread".
# XNIO выводит число потоков из числа ядер, а их тут десятки.
/subsystem=io/worker=default:write-attribute(name=io-threads, value=2)
/subsystem=io/worker=default:write-attribute(name=task-max-threads, value=8)
/subsystem=ejb3/thread-pool=default:write-attribute(name=max-threads, value=8)

# --- лишние подсистемы ---
# На helios действует лимит ресурсов пользователя, и второй WildFly падает с
# "unable to create native thread". Каждая подсистема тянет свои пулы потоков,
# поэтому всё, чем не пользуемся, убираем. Порядок важен: microprofile-config
# держат остальные подсистемы семейства, его убираем последним.
if (outcome == success) of /subsystem=webservices:read-resource
    /subsystem=webservices:remove
end-if
if (outcome == success) of /subsystem=batch-jberet:read-resource
    /subsystem=batch-jberet:remove
end-if
if (outcome == success) of /subsystem=jsf:read-resource
    /subsystem=jsf:remove
end-if
if (outcome == success) of /subsystem=mail:read-resource
    /subsystem=mail:remove
end-if
if (outcome == success) of /subsystem=jdr:read-resource
    /subsystem=jdr:remove
end-if
if (outcome == success) of /subsystem=sar:read-resource
    /subsystem=sar:remove
end-if
if (outcome == success) of /subsystem=pojo:read-resource
    /subsystem=pojo:remove
end-if
if (outcome == success) of /subsystem=micrometer:read-resource
    /subsystem=micrometer:remove
end-if
if (outcome == success) of /subsystem=microprofile-telemetry-smallrye:read-resource
    /subsystem=microprofile-telemetry-smallrye:remove
end-if
if (outcome == success) of /subsystem=opentelemetry:read-resource
    /subsystem=opentelemetry:remove
end-if
if (outcome == success) of /subsystem=microprofile-jwt-smallrye:read-resource
    /subsystem=microprofile-jwt-smallrye:remove
end-if
if (outcome == success) of /subsystem=health:read-resource
    /subsystem=health:remove
end-if
if (outcome == success) of /subsystem=metrics:read-resource
    /subsystem=metrics:remove
end-if
if (outcome == success) of /subsystem=microprofile-config-smallrye:read-resource
    /subsystem=microprofile-config-smallrye:remove
end-if

# --- пул компонентов EJB ---
# По умолчанию размер выводится из пула воркеров; задание требует настраиваемую мощность,
# поэтому задаём верхнюю границу явно. Экземпляры создаются по мере роста нагрузки,
# а при исчерпании пула вызов ждёт освобождения не дольше таймаута.
/subsystem=ejb3/strict-max-bean-instance-pool=slsb-strict-max-pool:undefine-attribute(name=derive-size)
/subsystem=ejb3/strict-max-bean-instance-pool=slsb-strict-max-pool:write-attribute( \\
    name=max-pool-size, value=$EJB_POOL_SIZE)
/subsystem=ejb3/strict-max-bean-instance-pool=slsb-strict-max-pool:write-attribute( \\
    name=timeout, value=10)
/subsystem=ejb3/strict-max-bean-instance-pool=slsb-strict-max-pool:write-attribute( \\
    name=timeout-unit, value=SECONDS)

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

if (outcome == success) of /subsystem=undertow/server=default-server/host=default-host/location=\\/:read-resource
    /subsystem=undertow/server=default-server/host=default-host/location=\\/:remove
end-if

stop-embedded-server
CLI

echo "== применяю конфигурацию узла $INSTANCE =="
cd "$WILDFLY_HOME"
# --properties нужен и здесь, а не только при запуске: встроенный сервер разрешает
# выражения ${...} сразу, на стадии RUNTIME, и без файла свойств добавление
# key-store падает с "Cannot resolve expression".
JAVA_OPTS="$JAVA_OPTS -Djboss.server.base.dir=$BASE_DIR" \
  ./bin/jboss-cli.sh --properties="$PROPERTIES" --file="$CLI_FILE"

chmod 600 "$BASE_DIR/configuration/standalone.xml"

echo
echo "== проверка результата =="
C="$BASE_DIR/configuration/standalone.xml"
echo "http-listener в конфиге (ожидается 0): $(grep -c 'http-listener' "$C" || true)"
grep -o "key-store name=\"soaKS\" path=\"[^\"]*\"" "$C" || true
grep -o "jndi-name=\"java:jboss/datasources/$DS_NAME\"" "$C" || true
grep -o 'strict-max-pool name="slsb-strict-max-pool"[^/]*' "$C" || true
echo "Узел $INSTANCE: https=$((8443 + PORT_OFFSET)), management=$((9990 + PORT_OFFSET))"
