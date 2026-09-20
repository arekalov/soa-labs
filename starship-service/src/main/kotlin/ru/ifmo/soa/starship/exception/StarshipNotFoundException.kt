package ru.ifmo.soa.starship.exception

import ru.ifmo.soa.starship.Messages

/** Корабль не найден. HTTP 404. */
class StarshipNotFoundException(id: Long) : RuntimeException(Messages.starshipNotFound(id))
