package ru.ifmo.soa.starship.application.error

/** Корабль с указанным идентификатором не найден. HTTP 404. */
class StarshipNotFoundException(val id: Long) :
    RuntimeException("Корабль с id=$id не найден")

/** Корабль с таким идентификатором уже существует. HTTP 409. */
class StarshipAlreadyExistsException(val id: Long) :
    RuntimeException("Корабль с id=$id уже существует")

/** Десантник не найден в первом сервисе. HTTP 404. */
class SpaceMarineNotFoundException(val id: Int) :
    RuntimeException("Десантник с id=$id не найден")

/** Десантник существует, но находится не на этом корабле. HTTP 404. */
class SpaceMarineNotOnBoardException(val starshipId: Long, val spaceMarineId: Int) :
    RuntimeException("Десантник с id=$spaceMarineId не находится на корабле с id=$starshipId")

/** Десантник уже находится на этом корабле. HTTP 409. */
class SpaceMarineAlreadyOnBoardException(val starshipId: Long, val spaceMarineId: Int) :
    RuntimeException("Десантник с id=$spaceMarineId уже находится на корабле с id=$starshipId")

/** Нарушены ограничения полей корабля. HTTP 422 с перечнем нарушений. */
class StarshipValidationException(val details: List<String>) :
    RuntimeException("Нарушены ограничения целостности класса Starship")

/**
 * Первый сервис недоступен или нарушил контракт. HTTP 503.
 *
 * Сюда же сводятся его ответы 5xx: для нашего клиента «сервис отвечает ошибкой»
 * и «сервис не отвечает» неразличимы по последствиям. Кода 502 в спецификации нет,
 * а 503 описан явно.
 */
class SpaceMarineServiceUnavailableException(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause)

/** Параметр запроса не соответствует схеме. HTTP 400. */
class InvalidParameterException(message: String) : RuntimeException(message)
