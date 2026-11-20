package ru.ifmo.soa.starship.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import ru.ifmo.soa.starship.client.MuleStarshipClient
import ru.ifmo.soa.starship.dto.StarshipInputDto

/** Preserves the client URLs; all SOAP and result/error conversion belongs to Mule. */
@RestController
@RequestMapping(value = ["/starship"], produces = ["application/json"])
class StarshipController(
    private val service: MuleStarshipClient,
    private val queryParser: StarshipQueryParser,
) {
    @PostMapping(value = ["/create/{id}/{name}", "/create/{id}", "/create/{id}/"])
    fun createWithId(@PathVariable id: Long, @PathVariable(required = false) name: String?) =
        service.call("createWithId", mapOf("id" to id, "name" to name))

    @PostMapping("/{starship-id}/unload/{space-marine-id}")
    fun unload(@PathVariable("starship-id") starshipId: Long, @PathVariable("space-marine-id") spaceMarineId: Int) =
        service.call("unload", mapOf("starshipId" to starshipId, "spaceMarineId" to spaceMarineId))

    @GetMapping(value = ["", "/"])
    fun list(@RequestParam(required = false) id: String?, @RequestParam(required = false) name: String?,
             @RequestParam(required = false) sort: List<String>?, @RequestParam(required = false) page: String?,
             @RequestParam(required = false) size: String?): ResponseEntity<ByteArray> =
        service.call("list", queryParser.parse(id, name, sort, page, size))

    @PostMapping(value = ["", "/"])
    fun create(@RequestBody(required = false) body: StarshipInputDto?) =
        service.call("create", mapOf("name" to body?.name))

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long) = service.call("getById", mapOf("id" to id))

    @PatchMapping("/{id}")
    fun rename(@PathVariable id: Long, @RequestBody(required = false) body: StarshipInputDto?) =
        service.call("rename", mapOf("id" to id, "name" to body?.name))

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long) = service.call("delete", mapOf("id" to id))

    @PostMapping("/{starship-id}/board/{space-marine-id}")
    fun board(@PathVariable("starship-id") starshipId: Long, @PathVariable("space-marine-id") spaceMarineId: Int) =
        service.call("board", mapOf("starshipId" to starshipId, "spaceMarineId" to spaceMarineId))
}
