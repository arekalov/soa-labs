package ru.ifmo.soa.spacemarine

import ru.ifmo.soa.spacemarine.model.AstartesCategory
import ru.ifmo.soa.spacemarine.model.MAX_COORDINATE_Y

private val CATEGORIES = AstartesCategory.entries.joinToString(", ") { it.name }

/**
 * Тексты, уходящие клиенту. Собраны в одном месте, чтобы формулировки не расходились
 * между контроллером, сервисом и обработчиком ошибок.
 */
object Messages {
    const val CLASS_NAME = "SpaceMarine"

    const val BAD_REQUEST = "Некорректный запрос"
    const val MALFORMED_JSON = "Тело запроса не является корректным JSON либо не соответствует ожидаемой схеме"
    const val NOT_FOUND = "Элемент не найден"
    const val ROUTE_NOT_FOUND = "Запрошенный ресурс не найден"
    const val METHOD_NOT_ALLOWED = "Метод не поддерживается для этого ресурса"
    const val REQUEST_FAILED = "Ошибка обработки запроса"
    const val INTERNAL_ERROR = "Внутренняя ошибка сервера"
    const val UNHANDLED_ERROR = "Необработанная ошибка при обслуживании запроса"
    const val CONSTRAINTS_VIOLATED = "Нарушены ограничения целостности класса $CLASS_NAME"

    fun marineNotFound(id: Int) = "Элемент с id=$id не найден"

    const val FIELD_NULL = "поле не может быть null"
    const val FIELD_BLANK = "строка не может быть пустой"
    const val FIELD_NOT_POSITIVE = "значение должно быть больше 0"
    const val FIELD_NOT_NUMBER = "значение должно быть числом"
    val FIELD_Y_TOO_BIG = "максимальное значение поля — ${MAX_COORDINATE_Y.toInt()}"

    fun violation(path: String, message: String) = "$path: $message"

    fun paramPositiveInt(name: String) = "Параметр '$name' должен быть целым числом больше 0"
    fun paramInt(name: String) = "Параметр '$name' должен быть целым числом"
    fun paramIntWithMin(name: String, min: Int) = "Параметр '$name' должен быть целым числом не меньше $min"
    fun paramNumber(name: String) = "Параметр '$name' должен быть числом"
    fun paramPositiveNumber(name: String) = "Параметр '$name' должен быть числом больше 0"
    fun paramMax(name: String, max: Int) = "Параметр '$name' не может превышать $max"
    fun paramBoolean(name: String) = "Параметр '$name' должен быть true или false"
    fun paramInstant(name: String) =
        "Параметр '$name' должен быть датой и временем в формате ISO-8601, например 2026-09-10T12:00:00Z"
    fun paramBlank(name: String) = "Параметр '$name' не может быть пустым"
    fun paramRequired(name: String) = "Параметр '$name' обязателен и не может быть пустым"
    fun paramDuplicated(name: String) = "Параметр '$name' указан более одного раза"
    fun paramUnknownValue(name: String, value: String) = "Параметр '$name' содержит недопустимое значение '$value'"
    fun sortFieldDuplicated(field: String) = "Поле '$field' указано в параметре 'sort' более одного раза"
    fun paramOneOf(name: String) = "Параметр '$name' должен быть одним из: $CATEGORIES"
}
