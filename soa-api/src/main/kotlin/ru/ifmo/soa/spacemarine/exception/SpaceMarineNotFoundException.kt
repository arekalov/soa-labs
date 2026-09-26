package ru.ifmo.soa.spacemarine.exception

import jakarta.ejb.ApplicationException
import ru.ifmo.soa.spacemarine.Messages

/** Элемент отсутствует. HTTP 404. */
@ApplicationException(rollback = true)
class SpaceMarineNotFoundException(id: Int) : RuntimeException(Messages.marineNotFound(id))
