#!/usr/bin/env bash
#
# Генерация самоподписанных сертификатов и файла секретов. Выполняется НА helios.
#
# Приватные ключи и пароли рождаются здесь и отсюда не уезжают: ни на ноутбук,
# ни в git. Поэтому скрипт можно коммитить — секретов в нём нет, только логика.
#
# Идемпотентен: повторный запуск ничего не перегенерирует, чтобы не сломать
# уже настроенные серверы. Для пересоздания удалите ~/soa/secrets.
set -euo pipefail

SOA="$HOME/soa"
SECRETS="$SOA/secrets"
JDK="${JDK:-/usr/local/openjdk21}"
KEYTOOL="$JDK/bin/keytool"
CACERTS="$JDK/lib/security/cacerts"

# Имена, под которыми к сервисам будут обращаться. localhost и 127.0.0.1
# обязательны: высокие порты helios закрыты снаружи, и на защите доступ идёт
# через SSH-туннель — без них проверка имени хоста завалится.
SAN='SAN=dns:helios.cs.ifmo.ru,dns:se.ifmo.ru,dns:localhost,ip:127.0.0.1'
# Адрес, по которому Starship зовёт SpaceMarine. Должен совпадать с портом WildFly
# из setup-wildfly.sh: сертификат привязан к имени хоста, а не к порту, поэтому
# рассинхрон проявится не ошибкой TLS, а 503 на посадке десантника.
SPACEMARINE_BASE_URL="${SPACEMARINE_BASE_URL:-https://helios.cs.ifmo.ru:27443}"
DNAME_SUFFIX='OU=SOA Lab2, O=ITMO, L=Saint-Petersburg, C=RU'
VALIDITY_DAYS=825

[ -x "$KEYTOOL" ] || { echo "Не найден keytool: $KEYTOOL" >&2; exit 1; }
[ -r "$CACERTS" ] || { echo "Не найден cacerts: $CACERTS" >&2; exit 1; }

# keytool — это JVM, а на helios 128 ГБ памяти при лимите datasize 32 ГБ:
# без явного -Xmx машина пытается зарезервировать четверть физической памяти
# и падает с "Could not reserve enough space for object heap".
keytool_run() { "$KEYTOOL" -J-Xmx256m "$@"; }

mkdir -p "$SECRETS"
chmod 700 "$SOA" "$SECRETS"

# --- пароли -----------------------------------------------------------------
if [ -f "$SECRETS/soa.env" ]; then
  echo "== soa.env уже существует, пароли сохраняю =="
  # shellcheck disable=SC1091
  . "$SECRETS/soa.env"
else
  echo "== генерирую пароли =="
  KS_PASS="$(openssl rand -base64 24 | tr -d '\n=' )"
  TS_PASS="$(openssl rand -base64 24 | tr -d '\n=' )"

  # Пароль к БД уже лежит в ~/.pgpass — берём оттуда, чтобы не плодить копии.
  DB_PASS="$(awk -F: '$4=="'"$USER"'" {print $5; exit}' "$HOME/.pgpass" 2>/dev/null || true)"
  [ -n "$DB_PASS" ] || { echo "Не удалось прочитать пароль из ~/.pgpass" >&2; exit 1; }

  umask 077
  cat > "$SECRETS/soa.env" <<EOF
# Секреты ЛР2. Создано $(date '+%Y-%m-%d %H:%M:%S'). НЕ КОПИРОВАТЬ С СЕРВЕРА.
SOA_KEYSTORE_PASSWORD='$KS_PASS'
SOA_TRUSTSTORE_PASSWORD='$TS_PASS'
SOA_TRUSTSTORE_PATH='$SECRETS/soa-truststore.p12'
SOA_DB_URL='jdbc:postgresql://pg:5432/studs'
SOA_DB_USER='$USER'
SOA_DB_PASSWORD='$DB_PASS'
SOA_SPACEMARINE_BASE_URL='$SPACEMARINE_BASE_URL'
EOF
  chmod 600 "$SECRETS/soa.env"
  # shellcheck disable=SC1091
  . "$SECRETS/soa.env"
fi

# Пароли переживают повторный запуск, а вот адрес соседа — настройка, а не секрет,
# и меняться может. Синхронизируем его отдельно, иначе смена порта в скриптах
# не доедет до уже настроенного сервера и останется расхождение.
if [ "${SOA_SPACEMARINE_BASE_URL:-}" != "$SPACEMARINE_BASE_URL" ]; then
  echo "== обновляю SOA_SPACEMARINE_BASE_URL -> $SPACEMARINE_BASE_URL =="
  if grep -q '^SOA_SPACEMARINE_BASE_URL=' "$SECRETS/soa.env"; then
    perl -pi -e "s{^SOA_SPACEMARINE_BASE_URL=.*}{SOA_SPACEMARINE_BASE_URL='$SPACEMARINE_BASE_URL'}" \
      "$SECRETS/soa.env"
  else
    echo "SOA_SPACEMARINE_BASE_URL='$SPACEMARINE_BASE_URL'" >> "$SECRETS/soa.env"
  fi
  # shellcheck disable=SC1091
  . "$SECRETS/soa.env"
fi

# --- сертификаты ------------------------------------------------------------
for SVC in spacemarine starship; do
  STORE="$SECRETS/$SVC.p12"
  if [ -f "$STORE" ]; then
    echo "== $SVC.p12 уже существует, пропускаю =="
    continue
  fi

  echo "== создаю сертификат для $SVC =="
  keytool_run -genkeypair \
    -alias "$SVC" \
    -keyalg RSA -keysize 2048 -sigalg SHA256withRSA \
    -validity "$VALIDITY_DAYS" \
    -dname "CN=helios.cs.ifmo.ru, $DNAME_SUFFIX" \
    -ext "$SAN" \
    -ext "KU=digitalSignature,keyEncipherment" \
    -ext "EKU=serverAuth" \
    -ext "BC=ca:false" \
    -storetype PKCS12 \
    -keystore "$STORE" \
    -storepass "$SOA_KEYSTORE_PASSWORD" \
    -keypass "$SOA_KEYSTORE_PASSWORD"
  chmod 600 "$STORE"

  keytool_run -exportcert -rfc \
    -alias "$SVC" -keystore "$STORE" -storetype PKCS12 \
    -storepass "$SOA_KEYSTORE_PASSWORD" \
    -file "$SECRETS/$SVC.crt"
done

# --- truststore -------------------------------------------------------------
# Копия системного cacerts, а не пустое хранилище: иначе сломались бы любые
# обращения к публичным HTTPS, которым доверяют по обычной цепочке.
TRUSTSTORE="$SECRETS/soa-truststore.p12"
if [ -f "$TRUSTSTORE" ]; then
  echo "== truststore уже существует, пропускаю =="
else
  echo "== собираю truststore на основе cacerts =="
  cp "$CACERTS" "$TRUSTSTORE"
  keytool_run -storepasswd -keystore "$TRUSTSTORE" -storetype PKCS12 \
    -storepass changeit -new "$SOA_TRUSTSTORE_PASSWORD"

  for SVC in spacemarine starship; do
    keytool_run -importcert -noprompt \
      -alias "soa-$SVC" -file "$SECRETS/$SVC.crt" \
      -keystore "$TRUSTSTORE" -storetype PKCS12 \
      -storepass "$SOA_TRUSTSTORE_PASSWORD"
  done
  chmod 600 "$TRUSTSTORE"
fi

# --- итог (без секретов) ----------------------------------------------------
echo
echo "== содержимое $SECRETS =="
ls -l "$SECRETS"
echo
echo "== наши записи в truststore =="
keytool_run -list -keystore "$TRUSTSTORE" -storetype PKCS12 \
  -storepass "$SOA_TRUSTSTORE_PASSWORD" 2>/dev/null | grep -i '^soa-' || true
echo
echo "== SAN сертификата spacemarine =="
openssl x509 -in "$SECRETS/spacemarine.crt" -noout -subject -dates -ext subjectAltName
