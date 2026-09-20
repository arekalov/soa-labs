package ru.ifmo.soa.starship.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.HttpMediaTypeNotAcceptableException
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.NoHandlerFoundException
import org.springframework.web.servlet.resource.NoResourceFoundException
import ru.ifmo.soa.starship.Messages
import ru.ifmo.soa.starship.dto.ErrorDto
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Перевод исключений в коды ответов из спецификации.
 *
 * Формат тела совпадает с первым сервисом. Чтобы Spring не подменил его на RFC 7807,
 * в настройках выключен `spring.mvc.problemdetails.enabled`.
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    private val log: Logger = Logger.getLogger(GlobalExceptionHandler::class.java.name)

    @ExceptionHandler(InvalidParameterException::class)
    fun onInvalidParameter(e: InvalidParameterException) =
        error(HttpStatus.BAD_REQUEST, e.message ?: Messages.BAD_REQUEST)

    /** Нечисловой или переполняющий идентификатор в пути. */
    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun onTypeMismatch(e: MethodArgumentTypeMismatchException) =
        error(HttpStatus.BAD_REQUEST, Messages.paramPositiveInt(e.name))

    @ExceptionHandler(
        StarshipNotFoundException::class,
        SpaceMarineNotFoundException::class,
        SpaceMarineNotOnBoardException::class,
    )
    fun onNotFound(e: RuntimeException) = error(HttpStatus.NOT_FOUND, e.message ?: Messages.NOT_FOUND)

    /**
     * При отключённых ресурсных маппингах Spring бросает NoHandlerFoundException,
     * а не NoResourceFoundException, — ловим оба, иначе неизвестный путь даст 500.
     */
    @ExceptionHandler(NoResourceFoundException::class, NoHandlerFoundException::class)
    fun onNoRoute(e: Exception) = error(HttpStatus.NOT_FOUND, Messages.ROUTE_NOT_FOUND)

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun onMethodNotAllowed(e: HttpRequestMethodNotSupportedException) =
        error(HttpStatus.METHOD_NOT_ALLOWED, Messages.methodNotAllowed(e.method))

    /** 415 и 406 спецификация не описывает; по смыслу это «запрос не соответствует формату». */
    @ExceptionHandler(HttpMediaTypeNotSupportedException::class, HttpMediaTypeNotAcceptableException::class)
    fun onMediaType(e: Exception) = error(HttpStatus.BAD_REQUEST, Messages.WRONG_FORMAT)

    @ExceptionHandler(StarshipAlreadyExistsException::class, SpaceMarineAlreadyOnBoardException::class)
    fun onConflict(e: RuntimeException) = error(HttpStatus.CONFLICT, e.message ?: Messages.CONFLICT)

    @ExceptionHandler(StarshipValidationException::class)
    fun onValidation(e: StarshipValidationException) =
        error(HttpStatus.UNPROCESSABLE_ENTITY, e.message ?: Messages.CONSTRAINTS_VIOLATED, e.details)

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun onUnreadableBody(e: HttpMessageNotReadableException) =
        error(HttpStatus.BAD_REQUEST, Messages.MALFORMED_JSON)

    /** Именно 503: спецификация описывает этот случай явно, а 502 в ней отсутствует. */
    @ExceptionHandler(SpaceMarineServiceUnavailableException::class)
    fun onUpstreamUnavailable(e: SpaceMarineServiceUnavailableException): ResponseEntity<ErrorDto> {
        log.log(Level.WARNING, Messages.UPSTREAM_DOWN, e)
        return error(HttpStatus.SERVICE_UNAVAILABLE, e.message ?: Messages.UPSTREAM_UNAVAILABLE)
    }

    @ExceptionHandler(Exception::class)
    fun onUnexpected(e: Exception): ResponseEntity<ErrorDto> {
        log.log(Level.SEVERE, Messages.UNHANDLED_ERROR, e)
        return error(HttpStatus.INTERNAL_SERVER_ERROR, Messages.INTERNAL_ERROR)
    }

    private fun error(status: HttpStatus, message: String, details: List<String>? = null) =
        ResponseEntity.status(status).body(ErrorDto(status.value(), message, details))
}
