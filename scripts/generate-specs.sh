#!/usr/bin/env bash
#
# Спецификация SpaceMarine из кода и сравнение с рукописной. Запускается С НОУТБУКА.
#
# SmallRye сканирует классы при сборке, сервер не нужен. Для Starship такого способа
# нет: springdoc описывает только работающее приложение, а оно теперь ищет свой
# бизнес-компонент в JNDI и вне контейнера не поднимается.
set -euo pipefail

cd "$(dirname "$0")/.."

OUT="docs/generated"
mkdir -p "$OUT"

echo "==> Генерирую спецификацию"
./gradlew -q :spacemarine:web:generateOpenApiSpec

# Плагин пишет в build/: если отдать ему docs/generated, Gradle сочтёт каталог своим
# и удалит оттуда всё постороннее, включая changelog.
cp spacemarine/web/build/generated/openapi/spacemarine-service.yaml "$OUT/"

command -v oasdiff >/dev/null || { echo "oasdiff не установлен: brew install oasdiff" >&2; exit 1; }

echo "==> Сравниваю с рукописной"
oasdiff changelog docs/spacemarine-service.yaml "$OUT/spacemarine-service.yaml" \
  -f markdown > "$OUT/spacemarine-service.changelog.md" || true

echo "==> Готово:"
ls -1 "$OUT"
