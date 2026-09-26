package ru.ifmo.soa.starship.exception

import jakarta.ejb.ApplicationException

import ru.ifmo.soa.starship.Messages

/** Десантник не найден в первом сервисе. HTTP 404. */
@ApplicationException(rollback = true)
class SpaceMarineNotFoundException(id: Int) : RuntimeException(Messages.marineNotFound(id))
