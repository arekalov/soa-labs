package ru.ifmo.soa.spacemarine.exception

/**
 * Параметр запроса не соответствует формату из схемы. HTTP 400.
 *
 * Отличать от [ValidationException]: здесь значение невозможно привести к типу,
 * там — значение разобрано, но недопустимо предметно.
 */
class InvalidParameterException(message: String) : RuntimeException(message)
