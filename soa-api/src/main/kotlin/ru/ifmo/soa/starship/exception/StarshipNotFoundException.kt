package ru.ifmo.soa.starship.exception

import jakarta.ejb.ApplicationException

import ru.ifmo.soa.starship.Messages

/** Корабль не найден. HTTP 404. */
@ApplicationException(rollback = true)
class StarshipNotFoundException(id: Long) : RuntimeException(Messages.starshipNotFound(id))
