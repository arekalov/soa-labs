# ЛР4, вариант 111 — отчёт о реализации

Дата проверки: 14 ноября 2026 года. Стенд: helios, ветка `lab4`.

## Соответствие заданию

| Требование | Реализация |
|---|---|
| Второй сервис — SOAP | Starship: JAX-WS endpoint, 8 операций, JAXB DTO, SOAP Fault, WSDL/XSD |
| Сервер приложений | WildFly 39.0.1.Final, Java 21; два узла Starship |
| Первый сервис без изменений | Исходники, контракт и WildFly-конфигурация SpaceMarine не менялись; прежний EAR используется повторно |
| Mule ESB на helios | Mule Kernel 4.11.0, Java 17, HTTP connector 1.12.2; FreeBSD Java bootstrap без native wrapper |
| Межсервисная интеграция через ESB | Starship → Mule `27780` → SpaceMarine HTTPS `27443`; прямого fallback нет |
| Неизменённый клиент | Точная копия прежних `index.html`, `favicon.svg`, `assets` размещена в `/soa/lab4/` |
| REST-прослойка без бизнес-логики | Spring MVC выполняет адаптацию URL/JSON ↔ SOAP; EJB хранит правила и работает с БД |

Код SpaceMarine, общий модуль `soa-api` и исходники клиента не отличаются от `lab3`. Секреты не копировались с helios и не добавлялись в git.

## Проверки

| Проверка | Результат |
|---|---|
| Gradle `:starship:ejb:test :starship:ear:ear` | Успех; 3 теста SOAP-контракта/Fault |
| Maven Mule application packaging | Успех с `-DskipAST=true`; AST/конфигурацию дополнительно проверяет реальный Kernel при deployment |
| Прямой SOAP/WSDL | 32 HTTP-запроса, PASS |
| SOAP через HAProxy | 32 HTTP-запроса, PASS; 8 typed operations, document/literal SOAP 1.1 |
| REST через HAProxy | 71 HTTP-запрос, PASS |
| Mule выключен | 47 HTTP-запросов, PASS: посадка 503, экипаж не изменён, прямой SpaceMarine доступен |
| Полный `scripts/deploy-lab4.sh` | Exit 0, оба Starship и остальные компоненты отвечают; проверен повторный deploy |
| HAProxy при выключенном Mule | Оба backend `UP`, `L7OK`, HTTP 200; бизнес-операция возвращает 503 |
| CORS клиента | Preflight с `Origin: https://se.ifmo.ru` получает 200 и корректные allow-origin/methods/headers |
| Логи ESB | `MULE_PROXY` подтверждает маршрутизацию `200` и `404` SpaceMarine |
| TLS | API-тесты проверяют сертификаты и hostname; `--insecure` не использовался |

REST-проверка включает CRUD, `createWithId`, фильтрацию, несколько полей сортировки, пагинацию, Unicode и XML-символы, некорректные параметры, повторную посадку, посадку на другой корабль, выгрузку, отсутствующих корабль/десантника, удаление и совместимость JSON/HTTP ошибок. Каждый прогон очищает только собственные fixtures.

## Размещение и управление

- Клиент: https://se.ifmo.ru/~s409449/soa/lab4/
- REST Starship через туннель: https://localhost:27343/starship
- SOAP/WSDL: https://localhost:27343/starship-soap/StarshipService?wsdl
- SpaceMarine: https://localhost:27443/space-marines
- Mule health: http://localhost:27780/health
- Статистика HAProxy: http://localhost:27344/
- Consul: http://localhost:27500/ui/

```bash
./scripts/tunnel-lab4.sh
ssh ifmo '~/soa/bin/soa-control.sh status'
./scripts/deploy-lab4.sh
```

Подробные команды, схема и ограничения — [LAB4.md](LAB4.md). Теория к защите — [LAB4-THEORY.md](LAB4-THEORY.md).

## Устранённые проблемы развёртывания

1. Подсистема `webservices` была удалена в ЛР3. Для Starship возвращены подсистема и стандартные endpoint/client configs.
2. JBossWS пытался автоматически найти отсутствующий HTTP listener. Явные WSDL-порты, HTTPS scheme и `CONFIDENTIAL` позволяют оставить внешний API только HTTPS.
3. Второй Starship упирался в native threads при загрузке. Ограничены MSC/ServerService/Weld/HTTP пулы только Starship. Внутренний клиент Mule не создаёт дополнительный async executor.
4. Синхронные REST → SOAP вызовы получили отдельный SOAP worker, исключающий ожидание свободного потока в занятом REST-пуле.
5. Health-check списка кораблей мог ждать зависимый сервис. Проверки Starship переведены на WSDL; `start haproxy` теперь проверяет реальную готовность и возвращает ошибку при сбое.
6. Consul после долгого простоя требовал вмешательства. Служебный metadata-файл сохранён в backup, каталог Raft не удалялся.

## Практические ограничения

Студенческий helios завершает процессы ночью; `daemon -r` не переживает выключение супервизора/хоста. Перед защитой выполнить `start` и `status`.

Самоподписанные сертификаты должны быть доверенными в браузере пользователя, как в ЛР3. Встроенный браузер смог открыть неизменённый клиент, но заблокировал API с `ERR_CERT_AUTHORITY_INVALID`; полноценная UI-проверка в нём не заявляется. Совместимость проверена на уровне реальных контрактов и CORS с TLS validation. Надпись «ЛР3» в UI оставлена, поскольку клиент не модифицировался.

Это компактная учебная конфигурация, а не production-профиль: FreeBSD не входит в официальную матрицу Mule; фактический запуск именно выбранной версии подтверждён на стенде. Пулы ограничены под ресурсы пользователя, а не под высокую нагрузку.
