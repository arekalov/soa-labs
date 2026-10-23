#!/usr/bin/env bash
#
# Возвращает стенд в норму после нагрузочных опытов:
#   - пул бинов 32 и таймаут 10 с на обоих узлах Starship;
#   - убирает наливные данные (десантники с именем Load и корабли 1001-1008, 9001).
# Запускать с ноутбука.

echo "== возвращаю пул 32 и таймаут 10 на обоих узлах =="
ssh ifmo '
  S=~/soa/bin/setup-wildfly.sh
  sed "s/name=timeout, value=1)/name=timeout, value=10)/" "$S" > "$S.tmp" && mv "$S.tmp" "$S" && chmod +x "$S"
  ~/soa/bin/soa-control.sh stop
  EJB_POOL_SIZE=32 ~/soa/bin/setup-wildfly.sh starship-1 2>&1 | grep -i strict-max-pool | tail -1
  EJB_POOL_SIZE=32 ~/soa/bin/setup-wildfly.sh starship-2 2>&1 | grep -i strict-max-pool | tail -1
  ~/soa/bin/soa-control.sh start 2>&1 | tail -6
'

echo "== чищу наливные данные из БД =="
ssh ifmo '
  . ~/soa/secrets/soa.env
  PGPASSWORD="$SOA_DB_PASSWORD" psql -h localhost -U "$SOA_DB_USER" -d studs -c \
    "DELETE FROM starship WHERE id IN (1001,1002,1003,1004,1005,1006,1007,1008,9001);
     DELETE FROM space_marine WHERE name = '\''Load'\'';"
'
echo "== готово =="
