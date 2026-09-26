package ru.ifmo.soa.starship.exception

import jakarta.ejb.ApplicationException

import ru.ifmo.soa.starship.Messages

/** Десантник существует, но находится не на этом корабле. HTTP 404. */
@ApplicationException(rollback = true)
class SpaceMarineNotOnBoardException(starshipId: Long, spaceMarineId: Int) :
    RuntimeException(Messages.marineNotOnBoard(starshipId, spaceMarineId))
