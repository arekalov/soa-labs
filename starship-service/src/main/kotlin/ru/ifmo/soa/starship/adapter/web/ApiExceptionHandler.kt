package ru.ifmo.soa.starship.adapter.web

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.HttpMediaTypeNotAcceptableException
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.NoHandlerFoundException
import org.springframework.web.servlet.resource.NoResourceFoundException
import ru.ifmo.soa.starship.application.error.InvalidParameterException
import ru.ifmo.soa.starship.application.error.SpaceMarineAlreadyOnBoardException
import ru.ifmo.soa.starship.application.error.SpaceMarineNotFoundException
import ru.ifmo.soa.starship.application.error.SpaceMarineNotOnBoardException
import ru.ifmo.soa.starship.application.error.SpaceMarineServiceUnavailableException
import ru.ifmo.soa.starship.application.error.StarshipAlreadyExistsException
import ru.ifmo.soa.starship.application.error.StarshipNotFoundException
import ru.ifmo.soa.starship.application.error.StarshipValidationException
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

    /**
     * Несуществующий маршрут должен давать наш JSON с 404, а не HTML сервера и не 500.
     *
     * Ловим оба исключения: при отключённых ресурсных маппингах Spring бросает
     * NoHandlerFoundException, а не NoResourceFoundException, — и без этой строки запрос
     * на неизвестный путь проваливался в общий обработчик как «внутренняя ошибка».
     */
    @ExceptionHandler(NoResourceFoundException::class, NoHandlerFoundException::class)
    fun onNoRoute(e: Exception) =
        error(HttpStatus.NOT_FOUND, "Запрошенный ресурс не найден")

    /** Путь существует, метод — нет. Статус сохраняем, тело приводим к схеме Error. */
    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun onMethodNotAllowed(e: HttpRequestMethodNotSupportedException) =
        error(HttpStatus.METHOD_NOT_ALLOWED, "Метод ${e.method} не поддерживается для этого ресурса")

    /** 415 и 406 в спецификации отсутствуют; по смыслу это «запрос не соответствует формату» — 400, как и в первом сервисе. */
    @ExceptionHandler(HttpMediaTypeNotSupportedException::class, HttpMediaTypeNotAcceptableException::class)
    fun onMediaType(e: Exception) =
        error(HttpStatus.BAD_REQUEST, "Запрос не соответствует ожидаемому формату")

    @ExceptionHandler(StarshipAlreadyExistsException::class, SpaceMarineAlreadyOnBoardException::class)
    fun onConflict(e: RuntimeException) =
        error(HttpStatus.CONFLICT, e.message ?: "Конфликт состояния")

    /** Тело разобрано, но нарушает ограничения полей — 422 с перечнем, как в первом сервисе. */
    @ExceptionHandler(StarshipValidationException::class)
    fun onValidation(e: StarshipValidationException) =
        error(HttpStatus.UNPROCESSABLE_ENTITY, e.message ?: "Нарушены ограничения целостности", e.details)

    /** Тело не является корректным JSON. */
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun onUnreadableBody(e: HttpMessageNotReadableException) =
        error(HttpStatus.BAD_REQUEST, "Тело запроса не является корректным JSON")

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
