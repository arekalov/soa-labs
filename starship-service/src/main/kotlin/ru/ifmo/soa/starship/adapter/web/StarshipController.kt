package ru.ifmo.soa.starship.adapter.web

import com.fasterxml.jackson.annotation.JsonInclude
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import ru.ifmo.soa.starship.application.usecase.CreateStarship
import ru.ifmo.soa.starship.application.usecase.UnloadSpaceMarine
import ru.ifmo.soa.starship.domain.model.Starship

/** Схема `Starship`. */
data class StarshipDto(
    val id: Long,
    val name: String,
    val marines: List<Int>,
)

/** Схема `UnloadResult`. */
data class UnloadResultDto(
    val starshipId: Long,
    val spaceMarineId: Int,
    val message: String,
)

/** Схема `Error`. Совпадает с форматом первого сервиса. */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ErrorDto(
    val code: Int,
    val message: String,
    val details: List<String>? = null,
)

/**
 * Две операции второго сервиса.
 *
 * Префикс `/starship` из спецификации даёт контекст развёртывания (WAR называется
 * `starship.war`), поэтому в аннотациях он не повторяется. Для запуска со встроенным
 * сервером тот же префикс задан свойством `server.servlet.context-path`, чтобы адреса
 * локально и на сервере совпадали.
 */
@RestController
class StarshipController(
    private val createStarship: CreateStarship,
    private val unloadSpaceMarine: UnloadSpaceMarine,
) {

    /**
     * Вариантов пути три, потому что пустое название даёт `/create/1/` или `/create/1`.
     * Без них такой запрос ушёл бы в 404, тогда как спецификация требует 400.
     */
    @PostMapping(
        value = ["/create/{id}/{name}", "/create/{id}", "/create/{id}/"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun create(
        @PathVariable id: Long,
        @PathVariable(required = false) name: String?,
    ): ResponseEntity<StarshipDto> =
        ResponseEntity
            .status(HttpStatus.CREATED)
            .body(createStarship.execute(id, name).toDto())

    /**
     * Имена переменных пути записаны через дефис — ровно как в спецификации,
     * поэтому связывание задано явно.
     */
    @PostMapping(
        value = ["/{starship-id}/unload/{space-marine-id}"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun unload(
        @PathVariable("starship-id") starshipId: Long,
        @PathVariable("space-marine-id") spaceMarineId: Int,
    ): UnloadResultDto {
        val result = unloadSpaceMarine.execute(starshipId, spaceMarineId)
        return UnloadResultDto(result.starshipId, result.spaceMarineId, result.message)
    }

    private fun Starship.toDto() = StarshipDto(
        id = id,
        name = name,
        marines = marines.sorted(),
    )
}
