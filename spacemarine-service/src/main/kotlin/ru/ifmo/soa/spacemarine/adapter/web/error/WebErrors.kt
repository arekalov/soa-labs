package ru.ifmo.soa.spacemarine.adapter.web.error

/**
 * Параметр запроса не соответствует ожидаемому формату. Отображается в HTTP 400.
 *
 * Отличать от нарушения ограничений целостности (422): здесь значение невозможно даже
 * привести к типу из схемы, а там — значение разобрано, но недопустимо предметно.
 */
class InvalidParameterException(message: String) : RuntimeException(message)

/** Тело запроса не является корректным JSON либо тип значения не соответствует схеме. HTTP 400. */
class MalformedRequestBodyException(message: String) : RuntimeException(message)
