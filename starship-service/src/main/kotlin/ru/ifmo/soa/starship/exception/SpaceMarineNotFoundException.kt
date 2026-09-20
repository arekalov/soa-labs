package ru.ifmo.soa.starship.exception

import ru.ifmo.soa.starship.Messages

/** Десантник не найден в первом сервисе. HTTP 404. */
class SpaceMarineNotFoundException(id: Int) : RuntimeException(Messages.marineNotFound(id))
