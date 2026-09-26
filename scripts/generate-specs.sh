#!/usr/bin/env bash
#
# Спецификации из кода обоих сервисов и их сравнение с рукописными из docs/.
# Запускается С НОУТБУКА. Результат — docs/generated/.
#
# SpaceMarine: SmallRye сканирует классы при сборке, сервер не нужен.
# Starship: springdoc отдаёт документ только из работающего приложения, поэтому
# оно поднимается локально на профиле openapi (встроенный Tomcat, H2 вместо Postgres).
set -euo pipefail

cd "$(dirname "$0")/.."

OUT="docs/generated"
mkdir -p "$OUT"

echo "==> Генерирую спецификации"
./gradlew -q :spacemarine-service:generateOpenApiSpec :starship-service:generateOpenApiDocs

# Плагины пишут в build/: если отдать им docs/generated как выходной каталог,
# Gradle считает его своим и удаляет оттуда всё постороннее, включая changelog.
cp spacemarine-service/build/generated/openapi/spacemarine-service.yaml "$OUT/"
cp starship-service/build/openapi/starship-service.yaml "$OUT/"

command -v oasdiff >/dev/null || { echo "oasdiff не установлен: brew install oasdiff" >&2; exit 1; }

echo "==> Сравниваю с рукописными"
oasdiff changelog docs/spacemarine-service.yaml "$OUT/spacemarine-service.yaml" \
  -f markdown > "$OUT/spacemarine-service.changelog.md" || true
# Контекст /starship springdoc выносит в servers, а пути пишет без него;
# в рукописной спецификации префикс входит в пути — выравниваем при сравнении.
oasdiff changelog docs/starship-service.yaml "$OUT/starship-service.yaml" \
  --prefix-revision /starship -f markdown > "$OUT/starship-service.changelog.md" || true

echo "==> Готово:"
ls -1 "$OUT"
