#!/usr/bin/env bash
#
# Сборка и развёртывание обоих веб-сервисов. Запускается С НОУТБУКА.
#
# Собираем локально и отправляем готовые артефакты: на helios нет kotlinc, а общий диск
# делает сборку там мучительно медленной.
#
#   ./scripts/deploy-services.sh [spacemarine|starship|all]
#
# Разворачивать по отдельности приходится из-за квоты в 1 ГиБ на helios: война Starship
# весит 57 МБ, и лишняя выкладка не влезает.
set -euo pipefail

cd "$(dirname "$0")/.."

SSH_HOST="${SSH_HOST:-ifmo}"
WHAT="${1:-all}"
SM_DEPLOY="soa/nodes/spacemarine/deployments"
SS_DEPLOY="soa/nodes/starship-1/deployments"

case "$WHAT" in
  spacemarine|starship|all) ;;
  *) echo "Неизвестная цель «$WHAT» (spacemarine|starship|all)" >&2; exit 1 ;;
esac

SM_EAR="spacemarine/ear/build/libs/spacemarine.ear"
SS_EAR="starship/ear/build/libs/starship.ear"

echo "==> Собираю: $WHAT"
[ "$WHAT" = starship ] || ./gradlew :spacemarine:ear:ear
[ "$WHAT" = spacemarine ] || ./gradlew :starship:ear:ear

echo "==> Останавливаю серверы"
ssh "$SSH_HOST" '~/soa/bin/soa-control.sh stop' || true

echo "==> Копирую артефакты"
# Копируем во временное имя и переименовываем на месте: и сканер WildFly,
# и autoDeploy Tomcat умеют подхватить недокопированный архив и упасть на Invalid zip.
if [ "$WHAT" != starship ]; then
  scp -q "$SM_EAR" "$SSH_HOST:~/$SM_DEPLOY/.spacemarine.ear.tmp"
  ssh "$SSH_HOST" "
    cd ~/$SM_DEPLOY
    rm -rf spacemarine-service.war spacemarine-service.war.* spacemarine.ear spacemarine.ear.*
    mv .spacemarine.ear.tmp spacemarine.ear
  "
fi

if [ "$WHAT" != spacemarine ]; then
  # Старую войну сносим до копирования новой: вдвоём они не помещаются в квоту.
  ssh "$SSH_HOST" "cd ~/$SS_DEPLOY && rm -rf ROOT.war ROOT.war.* starship.war starship.war.* starship.ear starship.ear.*"
  scp -q "$SS_EAR" "$SSH_HOST:~/$SS_DEPLOY/.starship.ear.tmp"
  ssh "$SSH_HOST" "cd ~/$SS_DEPLOY && mv .starship.ear.tmp starship.ear"
fi

echo "==> Запускаю серверы"
# Серверы поднимаются по очереди — так устроен soa-control.sh: одновременный старт
# двух JVM на helios упирается в лимиты выделения памяти под стеки потоков.
ssh "$SSH_HOST" '~/soa/bin/soa-control.sh start'

echo
echo "==> Проверяю, что оба сервиса отвечают"
ssh "$SSH_HOST" '
  ok=0
  for i in $(seq 1 40); do
    sm=$(curl -s -o /dev/null --cacert ~/soa/secrets/spacemarine.crt --max-time 6 \
         -w "%{http_code}" https://127.0.0.1:27443/space-marines 2>/dev/null)
    ss=$(curl -s -o /dev/null --cacert ~/soa/secrets/starship.crt --max-time 6 \
         -X POST -w "%{http_code}" https://127.0.0.1:27543/starship/create/0/x 2>/dev/null)
    if [ "$sm" = "200" ] && [ "$ss" = "400" ]; then ok=1; break; fi
    sleep 5
  done
  echo "SpaceMarine: $sm (ожидается 200)"
  echo "Starship:    $ss (ожидается 400 — проверка валидации)"
  [ "$ok" = "1" ] || { echo "Сервисы не поднялись, смотрите ~/soa/logs/" >&2; exit 1; }
'

echo "==> Развёртывание завершено"
