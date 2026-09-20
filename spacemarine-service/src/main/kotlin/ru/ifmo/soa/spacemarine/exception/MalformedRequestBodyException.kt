package ru.ifmo.soa.spacemarine.exception

/** Тело запроса не JSON либо тип значения не соответствует схеме. HTTP 400. */
class MalformedRequestBodyException(message: String) : RuntimeException(message)
