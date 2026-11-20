# Mule Kernel на helios

Runtime: **Mule Kernel 4.11.0**, Java **17**, HTTP connector **1.12.2**.
Это настоящий Community runtime (CPAL), без Enterprise license и native Tanuki wrapper.

Официальный ZIP (115,7 MiB; распакованный runtime около 131 MiB):

https://repository.mulesoft.org/nexus/service/local/repositories/releases/content/org/mule/distributions/mule-standalone/4.11.0/mule-standalone-4.11.0.zip

SHA256 проверенного скачанного архива:
`ba585d8cca78561a1f0b259fbf1fcc79c63d20c7fc488915443414d6ee6f1e97`.
MD5 совпадает с опубликованным MuleSoft: `9b43eb877226c8586304cbe2f89e9ff9`.

## Сборка и установка

Собирать приложение на ноутбуке, чтобы Maven cache не занимал квоту helios:

```bash
mvn -f mule/pom.xml -DskipAST=true -DskipTests package
```

Результат: `mule/target/soa-mule-proxy-1.0.0-mule-application.jar`.
`skipAST` отключает предварительное построение AST, которое в plugin 4.6.0
пытается скачать закрытый Enterprise BOM 4.11.0. Проверка XML выполняется самим
Kernel при deployment; успешная упаковка сама по себе её не заменяет.
Скопировать `mule/` (без Maven cache) в `~/soa/mule/`, оба скрипта из
`scripts/remote/` в `~/soa/bin/`. На helios:

```bash
~/soa/bin/setup-mule.sh --runtime-only  # можно до окончания сборки APP
~/soa/bin/setup-mule.sh               # после копирования готового APP
~/soa/bin/run-mule.sh                 # foreground, stdout/stderr
```

Если ZIP уже скачан в `/tmp`, передать
`MULE_ARCHIVE=/tmp/mule-standalone-4.11.0.zip` перед командой setup.
Скрипт не удаляет пользовательский архив. Автоматически скачанный временный ZIP
удаляется. Для установки без заранее скачанного ZIP нужно примерно 250 MiB
свободного места плюс приложение; распаковка приложения также потребует места.
Установка: `~/soa/opt/mule-standalone-4.11.0`. Обновлять конфиги лучше при
остановленном Mule. `soa-control.sh` эти файлы не изменяют.

Секреты читаются из `~/soa/secrets/soa.env`:
`SOA_TRUSTSTORE_PATH`, `SOA_TRUSTSTORE_PASSWORD` (PKCS12), `SOA_KEYSTORE_PASSWORD`.
Для HTTPS listener задан `SOA_KEYSTORE_PATH` (по умолчанию `~/soa/secrets/starship.p12`, alias `starship`).
Пароль передаётся через окружение, не через аргументы JVM или XML-файл.
Launcher включает `mule.tlsStores.filesystemLookup.enable=true`: без него
Kernel 4.11 не разрешает загрузку внешнего truststore из файловой системы.
Проверка сертификата не отключается; upstream certificate должен содержать
IP SAN `127.0.0.1` (текущий публичный `certs/spacemarine.crt` содержит).

## Маршруты и проверка

* `GET http://127.0.0.1:27780/health` → локальный `200 ok`, независимо от SpaceMarine.
* `GET /space-marines/{id}` → `https://127.0.0.1:27443`, тот же path и GET.
  Тело и статус upstream сохраняются, включая 400/404/500; redirect не выполняется.
* Ошибка TLS/соединения/таймаут → `502 upstream unavailable`.
* Маршрут SpaceMarine — узкий proxy текущего GET-клиента: не универсальный reverse proxy заголовков,
  cookies, query parameters или других HTTP методов.

* `POST https://localhost:27781/starship/operations/{operation}` — единый JSON → SOAP flow.
  Upstream: HTTPS localhost:27345 → HAProxy → доступный SOAP-узел :27544/:27644.
  Восемь операций: list, getById, create, createWithId, rename, delete, board, unload.
  Преобразования реализованы в `src/main/resources/transforms/request.dwl` и `response.dwl`.
  Core Set Payload загружает DataWeave через `${file::...}`; HTTP Connector отправляет
  SOAP Envelope. Web Service Consumer и Enterprise-компоненты не требуются.
  SOAP Fault превращается в JSON ErrorDto и соответствующий HTTP-статус;
  транспортный сбой — 503. Ответ создаётся в Mule, REST-прослойка его передаёт.
  В логах `MULE_ADAPTER upstream=haproxy-soap operation=board status=200`.

```text
REST-прослойка → JSON → Mule :27781 → SOAP XML → HAProxy :27345 → StarshipEndpoint → StarshipBean
                                                                │
                          SpaceMarine ← Mule :27780 ←────────────┘
```

Оба блока Mule — один runtime. У JSON/SOAP flow и SpaceMarine flow отдельные
лимиты `maxConcurrency=2`. Схемы: [взаимодействие](../README-LAB4-INTERACTION.md).

```bash
curl -i http://127.0.0.1:27780/health
curl -i http://127.0.0.1:27780/space-marines/1
curl -i http://127.0.0.1:27780/space-marines/2147483647
```

stdout содержит `MULE_PROXY method=GET path=/space-marines/1 status=200
upstream=https://127.0.0.1:27443` (фактический статус зависит от данных).
В лог не выводятся тело запроса/ответа, заголовки или секреты.
Health — проверка локального flow, не доступности upstream.

## Запуск и потоки

`run-mule.sh` делает `exec java` на `/usr/local/openjdk17`, используя
`org.mule.runtime.module.boot.internal.MuleContainerBasicWrapper` и JPMS bootstrap.
Native wrapper и его `lib/boot/tanuki` не используются. Для локальной проверки
можно переопределить `SOA`, `MULE_HOME`, `MULE_JAVA_HOME`.

Heap 64–256 MiB, metaspace до 192 MiB, direct memory до 64 MiB, stack 512 KiB,
1 видимый CPU, SerialGC, 2 compiler threads. Общий UBER pool: 2–4 потока;
по одному HTTP selector для сервера и клиента. Логирование синхронное.
Это ограничения отдельных пулов, **не жёсткий лимит всего процесса в 20–30 потоков**:
JVM, deployer и сервисы создают дополнительные потоки. На helios измерено 34; проверять после изменений, например `ps -o pid,nlwp,rss,command -p <MULE_PID>`.

При SM≈52 + двух SS≈120 + Consul≈9 остаётся только около 19 до лимита 200,
ещё до shell/daemon/HAProxy. Начинать с **одного Starship**, затем считать потоки.
Stack/core limits применяются только к дочерней JVM. Автоматические heap/core dumps
отключены; stdout при перенаправлении во внешний файл нужно ротировать.

## Границы подтверждения и источники

Пустой runtime из этого ZIP успешно запущен локально на macOS с Java 17.0.21
и 21.0.13 без Tanuki. Это не доказательство поддержки Java 21 всеми connectors. Для стенда используется Java 17; на helios она уже
установлена. После исследования установка и приложение успешно запущены на helios (FreeBSD 14.5, JDK17): `/health=200`, запрос к SpaceMarine через Mule возвращает его ответ, сквозные REST-тесты прошли.

Финальный APP (около 3,5 MiB) с HTTP connector 1.12.2 проверен локально на Java17:
deployment успешен, `/health` возвращает `200 ok`, HTTPS upstream со своим PKCS12
возвращает 200/404/500 с сохранением тела и статуса; после остановки upstream
proxy возвращает 502. Записи `MULE_PROXY` с path/status подтверждены в stdout.
Для изоляции от SSH tunnels в тестовой копии использовались порты 37780/37443;
готовый JAR содержит требуемые 27780/27443. На helios измерено 34 потока Mule после запуска и запросов.

* [Kernel downloads, размеры и лицензия](https://developer.mulesoft.com/download-mule-esb-runtime/).
* [Требования 4.11](https://docs.mulesoft.com/on-premises/4.11/hardware-and-software-requirements):
  JDK17, официальный ориентир 1 GB RAM / 4 GB disk; лабораторная компактная
  установка ниже дискового ориентира, FreeBSD не входит в перечень протестированных ОС.
* [Java matrix](https://docs.mulesoft.com/general/java-support).
* [Basic wrapper source](https://github.com/mulesoft/mule/blob/master/modules/boot-api/src/main/java/org/mule/runtime/module/boot/internal/MuleContainerBasicWrapper.java).
* [Mule application packaging](https://docs.mulesoft.com/mule-runtime/latest/package-a-mule-application).
* [HTTP connector 1.12.2](https://docs.mulesoft.com/release-notes/connector/connector-http): Java 17.
  Версия 1.10.6 в локальном deployment несовместима с Kernel 4.11:
  `RangeStatusCodeValidator` обращается к отсутствующему `core.api.util.ClassUtils`.
* [HTTP response validator](https://docs.mulesoft.com/http-connector/1.10/http-request-ref).

Страница Kernel содержит уведомление о необходимом security fix, инструкция
доступна после login. Этот фикс отдельно не проверялся; данный ZIP рассматривается
как лабораторный runtime с loopback listener, а не актуальный production baseline.

## Проверка преобразований без REST-прослойки

После запуска `scripts/tunnel-lab4.sh` из корня репозитория:

```bash
python3 scripts/test-lab4-mule.py
```

Каждый прогон проверяет JSON → SOAP → JSON, Unicode/XML-символы, пагинацию,
SOAP Fault и формат ошибок `{code, message, details}`; удаляет свой тестовый корабль.
Полный REST-контракт и успешные посадка/высадка проверяются `scripts/test-lab4.py`.
