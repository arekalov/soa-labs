package ru.ifmo.soa.starship.adapter.web

import com.fasterxml.jackson.annotation.JsonInclude
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import ru.ifmo.soa.starship.application.error.InvalidParameterException
import ru.ifmo.soa.starship.application.query.Page
import ru.ifmo.soa.starship.application.query.StarshipField
import ru.ifmo.soa.starship.application.query.StarshipFilter
import ru.ifmo.soa.starship.application.query.StarshipQuery
import ru.ifmo.soa.starship.application.query.StarshipSort
import ru.ifmo.soa.starship.application.usecase.BoardSpaceMarine
import ru.ifmo.soa.starship.application.usecase.CreateStarship
import ru.ifmo.soa.starship.application.usecase.CreateStarshipWithGeneratedId
import ru.ifmo.soa.starship.application.usecase.DeleteStarship
import ru.ifmo.soa.starship.application.usecase.GetStarship
import ru.ifmo.soa.starship.application.usecase.ListStarships
import ru.ifmo.soa.starship.application.usecase.RenameStarship
import ru.ifmo.soa.starship.application.usecase.UnloadSpaceMarine
import ru.ifmo.soa.starship.domain.model.Starship

/** Схема `Starship`. */
data class StarshipDto(
    val id: Long,
    val name: String,
    val marines: List<Int>,
)

/** Схема `StarshipInput`: тело создания и переименования. Поле nullable намеренно — проверку делает сценарий. */
data class StarshipInputDto(
    val name: String? = null,
)

/** Схема `StarshipPage`. */
data class StarshipPageDto(
    val items: List<StarshipDto>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
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
 * Операции второго сервиса: две из спецификации ЛР1 и базовый набор над коллекцией кораблей.
 *
 * Префикс `/starship` даёт контекст развёртывания (WAR называется `starship.war`),
 * поэтому в аннотациях он не повторяется. Литеральные сегменты вроде `create`
 * Spring сопоставляет раньше шаблонов `{id}`, так что пересечений нет.
 */
@RestController
class StarshipController(
    private val createStarship: CreateStarship,
    private val createWithGeneratedId: CreateStarshipWithGeneratedId,
    private val listStarships: ListStarships,
    private val getStarship: GetStarship,
    private val renameStarship: RenameStarship,
    private val deleteStarship: DeleteStarship,
    private val boardSpaceMarine: BoardSpaceMarine,
    private val unloadSpaceMarine: UnloadSpaceMarine,
) {

    // ------------------------------------------------------ спецификация ЛР1

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
        ResponseEntity.status(HttpStatus.CREATED).body(createStarship.execute(id, name).toDto())

    /** Имена переменных пути записаны через дефис — ровно как в спецификации. */
    @PostMapping(value = ["/{starship-id}/unload/{space-marine-id}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun unload(
        @PathVariable("starship-id") starshipId: Long,
        @PathVariable("space-marine-id") spaceMarineId: Int,
    ): UnloadResultDto {
        val result = unloadSpaceMarine.execute(starshipId, spaceMarineId)
        return UnloadResultDto(result.starshipId, result.spaceMarineId, result.message)
    }

    // ------------------------------------------------------- базовые операции

    @GetMapping(value = ["", "/"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun list(
        @RequestParam(required = false) id: String?,
        @RequestParam(required = false) name: String?,
        @RequestParam(required = false) sort: List<String>?,
        @RequestParam(required = false) page: String?,
        @RequestParam(required = false) size: String?,
    ): StarshipPageDto = listStarships.execute(parseQuery(id, name, sort, page, size)).toDto()

    @PostMapping(value = ["", "/"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun createGenerated(@RequestBody(required = false) body: StarshipInputDto?): ResponseEntity<StarshipDto> =
        ResponseEntity.status(HttpStatus.CREATED).body(createWithGeneratedId.execute(body?.name).toDto())

    @GetMapping(value = ["/{id}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun get(@PathVariable id: Long): StarshipDto = getStarship.execute(id).toDto()

    @PatchMapping(value = ["/{id}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun rename(@PathVariable id: Long, @RequestBody(required = false) body: StarshipInputDto?): StarshipDto =
        renameStarship.execute(id, body?.name).toDto()

    @DeleteMapping(value = ["/{id}"])
    fun delete(@PathVariable id: Long): ResponseEntity<Void> {
        deleteStarship.execute(id)
        return ResponseEntity.noContent().build()
    }

    @PostMapping(value = ["/{starship-id}/board/{space-marine-id}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun board(
        @PathVariable("starship-id") starshipId: Long,
        @PathVariable("space-marine-id") spaceMarineId: Int,
    ): StarshipDto = boardSpaceMarine.execute(starshipId, spaceMarineId).toDto()

    // ------------------------------------------------------------- разбор

    private fun parseQuery(id: String?, name: String?, sort: List<String>?, page: String?, size: String?): StarshipQuery {
        val filter = StarshipFilter(
            id = id?.let {
                it.toLongOrNull()?.takeIf { v -> v > 0 }
                    ?: throw InvalidParameterException("Параметр 'id' должен быть целым числом больше 0")
            },
            name = name?.also {
                if (it.isBlank()) throw InvalidParameterException("Параметр 'name' не может быть пустым")
            },
        )
        val sorts = (sort ?: emptyList()).map { token ->
            val descending = token.startsWith('-')
            val field = StarshipField.byApiNameOrNull(if (descending) token.substring(1) else token)
                ?: throw InvalidParameterException("Параметр 'sort' содержит недопустимое значение '$token'")
            StarshipSort(field, descending)
        }
        if (sorts.map { it.field }.toSet().size != sorts.size) {
            throw InvalidParameterException("Поле указано в параметре 'sort' более одного раза")
        }
        return StarshipQuery(
            filter = filter,
            sort = sorts,
            page = intWithMin("page", page, 0, StarshipQuery.DEFAULT_PAGE),
            size = intWithMin("size", size, 1, StarshipQuery.DEFAULT_SIZE),
        )
    }

    private fun intWithMin(name: String, raw: String?, min: Int, default: Int): Int {
        if (raw == null) return default
        val value = raw.toIntOrNull()
        if (value == null || value < min) {
            throw InvalidParameterException("Параметр '$name' должен быть целым числом не меньше $min")
        }
        return value
    }

    private fun Starship.toDto() = StarshipDto(id = id, name = name, marines = marines.sorted())

    private fun Page<Starship>.toDto() = StarshipPageDto(
        items = items.map { it.toDto() },
        page = page,
        size = size,
        totalElements = totalElements,
        totalPages = totalPages,
    )
}
