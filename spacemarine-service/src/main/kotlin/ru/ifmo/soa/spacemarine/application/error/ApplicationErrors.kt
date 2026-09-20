package ru.ifmo.soa.spacemarine.application.error

/** Элемент с указанным идентификатором отсутствует. Отображается в HTTP 404. */
class SpaceMarineNotFoundException(val id: Int) :
    RuntimeException("Элемент с id=$id не найден")
