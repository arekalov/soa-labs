package ru.ifmo.soa.starship.adapter.web

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.resource.NoResourceFoundException
import ru.ifmo.soa.starship.application.error.InvalidParameterException
import ru.ifmo.soa.starship.application.error.SpaceMarineNotFoundException
import ru.ifmo.soa.starship.application.error.SpaceMarineNotOnBoardException
import ru.ifmo.soa.starship.application.error.SpaceMarineServiceUnavailableException
import ru.ifmo.soa.starship.application.error.StarshipAlreadyExistsException
import ru.ifmo.soa.starship.application.error.StarshipNotFoundException
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Перевод исключений в коды ответов из спецификации.
 *
 * Формат тела совпадает с первым сервисом. Чтобы Spring не подменил его на RFC 7807,
 * в настройках выключен `spring.mvc.problemdetails.enabled` — иначе вместо схемы `Error`
 * ушёл бы `application/problem+json`, и соответствие спецификации сломалось бы
 * на каждом ответе с ошибкой.
 */
@RestControllerAdvice
class ApiExceptionHandler {

    private val log: Logger = Logger.getLogger(ApiExceptionHandler::class.java.name)

    @ExceptionHandler(InvalidParameterException::class)
    fun onInvalidParameter(e: InvalidParameterException) =
        error(HttpStatus.BAD_REQUEST, e.message ?: "Некорректный запрос")

    /** Нечисловой или переполняющий идентификатор в пути. */
    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun onTypeMismatch(e: MethodArgumentTypeMismatchException) = error(
        HttpStatus.BAD_REQUEST,
        "Параметр '${e.name}' должен быть целым числом больше 0",
    )

    @ExceptionHandler(StarshipNotFoundException::class, SpaceMarineNotFoundException::class, SpaceMarineNotOnBoardException::class)
    fun onNotFound(e: RuntimeException) =
        error(HttpStatus.NOT_FOUND, e.message ?: "Ресурс не найден")

    /** Несуществующий маршрут должен давать наш JSON, а не HTML-страницу сервера. */
    @ExceptionHandler(NoResourceFoundException::class)
    fun onNoResource(e: NoResourceFoundException) =
        error(HttpStatus.NOT_FOUND, "Запрошенный ресурс не найден")

    @ExceptionHandler(StarshipAlreadyExistsException::class)
    fun onConflict(e: StarshipAlreadyExistsException) =
        error(HttpStatus.CONFLICT, e.message ?: "Конфликт состояния")

    /**
     * Первый сервис недоступен. Именно 503: спецификация описывает этот случай явно,
     * а 502 в ней отсутствует.
     */
    @ExceptionHandler(SpaceMarineServiceUnavailableException::class)
    fun onUpstreamUnavailable(e: SpaceMarineServiceUnavailableException): ResponseEntity<ErrorDto> {
        log.log(Level.WARNING, "Первый сервис недоступен", e)
        return error(
            HttpStatus.SERVICE_UNAVAILABLE,
            e.message ?: "Сервис SpaceMarine недоступен, повторите запрос позже",
        )
    }

    @ExceptionHandler(Exception::class)
    fun onUnexpected(e: Exception): ResponseEntity<ErrorDto> {
        log.log(Level.SEVERE, "Необработанная ошибка при обслуживании запроса", e)
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Внутренняя ошибка сервера")
    }

    private fun error(status: HttpStatus, message: String, details: List<String>? = null) =
        ResponseEntity.status(status).body(ErrorDto(status.value(), message, details))
}
