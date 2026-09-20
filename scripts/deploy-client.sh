#!/usr/bin/env bash
#
# Сборка и развёртывание клиентского приложения. Запускается С НОУТБУКА.
#
# Собирать на helios нельзя: там нет Node. На сервер уезжает готовая статика,
# а отдаёт её nginx — тот самый, что обслуживает public_html, с валидным сертификатом.
set -euo pipefail

cd "$(dirname "$0")/../client"

SSH_HOST="${SSH_HOST:-ifmo}"
REMOTE_DIR="${REMOTE_DIR:-public_html/soa/lab2}"
DIST="dist"

echo "==> Ставлю зависимости и собираю клиент"
npm ci --no-audit --no-fund
npm run build

[ -f "$DIST/index.html" ] || { echo "В сборке нет index.html" >&2; exit 1; }
echo "==> Размер сборки: $(du -sh "$DIST" | cut -f1)"

# Каталог лабораторной общий: рядом может лежать docs/ со Swagger UI. Поэтому
# подменяем только свои файлы, а не каталог целиком. Сначала ассеты (у них
# хешированные имена, старые и новые не пересекаются), затем index.html.
echo "==> Копирую"
ssh "$SSH_HOST" "mkdir -p ~/$REMOTE_DIR && rm -rf ~/$REMOTE_DIR/assets"
scp -qr "$DIST/assets" "$SSH_HOST:~/$REMOTE_DIR/assets"
scp -q "$DIST/index.html" "$SSH_HOST:~/$REMOTE_DIR/index.html"
ssh "$SSH_HOST" "
  chmod 711 ~ && chmod 755 ~/public_html ~/public_html/soa
  find ~/$REMOTE_DIR -type d -exec chmod 755 {} \\;
  find ~/$REMOTE_DIR -type f -exec chmod 644 {} \\;
"

echo "==> Готово: https://se.ifmo.ru/~s409449/soa/lab2/"
