#!/usr/bin/env bash
# ЛР4: добавить SOAP только в Starship. SpaceMarine и его конфигурацию не трогаем.
# Запускать при остановленном узле: setup-lab4-wildfly.sh starship-1|starship-2
set -euo pipefail
SOA="$HOME/soa"
NODE="${1:?Укажите starship-1 или starship-2}"
case "$NODE" in
  starship-1) OFFSET=19100 ;;
  starship-2) OFFSET=19200 ;;
  *) echo 'Этот скрипт настраивает только Starship' >&2; exit 1 ;;
esac
BASE="$SOA/nodes/$NODE"
PROPERTIES="$SOA/secrets/wildfly-$NODE.properties"
WSDL_PORT="${HAPROXY_PORT:-27343}"
[ -f "$BASE/configuration/standalone.xml" ] || { echo 'Сначала setup-wildfly.sh для узла' >&2; exit 1; }
if pgrep -f "jboss.server.base.dir=$BASE" >/dev/null; then
  echo "Сначала остановите $NODE через soa-control.sh stop $NODE" >&2; exit 1
fi
# Секреты не выводятся ни в stdout, ни в аргументах JVM.
umask 077
TMP_PROPERTIES="$(mktemp)"
CLI="$(mktemp)"
trap 'rm -f "$TMP_PROPERTIES" "$CLI"' EXIT
sed '/^starship.soap.url=/d; /^starship.mule.url=/d; /^soa.mule.url=/d' "$PROPERTIES" > "$TMP_PROPERTIES"
printf '\nstarship.mule.url=https://localhost:27781/starship/operations\nsoa.mule.url=http://127.0.0.1:27780\n' >> "$TMP_PROPERTIES"
cp "$TMP_PROPERTIES" "$PROPERTIES"
[ -f "$BASE/configuration/standalone.pre-lab4.xml" ] || cp "$BASE/configuration/standalone.xml" "$BASE/configuration/standalone.pre-lab4.xml"
cat > "$CLI" <<'CLI'
embed-server --server-config=standalone.xml --std-out=echo
if (outcome != success) of /extension=org.jboss.as.webservices:read-resource
    /extension=org.jboss.as.webservices:add
end-if
if (outcome != success) of /subsystem=webservices:read-resource
    /subsystem=webservices:add(wsdl-host="${jboss.bind.address:127.0.0.1}")
end-if
if (outcome != success) of /subsystem=webservices/endpoint-config=Standard-Endpoint-Config:read-resource
    /subsystem=webservices/endpoint-config=Standard-Endpoint-Config:add
end-if
if (outcome != success) of /subsystem=webservices/client-config=Standard-Client-Config:read-resource
    /subsystem=webservices/client-config=Standard-Client-Config:add
end-if
/subsystem=webservices:write-attribute(name=wsdl-uri-scheme,value=https)
/subsystem=webservices:write-attribute(name=wsdl-host,value=localhost)
# Внешние REST-запросы ждут SOAP; ему нужен независимый пул воркеров.
if (outcome != success) of /subsystem=io/worker=soap:read-resource
    /subsystem=io/worker=soap:add(io-threads=1,task-max-threads=2,task-core-threads=1)
end-if
if (outcome != success) of /socket-binding-group=standard-sockets/socket-binding=soap:read-resource
    /socket-binding-group=standard-sockets/socket-binding=soap:add(port=8444,interface=management)
end-if
if (outcome != success) of /subsystem=undertow/server=default-server/https-listener=soap:read-resource
    /subsystem=undertow/server=default-server/https-listener=soap:add(socket-binding=soap,ssl-context=soaSSC,worker=soap,enable-http2=false)
end-if
# Применять и к существующему worker, а не только при создании.
/subsystem=io/worker=soap:write-attribute(name=task-core-threads,value=1)
/subsystem=io/worker=soap:write-attribute(name=task-max-threads,value=2)
/subsystem=io/worker=default:write-attribute(name=io-threads,value=1)
/subsystem=io/worker=default:write-attribute(name=task-core-threads,value=1)
/subsystem=io/worker=default:write-attribute(name=task-max-threads,value=4)
/subsystem=weld:write-attribute(name=thread-pool-size,value=1)
/subsystem=ejb3/strict-max-bean-instance-pool=slsb-strict-max-pool:write-attribute(name=max-pool-size,value=32)
/subsystem=ejb3/strict-max-bean-instance-pool=slsb-strict-max-pool:write-attribute(name=timeout,value=10)
CLI
# JBossWS читает оба порта даже при HTTPS; явный wsdl-port выключает поиск отсутствующего HTTP listener.
printf '/subsystem=webservices:write-attribute(name=wsdl-port,value=%s)\n/subsystem=webservices:write-attribute(name=wsdl-secure-port,value=%s)\nstop-embedded-server\n' "$((8080 + OFFSET))" "$WSDL_PORT" >> "$CLI"
ulimit -s 2048 2>/dev/null || true
ulimit -c 0 2>/dev/null || true
export JAVA_HOME="${JDK:-/usr/local/openjdk21}"
export JAVA_OPTS="-Xms32m -Xmx256m -Xss256k -XX:ActiveProcessorCount=2 -XX:+UseSerialGC -XX:CICompilerCount=2 -Djboss.server.base.dir=$BASE"
"$SOA/wildfly-home/bin/jboss-cli.sh" --properties="$PROPERTIES" --file="$CLI"
echo "$NODE: SOAP TLS localhost:$((8444 + OFFSET)); Mule 127.0.0.1:27780"
