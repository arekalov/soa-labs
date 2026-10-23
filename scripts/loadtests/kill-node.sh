#!/usr/bin/env bash
#
# Валит ОДИН узел Starship — бьёт прямо в него, минуя балансировщик.
# Показывает исчерпание пула бинов (узлы урезаны до пула 2, таймаут 1 с).
# Запускать с ноутбука; curl крутится на самом helios. Данные не меняет.
#
#   ./kill-node.sh          # node2, 20 потоков
#   ./kill-node.sh 1        # node1
#   ./kill-node.sh 2 30     # node2, 30 потоков

NODE="${1:-2}"
C="${2:-20}"
N=$(( C * 10 ))
case "$NODE" in
  1) PORT=27543 ;;
  2) PORT=27643 ;;
  *) echo "узел должен быть 1 или 2"; exit 1 ;;
esac

echo "Бью в node$NODE (порт $PORT) напрямую: $N запросов по $C одновременно"
ssh ifmo "seq $N | xargs -P $C -I{} \
  curl -s -k -o /dev/null --max-time 20 -w '%{http_code}\n' \
  'https://127.0.0.1:$PORT/starship?size=50' | sort | uniq -c"
echo "200 — успех, 500 — пул исчерпан (WFLYEJB0378), 000 — таймаут"
