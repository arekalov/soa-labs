#!/usr/bin/env bash
# Foreground process; exec preserves the PID for daemon(8) or the caller.
set +x
set -euo pipefail
umask 077
SOA="${SOA:-$HOME/soa}"
export MULE_HOME="${MULE_HOME:-$SOA/opt/mule-standalone-4.11.0}"
export MULE_BASE="$MULE_HOME"
export JAVA_HOME="${MULE_JAVA_HOME:-/usr/local/openjdk17}"
[ -x "$JAVA_HOME/bin/java" ] || { echo 'Mule requires Java 17' >&2; exit 1; }
[ -f "$MULE_HOME/lib/boot/mule-module-reboot-4.11.0.jar" ] || { echo 'Run setup-mule.sh first' >&2; exit 1; }
# shellcheck disable=SC1091
. "$SOA/secrets/soa.env"
: "${SOA_TRUSTSTORE_PATH:?Missing truststore path}"
: "${SOA_TRUSTSTORE_PASSWORD:?Missing truststore password}"
[ -r "$SOA_TRUSTSTORE_PATH" ] || { echo 'Truststore is not readable' >&2; exit 1; }
export SOA_TRUSTSTORE_PATH SOA_TRUSTSTORE_PASSWORD
ulimit -c 0
ulimit -s 2048
cd "$MULE_HOME"
exec "$JAVA_HOME/bin/java" \
  -Xms64m -Xmx256m -Xss512k \
  -XX:ActiveProcessorCount=1 -XX:+UseSerialGC -XX:CICompilerCount=2 \
  -XX:CompressedClassSpaceSize=48m -XX:MaxMetaspaceSize=192m \
  -XX:MaxDirectMemorySize=64m -XX:-HeapDumpOnOutOfMemoryError \
  -Djava.net.preferIPv4Stack=true -Dfile.encoding=UTF-8 \
  -Dlog4j2.disable.jmx=true \
  -Dmule.tlsStores.filesystemLookup.enable=true \
  -Dorg.mule.service.http.impl.service.client.GrizzlyHttpClient.DEFAULT_SELECTOR_THREAD_COUNT=1 \
  -Dorg.mule.service.http.impl.service.server.HttpListenerConnectionManager.DEFAULT_SELECTOR_THREAD_COUNT=1 \
  -Dmule.home="$MULE_HOME" -Dmule.base="$MULE_BASE" \
  -Dmule.bootstrap.container.wrapper.class=org.mule.runtime.module.boot.internal.MuleContainerBasicWrapper \
  --module-path "$MULE_HOME/lib/boot" \
  --add-modules=java.se,org.mule.runtime.boot.log4j,org.mule.runtime.logging,org.mule.runtime.jpms.utils,com.fasterxml.jackson.core \
  --add-opens=java.base/java.lang=org.mule.runtime.jpms.utils \
  --add-opens=java.base/java.lang.reflect=org.mule.runtime.jpms.utils \
  --add-opens=java.base/java.lang.invoke=org.mule.runtime.jpms.utils \
  --add-opens=java.sql/java.sql=org.mule.runtime.jpms.utils \
  --add-opens=java.naming/javax.naming=org.mule.runtime.jpms.utils \
  --module=org.mule.boot/org.mule.runtime.module.reboot.MuleContainerBootstrap
