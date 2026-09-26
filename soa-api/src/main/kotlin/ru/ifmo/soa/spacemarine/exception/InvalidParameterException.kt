package ru.ifmo.soa.spacemarine.exception

import jakarta.ejb.ApplicationException

/**
 * Параметр запроса не соответствует формату из схемы. HTTP 400.
 *
 * Отличать от [ValidationException]: здесь значение невозможно привести к типу,
 * там — значение разобрано, но недопустимо предметно.
 */
@ApplicationException(rollback = true)
class InvalidParameterException(message: String) : RuntimeException(message)
