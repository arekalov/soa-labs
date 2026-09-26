package ru.ifmo.soa.starship.exception

import jakarta.ejb.ApplicationException

/** Параметр запроса не соответствует схеме. HTTP 400. */
@ApplicationException(rollback = true)
class InvalidParameterException(message: String) : RuntimeException(message)
