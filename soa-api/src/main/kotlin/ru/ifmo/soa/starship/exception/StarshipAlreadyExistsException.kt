package ru.ifmo.soa.starship.exception

import jakarta.ejb.ApplicationException

import ru.ifmo.soa.starship.Messages

/** Корабль с таким идентификатором уже существует. HTTP 409. */
@ApplicationException(rollback = true)
class StarshipAlreadyExistsException(id: Long) : RuntimeException(Messages.starshipAlreadyExists(id))
