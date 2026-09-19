package ru.ifmo.soa.spacemarine.application.error

/** Элемент с указанным идентификатором отсутствует. Отображается в HTTP 404. */
class SpaceMarineNotFoundException(val id: Int) :
    RuntimeException("Элемент с id=$id не найден")

/**
 * Коллекция пуста, поэтому запрошенный агрегат не определён.
 *
 * Спецификация описывает этот случай отдельно для `GET /space-marines/health/min`
 * и требует именно 404, а не пустой 200.
 */
class EmptyCollectionException :
    RuntimeException("Коллекция пуста")
