package ru.ifmo.soa.starship.exception

/**
 * Первый сервис недоступен или нарушил контракт. HTTP 503.
 *
 * Сюда же сводятся его ответы 5xx: для нас «сервис отвечает ошибкой» и «сервис не отвечает»
 * неразличимы по последствиям. Кода 502 в спецификации нет, а 503 описан явно.
 */
class SpaceMarineServiceUnavailableException(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause)
