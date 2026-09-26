package ru.ifmo.soa.starship.controller

import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import ru.ifmo.soa.starship.Messages
import ru.ifmo.soa.starship.dto.StarshipDto
import ru.ifmo.soa.starship.dto.StarshipInputDto
import ru.ifmo.soa.starship.dto.StarshipPageDto
import ru.ifmo.soa.starship.dto.UnloadResultDto
import ru.ifmo.soa.starship.api.StarshipRemote

/**
 * Операции второго сервиса: две из спецификации ЛР1 и базовый набор над коллекцией.
 *
 * Префикс задан здесь, а не контекстом развёртывания: на запрос `/starship` без
 * завершающего слэша контейнер отвечает редиректом, а спецификация описывает
 * именно такой путь. WAR разворачивается в корень, префикс сопоставляет Spring.
 * Литеральные сегменты вроде `create` Spring сопоставляет раньше шаблонов `{id}`.
 */
@RestController
@RequestMapping("/starship")
class StarshipController(
    private val service: StarshipRemote,
    private val queryParser: StarshipQueryParser,
) {
    @PostMapping(
        value = ["/create/{id}/{name}", "/create/{id}", "/create/{id}/"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun createWithId(
        @PathVariable id: Long,
        @PathVariable(required = false) name: String?,
    ): ResponseEntity<StarshipDto> = ResponseEntity
        .status(HttpStatus.CREATED)
        .body(service.createWithId(id, name))

    @PostMapping(value = ["/{starship-id}/unload/{space-marine-id}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun unload(
        @PathVariable("starship-id") starshipId: Long,
        @PathVariable("space-marine-id") spaceMarineId: Int,
    ): UnloadResultDto {
        service.unload(starshipId, spaceMarineId)
        return UnloadResultDto(starshipId, spaceMarineId, Messages.unloaded(starshipId, spaceMarineId))
    }

    @GetMapping(value = ["", "/"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun list(
        @RequestParam(required = false) id: String?,
        @RequestParam(required = false) name: String?,
        @RequestParam(required = false) sort: List<String>?,
        @RequestParam(required = false) page: String?,
        @RequestParam(required = false) size: String?,
    ): StarshipPageDto = service.list(queryParser.parse(id, name, sort, page, size))

    @PostMapping(value = ["", "/"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun create(@RequestBody(required = false) body: StarshipInputDto?): ResponseEntity<StarshipDto> = ResponseEntity
        .status(HttpStatus.CREATED)
        .body(service.create(body?.name))

    @GetMapping(value = ["/{id}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getById(@PathVariable id: Long): StarshipDto = service.getById(id)

    @PatchMapping(value = ["/{id}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun rename(@PathVariable id: Long, @RequestBody(required = false) body: StarshipInputDto?): StarshipDto =
        service.rename(id, body?.name)

    @DeleteMapping(value = ["/{id}"])
    fun delete(@PathVariable id: Long): ResponseEntity<Void> {
        service.delete(id)
        return ResponseEntity.noContent().build()
    }

    @PostMapping(value = ["/{starship-id}/board/{space-marine-id}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun board(
        @PathVariable("starship-id") starshipId: Long,
        @PathVariable("space-marine-id") spaceMarineId: Int,
    ): StarshipDto = service.board(starshipId, spaceMarineId)
}
