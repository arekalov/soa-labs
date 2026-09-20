package ru.ifmo.soa.starship.exception

import ru.ifmo.soa.starship.Messages

/** Нарушены ограничения полей корабля. HTTP 422 с перечнем нарушений. */
class StarshipValidationException(val details: List<String>) : RuntimeException(Messages.CONSTRAINTS_VIOLATED)
