package ru.ifmo.soa.starship.exception

import jakarta.ejb.ApplicationException

import ru.ifmo.soa.starship.Messages

/** Десантник уже на борту — этого или другого корабля. HTTP 409. */
@ApplicationException(rollback = true)
class SpaceMarineAlreadyOnBoardException(starshipId: Long, spaceMarineId: Int) :
    RuntimeException(Messages.marineAlreadyOnBoard(starshipId, spaceMarineId))
