#!/usr/bin/env bash
#
# Сборка и развёртывание клиентского приложения. Запускается С НОУТБУКА.
#
# Собирать на helios нельзя: там нет Node. На сервер уезжает готовая статика,
# а отдаёт её nginx — тот самый, что обслуживает public_html, с валидным сертификатом.
set -euo pipefail

cd "$(dirname "$0")/../client"

SSH_HOST="${SSH_HOST:-ifmo}"
REMOTE_DIR="${REMOTE_DIR:-public_html/soa/lab2/client}"
DIST="dist"

echo "==> Ставлю зависимости и собираю клиент"
npm ci --no-audit --no-fund
npm run build

[ -f "$DIST/index.html" ] || { echo "В сборке нет index.html" >&2; exit 1; }
echo "==> Размер сборки: $(du -sh "$DIST" | cut -f1)"

# Копируем во временный каталог и подменяем одним движением: иначе посетитель,
# зашедший в момент заливки, получит наполовину обновлённое приложение.
echo "==> Копирую"
ssh "$SSH_HOST" "rm -rf ~/$REMOTE_DIR.new && mkdir -p ~/$REMOTE_DIR.new"
scp -qr "$DIST/." "$SSH_HOST:~/$REMOTE_DIR.new/"
ssh "$SSH_HOST" "
  rm -rf ~/$REMOTE_DIR.old
  [ -d ~/$REMOTE_DIR ] && mv ~/$REMOTE_DIR ~/$REMOTE_DIR.old
  mv ~/$REMOTE_DIR.new ~/$REMOTE_DIR
  rm -rf ~/$REMOTE_DIR.old
  find ~/$REMOTE_DIR -type d -exec chmod 755 {} \;
  find ~/$REMOTE_DIR -type f -exec chmod 644 {} \;
"

echo "==> Готово: https://se.ifmo.ru/~s409449/soa/lab2/client/"
