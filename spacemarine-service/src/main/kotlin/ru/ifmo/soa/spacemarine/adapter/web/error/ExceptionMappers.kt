package ru.ifmo.soa.spacemarine.adapter.web.error

import com.fasterxml.jackson.core.JsonProcessingException
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.ExceptionMapper
import jakarta.ws.rs.ext.Provider
import ru.ifmo.soa.spacemarine.adapter.web.dto.ErrorDto
import ru.ifmo.soa.spacemarine.application.error.SpaceMarineNotFoundException
import ru.ifmo.soa.spacemarine.domain.validation.DomainValidationException
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Перевод исключений в коды ответов, описанные спецификацией.
 *
 * Разграничение 400 и 422 — главное решение этого файла:
 * 400 означает «значение невозможно привести к типу из схемы»,
 * 422 — «значение разобрано, но нарушает ограничения целостности класса».
 * Поэтому отсутствие обязательного поля в теле даёт именно 422, а не 400.
 *
 * JAX-RS выбирает наиболее специфичный маппер, поэтому запасной вариант на [Exception]
 * не перехватывает то, что покрыто остальными.
 */
/** Коды заданы числами: [Response.Status] не содержит 422 в этой версии Jakarta REST. */
private object HttpCodes {
    const val BAD_REQUEST = 400
    const val NOT_FOUND = 404
    const val NOT_ACCEPTABLE = 406
    const val UNSUPPORTED_MEDIA_TYPE = 415
    const val UNPROCESSABLE_ENTITY = 422
    const val METHOD_NOT_ALLOWED = 405
    const val INTERNAL_SERVER_ERROR = 500
}

private fun errorResponse(
    status: Int,
    message: String,
    details: List<String>? = null,
): Response = Response.status(status)
    .type(MediaType.APPLICATION_JSON)
    .entity(ErrorDto(status, message, details))
    .build()

@Provider
class InvalidParameterExceptionMapper : ExceptionMapper<InvalidParameterException> {
    override fun toResponse(exception: InvalidParameterException): Response =
        errorResponse(HttpCodes.BAD_REQUEST, exception.message ?: "Некорректный запрос")
}

@Provider
class MalformedRequestBodyExceptionMapper : ExceptionMapper<MalformedRequestBodyException> {
    override fun toResponse(exception: MalformedRequestBodyException): Response =
        errorResponse(HttpCodes.BAD_REQUEST, exception.message ?: "Некорректное тело запроса")
}

/** Тело не разобралось как JSON либо тип значения не совпал с типом поля. */
@Provider
class JsonProcessingExceptionMapper : ExceptionMapper<JsonProcessingException> {
    override fun toResponse(exception: JsonProcessingException): Response =
        errorResponse(
            HttpCodes.BAD_REQUEST,
            "Тело запроса не является корректным JSON либо не соответствует ожидаемой схеме",
        )
}

@Provider
class DomainValidationExceptionMapper : ExceptionMapper<DomainValidationException> {
    override fun toResponse(exception: DomainValidationException): Response =
        errorResponse(
            HttpCodes.UNPROCESSABLE_ENTITY,
            exception.message ?: "Нарушены ограничения целостности",
            exception.details,
        )
}

@Provider
class SpaceMarineNotFoundExceptionMapper : ExceptionMapper<SpaceMarineNotFoundException> {
    override fun toResponse(exception: SpaceMarineNotFoundException): Response =
        errorResponse(HttpCodes.NOT_FOUND, exception.message ?: "Элемент не найден")
}

/**
 * Исключения самого JAX-RS: несуществующий маршрут, неподходящий Content-Type и прочее.
 *
 * Статус сохраняется, но тело подменяется на схему `Error` — иначе клиент получил бы
 * HTML-страницу сервера вместо JSON. Коды 415 и 406 переводятся в 400: спецификация их
 * не описывает, а по смыслу это «запрос не соответствует ожидаемому формату».
 */
@Provider
class WebApplicationExceptionMapper : ExceptionMapper<WebApplicationException> {

    override fun toResponse(exception: WebApplicationException): Response {
        val status = when (exception.response.status) {
            HttpCodes.UNSUPPORTED_MEDIA_TYPE, HttpCodes.NOT_ACCEPTABLE -> HttpCodes.BAD_REQUEST
            else -> exception.response.status
        }

        return errorResponse(status, defaultMessage(status))
    }

    private fun defaultMessage(status: Int): String = when (status) {
        HttpCodes.NOT_FOUND -> "Запрошенный ресурс не найден"
        HttpCodes.METHOD_NOT_ALLOWED -> "Метод не поддерживается для этого ресурса"
        HttpCodes.BAD_REQUEST -> "Некорректный запрос"
        else -> "Ошибка обработки запроса"
    }
}

/** Всё непредусмотренное. Тело наружу не раскрываем, стектрейс пишем в журнал сервера. */
@Provider
class FallbackExceptionMapper : ExceptionMapper<Exception> {

    private val log: Logger = Logger.getLogger(FallbackExceptionMapper::class.java.name)

    override fun toResponse(exception: Exception): Response {
        log.log(Level.SEVERE, "Необработанная ошибка при обслуживании запроса", exception)
        return errorResponse(HttpCodes.INTERNAL_SERVER_ERROR, "Внутренняя ошибка сервера")
    }
}
