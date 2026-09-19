#!/usr/bin/env bash
#
# Развёртывание веб-документации Swagger UI. Запускается С НОУТБУКА.
#
#   ./scripts/deploy-docs.sh [lab2]
#
# Наследник deploy.sh из ЛР1. Отличий два: имя лабораторной стало параметром,
# и появилась проверка этого имени.
set -euo pipefail

cd "$(dirname "$0")/.."

SSH_HOST="${SSH_HOST:-ifmo}"
LAB="${1:-lab2}"

# Без этой проверки пустое значение превратило бы команду ниже в
# `rm -rf ~/public_html/soa/` и снесло бы вместе с ЛР2 ещё и ЛР1.
case "$LAB" in
  lab[0-9]) ;;
  *) echo "Отказ: недопустимое имя лабораторной «$LAB» (ожидается вида lab2)" >&2; exit 1 ;;
esac

REMOTE_DIR="public_html/soa/$LAB"

[ -d docs ] || { echo "Не найден каталог docs/" >&2; exit 1; }

echo "==> Пересоздаю ~/$REMOTE_DIR"
ssh "$SSH_HOST" "
  rm -rf ~/$REMOTE_DIR
  mkdir -p ~/$REMOTE_DIR
  chmod 711 ~
  chmod 755 ~/public_html ~/public_html/soa
"

echo "==> Копирую спецификации и Swagger UI"
scp -qr docs/. "$SSH_HOST:$REMOTE_DIR/"

echo "==> Выставляю права"
ssh "$SSH_HOST" "
  find ~/$REMOTE_DIR -type d -exec chmod 755 {} \;
  find ~/$REMOTE_DIR -type f -exec chmod 644 {} \;
"

echo "==> Готово: https://se.ifmo.ru/~s409449/soa/$LAB/"
