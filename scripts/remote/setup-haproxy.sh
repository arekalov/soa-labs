#!/usr/bin/env bash
#
# Установка HAProxy и конфигурация балансировки узлов Starship. Выполняется НА helios.
#
# Пакет из репозитория портов FreeBSD: собственных сборок upstream не даёт,
# а собирать из исходников незачем. Единственная зависимость pcre2 на helios уже есть.
set -euo pipefail

SOA="$HOME/soa"
OPT="$SOA/opt"
SECRETS="$SOA/secrets"
HAPROXY_HOME="$SOA/haproxy"
VERSION="${HAPROXY_VERSION:-3.4.4}"
PKG_URL="https://pkg.freebsd.org/FreeBSD:14:amd64/latest/All/haproxy-${VERSION}.pkg"

FRONTEND_PORT="${HAPROXY_PORT:-27343}"
STATS_PORT="${HAPROXY_STATS_PORT:-27344}"
NODE1_PORT="${STARSHIP_NODE1_PORT:-27543}"
NODE2_PORT="${STARSHIP_NODE2_PORT:-27643}"

[ -f "$SECRETS/starship.pem" ] || { echo "Нет $SECRETS/starship.pem — выполните setup-secrets.sh" >&2; exit 1; }

mkdir -p "$OPT/sbin" "$HAPROXY_HOME"

if [ -x "$OPT/sbin/haproxy" ]; then
  echo "== haproxy уже установлен: $("$OPT/sbin/haproxy" -v | head -1) =="
else
  echo "== скачиваю и распаковываю haproxy $VERSION =="
  TMP="$(mktemp -d)"
  trap 'rm -rf "$TMP"' EXIT
  curl -fsS -o "$TMP/haproxy.pkg" "$PKG_URL"
  # Пути внутри пакета абсолютные, поэтому распаковываем целиком и берём нужное.
  tar xf "$TMP/haproxy.pkg" -C "$TMP"
  cp "$TMP/usr/local/sbin/haproxy" "$OPT/sbin/haproxy"
  chmod 755 "$OPT/sbin/haproxy"
fi

# Проверка узлов идёт по пути, который отвечает 200 без побочных эффектов.
#
# verify none на узлах вынужденно: сертификаты самоподписанные и помечены BC=ca:false,
# поэтому OpenSSL не может считать их якорем доверия, а другого издателя нет.
# Java-клиенты такой сертификат из truststore принимают, OpenSSL — нет. Трафик
# до узлов всё равно шифруется и не покидает петлю.
cat > "$HAPROXY_HOME/haproxy.cfg" <<EOF
global
    maxconn 256
    log stdout format raw local0 info

defaults
    mode    http
    log     global
    option  httplog
    timeout connect 5s
    timeout client  30s
    timeout server  30s

frontend starship
    bind 0.0.0.0:$FRONTEND_PORT ssl crt $SECRETS/starship.pem
    default_backend starship_nodes

backend starship_nodes
    balance roundrobin
    option httpchk GET /starship?size=1
    http-check expect status 200
    server node1 127.0.0.1:$NODE1_PORT check ssl verify none
    server node2 127.0.0.1:$NODE2_PORT check ssl verify none

listen stats
    bind 127.0.0.1:$STATS_PORT
    stats enable
    stats uri /
    stats refresh 5s
EOF

"$OPT/sbin/haproxy" -c -f "$HAPROXY_HOME/haproxy.cfg"

echo
echo "== готово =="
echo "балансировщик: https://127.0.0.1:$FRONTEND_PORT  узлы: $NODE1_PORT, $NODE2_PORT"
echo "статистика:    http://127.0.0.1:$STATS_PORT"
