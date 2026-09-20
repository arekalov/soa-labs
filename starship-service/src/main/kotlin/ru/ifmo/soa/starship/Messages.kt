package ru.ifmo.soa.starship

/** Тексты, уходящие клиенту. Собраны в одном месте, чтобы формулировки не расходились. */
object Messages {

    const val CLASS_NAME = "Starship"

    const val BAD_REQUEST = "Некорректный запрос"
    const val NOT_FOUND = "Ресурс не найден"
    const val ROUTE_NOT_FOUND = "Запрошенный ресурс не найден"
    const val CONFLICT = "Конфликт состояния"
    const val MALFORMED_JSON = "Тело запроса не является корректным JSON"
    const val WRONG_FORMAT = "Запрос не соответствует ожидаемому формату"
    const val INTERNAL_ERROR = "Внутренняя ошибка сервера"
    const val UNHANDLED_ERROR = "Необработанная ошибка при обслуживании запроса"
    const val CONSTRAINTS_VIOLATED = "Нарушены ограничения целостности класса $CLASS_NAME"
    const val NAME_BLANK = "name: строка не может быть пустой"

    const val UPSTREAM_UNAVAILABLE = "Сервис SpaceMarine недоступен, повторите запрос позже"
    const val UPSTREAM_DOWN = "Первый сервис недоступен"

    fun starshipNotFound(id: Long) = "Корабль с id=$id не найден"
    fun starshipAlreadyExists(id: Long) = "Корабль с id=$id уже существует"
    fun marineNotFound(id: Int) = "Десантник с id=$id не найден"
    fun marineNotOnBoard(starshipId: Long, marineId: Int) =
        "Десантник с id=$marineId не находится на корабле с id=$starshipId"
    fun marineAlreadyOnBoard(starshipId: Long, marineId: Int) =
        "Десантник с id=$marineId уже находится на корабле с id=$starshipId"
    fun unloaded(starshipId: Long, marineId: Int) = "Десантник $marineId высажен с корабля $starshipId"

    fun paramPositiveInt(name: String) = "Параметр '$name' должен быть целым числом больше 0"
    fun paramIntWithMin(name: String, min: Int) = "Параметр '$name' должен быть целым числом не меньше $min"
    fun paramBlank(name: String) = "Параметр '$name' не может быть пустым"
    fun paramUnknownValue(name: String, value: String) = "Параметр '$name' содержит недопустимое значение '$value'"
    fun sortFieldDuplicated(field: String) = "Поле '$field' указано в параметре 'sort' более одного раза"
    fun methodNotAllowed(method: String?) = "Метод $method не поддерживается для этого ресурса"
    fun upstreamUnexpectedStatus(status: Int) = "Сервис SpaceMarine вернул неожиданный статус $status"
    fun upstreamRejectedRequest(status: Int) = "Первый сервис отклонил корректный запрос: $status"
}
