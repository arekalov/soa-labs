package ru.ifmo.soa.starship.exception

import ru.ifmo.soa.starship.Messages

/** Корабль с таким идентификатором уже существует. HTTP 409. */
class StarshipAlreadyExistsException(id: Long) : RuntimeException(Messages.starshipAlreadyExists(id))
