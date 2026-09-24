#!/usr/bin/env bash
#
# Сборка и развёртывание обоих веб-сервисов. Запускается С НОУТБУКА.
#
# Собираем локально и отправляем готовые WAR: на helios нет kotlinc, а общий диск
# делает сборку там мучительно медленной.
set -euo pipefail

cd "$(dirname "$0")/.."

SSH_HOST="${SSH_HOST:-ifmo}"
WILDFLY_DEPLOY="soa/wildfly-spacemarine/standalone/deployments"
TOMCAT_WEBAPPS="soa/base-starship/webapps"

echo "==> Собираю сервисы"
./gradlew :spacemarine-service:war :starship-service:bootWar

SM_WAR="spacemarine-service/build/libs/spacemarine-service.war"
SS_WAR="starship-service/build/libs/starship.war"
[ -f "$SM_WAR" ] && [ -f "$SS_WAR" ] || { echo "Не найдены артефакты сборки" >&2; exit 1; }

echo "==> Останавливаю серверы"
ssh "$SSH_HOST" '~/soa/bin/soa-control.sh stop' || true

echo "==> Копирую артефакты"
# Копируем во временное имя и переименовываем на месте: и сканер WildFly,
# и autoDeploy Tomcat умеют подхватить недокопированный архив и упасть на Invalid zip.
scp -q "$SM_WAR" "$SSH_HOST:~/$WILDFLY_DEPLOY/.spacemarine-service.war.tmp"
scp -q "$SS_WAR" "$SSH_HOST:~/$TOMCAT_WEBAPPS/.starship.war.tmp"

ssh "$SSH_HOST" "
  cd ~/$WILDFLY_DEPLOY
  rm -f spacemarine-service.war spacemarine-service.war.*
  mv .spacemarine-service.war.tmp spacemarine-service.war

  cd ~/$TOMCAT_WEBAPPS
  rm -rf starship starship.war
  mv .starship.war.tmp starship.war
"

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
