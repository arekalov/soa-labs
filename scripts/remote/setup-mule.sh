#!/usr/bin/env bash
# Run on helios after copying mule/ and the locally built application to ~/soa/mule/.
set -euo pipefail
umask 077
SOA="${SOA:-$HOME/soa}"
VERSION=4.11.0
DEST="$SOA/opt/mule-standalone-$VERSION"
SOURCE="${MULE_SOURCE:-$SOA/mule}"
RUNTIME_ONLY=false
if [ "${1:-}" = --runtime-only ]; then RUNTIME_ONLY=true; shift; fi
APP="${1:-$SOURCE/target/soa-mule-proxy-1.0.0-mule-application.jar}"
URL="https://repository.mulesoft.org/nexus/service/local/repositories/releases/content/org/mule/distributions/mule-standalone/$VERSION/mule-standalone-$VERSION.zip"
EXPECTED=ba585d8cca78561a1f0b259fbf1fcc79c63d20c7fc488915443414d6ee6f1e97
$RUNTIME_ONLY || [ -f "$APP" ] || { echo "Build locally and copy application: $APP" >&2; exit 1; }
[ -f "$SOURCE/conf/scheduler-pools.conf" ] || { echo 'Copy mule/conf first' >&2; exit 1; }
[ -f "$SOA/secrets/soa.env" ] || { echo 'Missing secrets/soa.env' >&2; exit 1; }
mkdir -p "$SOA/opt"
if [ ! -d "$DEST" ]; then
  STAGE="$(mktemp -d "$SOA/opt/.mule-install.XXXXXX")"
  trap 'rm -rf "$STAGE"' EXIT
  ARCHIVE="${MULE_ARCHIVE:-$STAGE/runtime.zip}"
  if [ -z "${MULE_ARCHIVE:-}" ]; then
    curl -fL --connect-timeout 15 --max-time 300 --speed-time 30 --speed-limit 1024 --retry 2 "$URL" -o "$ARCHIVE"
  fi
  if command -v sha256 >/dev/null 2>&1; then
    ACTUAL="$(sha256 -q "$ARCHIVE")"
  else
    ACTUAL="$(shasum -a 256 "$ARCHIVE" | awk '{print $1}')"
  fi
  [ "$ACTUAL" = "$EXPECTED" ] || { echo 'Runtime SHA256 mismatch' >&2; exit 1; }
  unzip -q "$ARCHIVE" -d "$STAGE"
  mv "$STAGE/mule-standalone-$VERSION" "$DEST"
fi
[ -f "$DEST/lib/boot/mule-module-reboot-$VERSION.jar" ] || { echo 'Incomplete runtime installation' >&2; exit 1; }
cp "$SOURCE/conf/scheduler-pools.conf" "$DEST/conf/scheduler-pools.conf"
# Synchronous console logger avoids an extra asynchronous logging thread.
cp "$SOURCE/src/main/resources/log4j2.xml" "$DEST/conf/log4j2.xml"
if ! $RUNTIME_ONLY; then
  cp "$APP" "$DEST/apps/soa-mule-proxy.jar.tmp"
  mv "$DEST/apps/soa-mule-proxy.jar.tmp" "$DEST/apps/soa-mule-proxy.jar"
fi
echo "Installed Mule $VERSION into $DEST; start with run-mule.sh"
du -sh "$DEST"
