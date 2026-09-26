package ru.ifmo.soa.spacemarine.exception

import jakarta.ejb.ApplicationException
import ru.ifmo.soa.spacemarine.Messages

/**
 * Нарушены ограничения целостности класса. HTTP 422.
 *
 * Нарушения собираются все разом: спецификация требует массив `details`, а пользователю
 * полезно увидеть весь список сразу, а не по одному за запрос.
 */
@ApplicationException(rollback = true)
class ValidationException(val details: List<String>) : RuntimeException(Messages.CONSTRAINTS_VIOLATED) {
    init {
        require(details.isNotEmpty()) { "Нельзя сообщить о нарушениях, не указав ни одного" }
    }
}
