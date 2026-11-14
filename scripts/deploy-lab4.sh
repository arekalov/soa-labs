#!/usr/bin/env bash
# ЛР4: обновить только Starship и Mule. SpaceMarine/клиент не пересобираются.
set -euo pipefail
cd "$(dirname "$0")/.."
SSH_HOST="${SSH_HOST:-ifmo}"
MVN="${MVN:-mvn}"
./gradlew :starship:ear:ear
"$MVN" -B -f mule/pom.xml -DskipAST=true package
APP=mule/target/soa-mule-proxy-1.0.0-mule-application.jar
[ -f "$APP" ]
ssh "$SSH_HOST" 'mkdir -p ~/soa/bin ~/soa/mule/conf ~/soa/mule/src/main/resources ~/soa/mule/target'
scp scripts/remote/{soa-control,setup-lab4-wildfly,setup-haproxy,setup-mule,run-mule}.sh "$SSH_HOST:soa/bin/"
scp mule/conf/scheduler-pools.conf "$SSH_HOST:soa/mule/conf/"
scp mule/src/main/resources/log4j2.xml "$SSH_HOST:soa/mule/src/main/resources/"
scp "$APP" "$SSH_HOST:soa/mule/target/"
ssh "$SSH_HOST" 'chmod +x ~/soa/bin/*.sh; ~/soa/bin/soa-control.sh stop starship-1; ~/soa/bin/soa-control.sh stop starship-2; ~/soa/bin/soa-control.sh stop mule'
ssh "$SSH_HOST" 'set -e; ~/soa/bin/setup-mule.sh; ~/soa/bin/setup-lab4-wildfly.sh starship-1; ~/soa/bin/setup-lab4-wildfly.sh starship-2'
for node in starship-1 starship-2; do
  scp starship/ear/build/libs/starship.ear "$SSH_HOST:soa/nodes/$node/deployments/.starship.ear.tmp"
  ssh "$SSH_HOST" "set -e; cd ~/soa/nodes/$node/deployments; mv .starship.ear.tmp starship.ear; rm -f starship.ear.failed starship.ear.undeployed; touch starship.ear.dodeploy"
done
# Уже работающие SpaceMarine/Consul не перезапускаются. start идемпотентен.
ssh "$SSH_HOST" 'set -e; ~/soa/bin/soa-control.sh start consul; ~/soa/bin/soa-control.sh start spacemarine; ~/soa/bin/soa-control.sh start mule; ~/soa/bin/soa-control.sh start starship-1; ~/soa/bin/soa-control.sh start starship-2; ~/soa/bin/soa-control.sh stop haproxy; ~/soa/bin/setup-haproxy.sh; ~/soa/bin/soa-control.sh start haproxy'
# Существующий клиент копируется без изменения исходников или bundle.
ssh "$SSH_HOST" 'set -e; mkdir -p ~/public_html/soa/lab4; cp ~/public_html/soa/lab3/index.html ~/public_html/soa/lab3/favicon.svg ~/public_html/soa/lab4/; cp -R ~/public_html/soa/lab3/assets ~/public_html/soa/lab4/; chmod 755 ~/public_html/soa/lab4'
echo 'Стенд запущен. Через SSH-туннель выполните scripts/test-lab4.py и scripts/test-lab4-soap.py.'
