package ru.ifmo.soa.spacemarine.exception

import ru.ifmo.soa.spacemarine.Messages

/** Элемент отсутствует. HTTP 404. */
class SpaceMarineNotFoundException(id: Int) : RuntimeException(Messages.marineNotFound(id))
