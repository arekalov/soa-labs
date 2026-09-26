#!/usr/bin/env bash
#
# Установка Consul и конфигурация одноузлового реестра. Выполняется НА helios.
#
# HashiCorp сборок под FreeBSD не публикует, зато есть готовый пакет в репозитории
# портов. Пакет — обычный tar-архив, распаковывается в домашний каталог без root.
set -euo pipefail

SOA="$HOME/soa"
OPT="$SOA/opt"
CONSUL_HOME="$SOA/consul"
VERSION="${CONSUL_VERSION:-2.0.4}"
PKG_URL="https://pkg.freebsd.org/FreeBSD:14:amd64/latest/All/consul-${VERSION}.pkg"

HTTP_PORT="${CONSUL_HTTP_PORT:-27500}"
SERVER_PORT="${CONSUL_SERVER_PORT:-27510}"
SERF_LAN_PORT="${CONSUL_SERF_LAN_PORT:-27511}"
SERF_WAN_PORT="${CONSUL_SERF_WAN_PORT:-27512}"
DNS_PORT="${CONSUL_DNS_PORT:-27513}"

mkdir -p "$OPT" "$CONSUL_HOME/data"

if [ -x "$OPT/bin/consul" ]; then
  echo "== consul уже установлен: $("$OPT/bin/consul" version | head -1) =="
else
  echo "== скачиваю и распаковываю consul $VERSION =="
  TMP="$(mktemp -d)"
  trap 'rm -rf "$TMP"' EXIT
  curl -fsS -o "$TMP/consul.pkg" "$PKG_URL"
  # Пути внутри пакета абсолютные, поэтому распаковываем целиком и берём нужное.
  tar xf "$TMP/consul.pkg" -C "$TMP"
  mkdir -p "$OPT/bin"
  cp "$TMP/usr/local/bin/consul" "$OPT/bin/consul"
  chmod 755 "$OPT/bin/consul"
fi

# Слушаем только петлю: наружу helios высокие порты всё равно не пускает,
# а реестр нужен лишь сервисам на этой же машине.
cat > "$CONSUL_HOME/consul.hcl" <<EOF
datacenter = "soa"
node_name  = "helios"
data_dir   = "$CONSUL_HOME/data"

server           = true
bootstrap_expect = 1
bind_addr        = "127.0.0.1"
client_addr      = "127.0.0.1"

ui_config {
  enabled = true
}

ports {
  http     = $HTTP_PORT
  https    = -1
  server   = $SERVER_PORT
  serf_lan = $SERF_LAN_PORT
  serf_wan = $SERF_WAN_PORT
  dns      = $DNS_PORT
  grpc     = -1
  grpc_tls = -1
}
EOF

echo
echo "== готово =="
"$OPT/bin/consul" version | head -1
echo "HTTP API и UI: http://127.0.0.1:$HTTP_PORT"
