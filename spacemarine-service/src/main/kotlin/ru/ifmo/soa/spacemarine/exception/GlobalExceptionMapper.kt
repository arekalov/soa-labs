package ru.ifmo.soa.spacemarine.exception

import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.ExceptionMapper
import jakarta.ws.rs.ext.Provider
import ru.ifmo.soa.spacemarine.Messages
import ru.ifmo.soa.spacemarine.dto.ErrorDto
import java.util.logging.Level
import java.util.logging.Logger

private const val BAD_REQUEST = 400
private const val NOT_FOUND = 404
private const val METHOD_NOT_ALLOWED = 405
private const val NOT_ACCEPTABLE = 406
private const val UNSUPPORTED_MEDIA_TYPE = 415
private const val UNPROCESSABLE_ENTITY = 422
private const val INTERNAL_SERVER_ERROR = 500

/**
 * Перевод исключений в коды ответов из спецификации.
 *
 * Разграничение 400 и 422 — главное решение этого класса: 400 означает «значение невозможно
 * привести к типу из схемы», 422 — «значение разобрано, но нарушает ограничения целостности».
 * Поэтому отсутствие обязательного поля в теле даёт именно 422.
 */
@Provider
class GlobalExceptionMapper : ExceptionMapper<Throwable> {
    private val log: Logger = Logger.getLogger(GlobalExceptionMapper::class.java.name)

    override fun toResponse(exception: Throwable): Response {
        exception.find<ValidationException>()?.let {
            return error(UNPROCESSABLE_ENTITY, it.message ?: Messages.CONSTRAINTS_VIOLATED, it.details)
        }
        exception.find<SpaceMarineNotFoundException>()?.let {
            return error(NOT_FOUND, it.message ?: Messages.NOT_FOUND)
        }
        exception.find<InvalidParameterException>()?.let {
            return error(BAD_REQUEST, it.message ?: Messages.BAD_REQUEST)
        }
        exception.find<WebApplicationException>()?.let { return fromContainer(it) }

        log.log(Level.SEVERE, Messages.UNHANDLED_ERROR, exception)
        return error(INTERNAL_SERVER_ERROR, Messages.INTERNAL_ERROR)
    }

    private fun fromContainer(exception: WebApplicationException): Response {
        val status = when (val original = exception.response.status) {
            UNSUPPORTED_MEDIA_TYPE, NOT_ACCEPTABLE -> BAD_REQUEST
            else -> original
        }
        val message = when (status) {
            NOT_FOUND -> Messages.ROUTE_NOT_FOUND
            METHOD_NOT_ALLOWED -> Messages.METHOD_NOT_ALLOWED
            BAD_REQUEST -> Messages.BAD_REQUEST
            else -> Messages.REQUEST_FAILED
        }
        return error(status, message)
    }

    private fun error(status: Int, message: String, details: List<String>? = null): Response =
        Response.status(status)
            .type(MediaType.APPLICATION_JSON)
            .entity(ErrorDto(status, message, details))
            .build()

    private inline fun <reified T : Throwable> Throwable.find(): T? {
        var current: Throwable? = this
        while (current != null) {
            if (current is T) return current
            current = current.cause.takeIf { it !== current }
        }
        return null
    }
}
