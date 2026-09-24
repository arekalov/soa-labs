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
 */
@Provider
class MalformedJsonMapper : ExceptionMapper<JsonProcessingException> {
    override fun toResponse(exception: JsonProcessingException): Response =
        Response.status(BAD_REQUEST)
            .type(MediaType.APPLICATION_JSON)
            .entity(ErrorDto(BAD_REQUEST, Messages.MALFORMED_JSON))
            .build()
}
