package ru.ifmo.soa.spacemarine.exception

import com.fasterxml.jackson.core.JsonProcessingException
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.ExceptionMapper
import jakarta.ws.rs.ext.Provider
import ru.ifmo.soa.spacemarine.Messages
import ru.ifmo.soa.spacemarine.dto.ErrorDto

private const val BAD_REQUEST = 400

/**
 * Тело не разобралось как JSON либо тип значения не совпал с типом поля.
 *
 * Отдельный маппер, а не ветка в [GlobalExceptionMapper]: сбой чтения тела RESTEasy
 * оборачивает в свой `ReaderException` и ищет обработчик по точному типу причины.
 * Обработчик на `Throwable` в этот поиск не попадает, и наружу ушёл бы текст сервера.
 */
@Provider
class MalformedJsonMapper : ExceptionMapper<JsonProcessingException> {

    override fun toResponse(exception: JsonProcessingException): Response =
        Response.status(BAD_REQUEST)
            .type(MediaType.APPLICATION_JSON)
            .entity(ErrorDto(BAD_REQUEST, Messages.MALFORMED_JSON))
            .build()
}
