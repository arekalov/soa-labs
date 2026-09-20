package ru.ifmo.soa.starship.exception

/** Параметр запроса не соответствует схеме. HTTP 400. */
class InvalidParameterException(message: String) : RuntimeException(message)
