package ru.ifmo.soa.starship.exception

import ru.ifmo.soa.starship.Messages

/** Десантник уже на борту — этого или другого корабля. HTTP 409. */
class SpaceMarineAlreadyOnBoardException(starshipId: Long, spaceMarineId: Int) :
    RuntimeException(Messages.marineAlreadyOnBoard(starshipId, spaceMarineId))
