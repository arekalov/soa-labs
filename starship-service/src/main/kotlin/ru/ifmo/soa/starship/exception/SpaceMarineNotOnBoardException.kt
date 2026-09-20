package ru.ifmo.soa.starship.exception

import ru.ifmo.soa.starship.Messages

/** Десантник существует, но находится не на этом корабле. HTTP 404. */
class SpaceMarineNotOnBoardException(starshipId: Long, spaceMarineId: Int) :
    RuntimeException(Messages.marineNotOnBoard(starshipId, spaceMarineId))
