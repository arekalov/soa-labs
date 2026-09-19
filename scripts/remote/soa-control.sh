#!/usr/bin/env bash
#
# Управление серверами ЛР2 на helios: start | stop | restart | status.
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
JDK="${JDK:-/usr/local/openjdk21}"

WILDFLY="$SOA/wildfly-spacemarine"
PORT_OFFSET="${PORT_OFFSET:-16000}"
WILDFLY_HTTPS=$((8443 + PORT_OFFSET))

TOMCAT_HOME="$SOA/tomcat-home"
TOMCAT_BASE="$SOA/base-starship"
TOMCAT_HTTPS="${TOMCAT_HTTPS:-24543}"

mkdir -p "$RUN" "$LOGS"

export JAVA_HOME="$JDK"
export PATH="$JDK/bin:$PATH"

# --------------------------------------------------------------------- утилиты

alive() {  # alive <имя>
    local pidfile="$RUN/$1.pid"
    [ -f "$pidfile" ] && pgrep -F "$pidfile" >/dev/null 2>&1
}

port_open() {  # port_open <порт>
    nc -z -w 2 127.0.0.1 "$1" >/dev/null 2>&1
}

stop_one() {  # stop_one <имя> <шаблон-пути-экземпляра>
    local name="$1"
    local pattern="${2:-}"
    local sup="$RUN/$name.sup.pid"
    local pid="$RUN/$name.pid"

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

    # И standalone.sh, и catalina.sh — это обёртки, которые запускают java
    # дочерним процессом. daemon записывает pid обёртки, поэтому её смерть
    # оставляет JVM жить и держать порт. Добиваем по пути нашего экземпляра:
    # шаблон узкий, чужие JVM (например, курса BLPS) под него не подпадают.
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

start_wildfly() {
    if alive wildfly; then
        echo "  wildfly уже работает"
        return 0
    fi
    [ -d "$WILDFLY" ] || { echo "  wildfly не установлен, пропускаю"; return 0; }

    # Два явных ограничения, без которых JVM на helios не живёт:
    #
    # -Xmx — иначе она просит четверть от 128 ГБ ОЗУ и падает на лимите datasize 32 ГБ.
    #
    # -Xss — здесь rlimit stacksize равен 512 МБ, и JVM отводит столько на стек каждого
    # нативного потока. Пулы WildFly упираются в datasize уже на десятке потоков
    # и валятся с "OutOfMemoryError: unable to create native thread", из-за чего
    # молча не стартуют подсистемы JCA и JPA, то есть датасорс.
    JAVA_OPTS="-Xms64m -Xmx512m -Xss512k -Djava.net.preferIPv4Stack=true -Dfile.encoding=UTF-8" \
    /usr/sbin/daemon -r -f \
        -P "$RUN/wildfly.sup.pid" -p "$RUN/wildfly.pid" \
        -o "$LOGS/wildfly.out" \
        "$WILDFLY/bin/standalone.sh" \
            -b 0.0.0.0 \
            -Djboss.socket.binding.port-offset="$PORT_OFFSET" \
            --properties="file://$SECRETS/wildfly.properties"

    echo "  wildfly запускается (https=$WILDFLY_HTTPS)"
}

# --------------------------------------------------------------------- Tomcat

start_tomcat() {
    if alive tomcat-starship; then
        echo "  tomcat уже работает"
        return 0
    fi
    [ -d "$TOMCAT_BASE" ] || { echo "  tomcat не установлен, пропускаю"; return 0; }

    # Именно `catalina.sh run` (foreground): при `start` Tomcat форкается сам,
    # daemon потеряет процесс и pid-файл станет бесполезен.
    CATALINA_HOME="$TOMCAT_HOME" CATALINA_BASE="$TOMCAT_BASE" \
    CATALINA_OPTS="-Xms64m -Xmx512m -Xss512k -Djava.net.preferIPv4Stack=true -Dfile.encoding=UTF-8" \
    /usr/sbin/daemon -r -f \
        -P "$RUN/tomcat-starship.sup.pid" -p "$RUN/tomcat-starship.pid" \
        -o "$LOGS/tomcat-starship.out" \
        "$TOMCAT_HOME/bin/catalina.sh" run

    echo "  tomcat запускается (https=$TOMCAT_HTTPS)"
}

# ----------------------------------------------------------------------- вывод

show_status() {
    printf "%-18s %-12s %s\n" "СЕРВИС" "ПРОЦЕСС" "ПОРТ"
    for entry in "wildfly:$WILDFLY_HTTPS" "tomcat-starship:$TOMCAT_HTTPS"; do
        local name="${entry%%:*}"
        local port="${entry##*:}"
        local proc port_state
        alive "$name" && proc="работает" || proc="остановлен"
        port_open "$port" && port_state="$port открыт" || port_state="$port закрыт"
        printf "%-18s %-12s %s\n" "$name" "$proc" "$port_state"
    done
}

# ------------------------------------------------------------------- диспетчер

case "${1:-status}" in
    start)
        echo "Запуск:"
        start_wildfly
        start_tomcat
        echo "Ждём готовности..."
        for _ in $(seq 1 30); do
            port_open "$WILDFLY_HTTPS" && break
            sleep 2
        done
        echo
        show_status
        ;;
    stop)
        echo "Остановка:"
        stop_one tomcat-starship "catalina.base=$TOMCAT_BASE"
        stop_one wildfly "jboss.home.dir=$WILDFLY"
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
