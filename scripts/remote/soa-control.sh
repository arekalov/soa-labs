#!/usr/bin/env bash
#
# Управление серверами на helios: start | stop | restart | status.
#
# systemd на FreeBSD нет, crontab студентам запрещён, поэтому супервизор —
# /usr/sbin/daemon -r: он поднимет процесс, если тот упадёт. Перезагрузку хоста
# это не переживает, так что перед защитой запускать `status` обязательно.
#
# Процессы гасятся строго по pid-файлам. Соседний скрипт курса BLPS делает
# `pkill -9 java` и цикл по всем процессам пользователя — с ним наши JVM
# умирали бы вместе с чужими.
set -uo pipefail

SOA="$HOME/soa"
SECRETS="$SOA/secrets"
RUN="$SOA/run"
LOGS="$SOA/logs"
NODES_DIR="$SOA/nodes"
JDK="${JDK:-/usr/local/openjdk21}"

WILDFLY_HOME="$SOA/wildfly-home"
OPT="$SOA/opt"
CONSUL_HOME="$SOA/consul"
CONSUL_HTTP="${CONSUL_HTTP:-27500}"
HAPROXY_HOME="$SOA/haproxy"
HAPROXY_PORT="${HAPROXY_PORT:-27343}"

mkdir -p "$RUN" "$LOGS"

# Ключевая настройка для helios. Здесь rlimit stacksize равен 512 МБ, и под стек
# каждого нативного потока резервируется именно столько адресного пространства.
# Две JVM с парой сотен потоков исчерпывают его, после чего fork начинает отказывать
# с "Resource temporarily unavailable", пул соединений не создаёт свой поток,
# а Hibernate падает с "Unable to determine Dialect". Ни один из симптомов
# на настоящую причину не указывает.
ulimit -s 2048 2>/dev/null || true
# Дамп упавшей JVM — это 660 МБ при квоте 1 ГиБ: одним падением выносит весь каталог.
ulimit -c 0 2>/dev/null || true

export JAVA_HOME="$JDK"
export PATH="$JDK/bin:$PATH"

# Общие флаги JVM. Ограничение по числу потоков на пользователя упирается не в память,
# а в счётчик: pthread_create отказывает с EAGAIN при стеке в 1 МБ. Размеры пулов —
# GC, JIT, воркеры Undertow — JVM выводит из числа ядер, а их у helios десятки.
# Говорим ей, что ядра два; SerialGC вдобавок не создаёт потоков сборщика вовсе.
JVM_OPTS="-Xms48m -Xmx320m -Xss256k -XX:CompressedClassSpaceSize=48m -XX:MaxMetaspaceSize=192m"
JVM_OPTS="$JVM_OPTS -XX:ActiveProcessorCount=2 -XX:+UseSerialGC -XX:CICompilerCount=2"
JVM_OPTS="$JVM_OPTS -Djava.net.preferIPv4Stack=true -Dfile.encoding=UTF-8"

# --------------------------------------------------------------------- узлы

node_offset() {  # node_offset <узел>
    case "$1" in
        spacemarine) echo 19000 ;;
        starship-1)  echo 19100 ;;
        starship-2)  echo 19200 ;;
        *) echo "" ;;
    esac
}

node_cert() {  # node_cert <узел>
    case "$1" in
        spacemarine) echo spacemarine ;;
        *) echo starship ;;
    esac
}

node_health() {  # node_health <узел>
    case "$1" in
        spacemarine) echo "/space-marines?size=1" ;;
        *) echo "/starship?size=1" ;;
    esac
}

node_port() { echo $((8443 + $(node_offset "$1"))); }

nodes() {
    local dir name
    for dir in "$NODES_DIR"/*; do
        [ -d "$dir" ] || continue
        name="$(basename "$dir")"
        [ -n "$(node_offset "$name")" ] && echo "$name"
    done
}

# --------------------------------------------------------------------- утилиты

alive() {  # alive <имя>
    local pidfile="$RUN/$1.pid"
    [ -f "$pidfile" ] && pgrep -F "$pidfile" >/dev/null 2>&1
}

app_code() {  # app_code <порт> <сертификат> <путь>
    curl -s -o /dev/null --max-time 5 --cacert "$2" -w "%{http_code}" "https://127.0.0.1:$1$3" 2>/dev/null
}

http_ready() {  # http_ready <порт> <путь>
    curl -s -o /dev/null --max-time 5 "http://127.0.0.1:$1$2" 2>/dev/null
}

wait_ready() {  # wait_ready <узел> <попыток>
    local port cert path attempts="${2:-45}"
    port="$(node_port "$1")"; cert="$SECRETS/$(node_cert "$1").crt"; path="$(node_health "$1")"
    for _ in $(seq 1 "$attempts"); do
        [ "$(app_code "$port" "$cert" "$path")" = "200" ] && return 0
        sleep 2
    done
    return 1
}

stop_one() {  # stop_one <имя> <шаблон-пути-экземпляра>
    local name="$1" pattern="${2:-}" sup="$RUN/$1.sup.pid" pid="$RUN/$1.pid"

    # Сначала супервизор, иначе daemon -r немедленно поднимет процесс обратно.
    if [ -f "$sup" ]; then
        kill "$(cat "$sup")" 2>/dev/null
        sleep 1
    fi
    if [ -f "$pid" ]; then
        kill "$(cat "$pid")" 2>/dev/null
        for _ in $(seq 1 10); do
            pgrep -F "$pid" >/dev/null 2>&1 || break
            sleep 1
        done
        pgrep -F "$pid" >/dev/null 2>&1 && kill -9 "$(cat "$pid")" 2>/dev/null
    fi

    # standalone.sh — обёртка, запускающая java дочерним процессом. daemon пишет pid
    # обёртки, поэтому её смерть оставляет JVM жить и держать порт. Добиваем по пути
    # экземпляра: шаблон узкий, чужие JVM под него не подпадают.
    if [ -n "$pattern" ]; then
        local leftovers
        leftovers="$(pgrep -f "$pattern" 2>/dev/null || true)"
        if [ -n "$leftovers" ]; then
            echo "$leftovers" | xargs kill 2>/dev/null
            sleep 3
            leftovers="$(pgrep -f "$pattern" 2>/dev/null || true)"
            [ -n "$leftovers" ] && echo "$leftovers" | xargs kill -9 2>/dev/null
        fi
    fi

    rm -f "$sup" "$pid"
    echo "  $name остановлен"
}

# -------------------------------------------------------------------- WildFly

start_node() {  # start_node <узел>
    local name="$1" base="$NODES_DIR/$1" offset
    if alive "$name"; then
        echo "  $name уже работает"
        return 0
    fi
    [ -d "$base" ] || { echo "  $name не настроен, пропускаю"; return 0; }
    offset="$(node_offset "$name")"

    # Starship читает адрес БД и truststore из окружения — как раньше делал setenv.sh Tomcat.
    # shellcheck disable=SC1091
    . "$SECRETS/soa.env"
    export SOA_KEYSTORE_PASSWORD SOA_TRUSTSTORE_PATH SOA_TRUSTSTORE_PASSWORD
    export SOA_DB_URL SOA_DB_USER SOA_DB_PASSWORD SOA_SPACEMARINE_BASE_URL

    JAVA_OPTS="$JVM_OPTS" \
    /usr/sbin/daemon -r -f \
        -P "$RUN/$name.sup.pid" -p "$RUN/$name.pid" \
        -o "$LOGS/$name.out" \
        "$WILDFLY_HOME/bin/standalone.sh" \
            -b 0.0.0.0 \
            -Djboss.server.base.dir="$base" \
            -Djboss.socket.binding.port-offset="$offset" \
            --properties="file://$SECRETS/wildfly-$name.properties"

    echo "  $name запускается (https=$((8443 + offset)))"
}

# --------------------------------------------------------- Consul и HAProxy

start_consul() {
    if alive consul; then
        echo "  consul уже работает"
        return 0
    fi
    [ -x "$OPT/bin/consul" ] || { echo "  consul не установлен, пропускаю"; return 0; }

    /usr/sbin/daemon -r -f \
        -P "$RUN/consul.sup.pid" -p "$RUN/consul.pid" \
        -o "$LOGS/consul.out" \
        "$OPT/bin/consul" agent -config-file="$CONSUL_HOME/consul.hcl"

    echo "  consul запускается (http=$CONSUL_HTTP)"
}

start_haproxy() {
    if alive haproxy; then
        echo "  haproxy уже работает"
        return 0
    fi
    [ -x "$OPT/sbin/haproxy" ] || { echo "  haproxy не установлен, пропускаю"; return 0; }

    # -db держит процесс на переднем плане: иначе haproxy демонизируется сам
    # и daemon запишет pid умершего родителя.
    /usr/sbin/daemon -r -f \
        -P "$RUN/haproxy.sup.pid" -p "$RUN/haproxy.pid" \
        -o "$LOGS/haproxy.out" \
        "$OPT/sbin/haproxy" -db -f "$HAPROXY_HOME/haproxy.cfg"

    echo "  haproxy запускается (https=$HAPROXY_PORT)"
}

# ----------------------------------------------------------------------- вывод

show_status() {
    local name proc state code
    printf "%-14s %-12s %s\n" "УЗЕЛ" "ПРОЦЕСС" "HTTPS"
    for name in $(nodes); do
        alive "$name" && proc="работает" || proc="остановлен"
        code="$(app_code "$(node_port "$name")" "$SECRETS/$(node_cert "$name").crt" "$(node_health "$name")")"
        [ "$code" = "200" ] && state="$(node_port "$name") отвечает" || state="$(node_port "$name") не отвечает ($code)"
        printf "%-14s %-12s %s\n" "$name" "$proc" "$state"
    done

    alive consul && proc="работает" || proc="остановлен"
    http_ready "$CONSUL_HTTP" "/v1/status/leader" && state="$CONSUL_HTTP отвечает" || state="$CONSUL_HTTP не отвечает"
    printf "%-14s %-12s %s\n" "consul" "$proc" "$state"

    # Именно 200: при мёртвых узлах балансировщик отвечает своей страницей 503,
    # и проверка «сокет ответил» показала бы работоспособность там, где её нет.
    alive haproxy && proc="работает" || proc="остановлен"
    code="$(app_code "$HAPROXY_PORT" "$SECRETS/starship.crt" "/starship?size=1")"
    [ "$code" = "200" ] && state="$HAPROXY_PORT отвечает" || state="$HAPROXY_PORT не отвечает ($code)"
    printf "%-14s %-12s %s\n" "haproxy" "$proc" "$state"
}

# ------------------------------------------------------------------- диспетчер

case "${1:-status}" in
    start)
        echo "Запуск:"
        start_consul
        # Узлы поднимаются строго по очереди: одновременный старт двух JVM упирается
        # в выделение памяти под стеки потоков, пул соединений не может создать
        # housekeeper-поток, и Hibernate падает с "Unable to determine Dialect".
        # Симптом указывает куда угодно, только не на гонку за ресурсы.
        for node in $(nodes); do
            start_node "$node"
            wait_ready "$node" && echo "  $node отвечает" || echo "  $node не ответил вовремя" >&2
            sleep 3
        done
        start_haproxy
        echo
        show_status
        ;;
    stop)
        echo "Остановка:"
        stop_one haproxy "$HAPROXY_HOME/haproxy.cfg"
        for node in $(nodes); do
            stop_one "$node" "jboss.server.base.dir=$NODES_DIR/$node"
        done
        stop_one consul "$CONSUL_HOME/consul.hcl"
        ;;
    restart)
        "$0" stop
        sleep 2
        "$0" start
        ;;
    status)
        show_status
        ;;
    *)
        echo "Использование: $0 {start|stop|restart|status}" >&2
        exit 1
        ;;
esac
