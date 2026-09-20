package ru.ifmo.soa.spacemarine.controller

import com.fasterxml.jackson.databind.node.ObjectNode
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.PATCH
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.core.UriInfo
import ru.ifmo.soa.spacemarine.Messages
import ru.ifmo.soa.spacemarine.dto.CountResultDto
import ru.ifmo.soa.spacemarine.dto.SpaceMarineDto
import ru.ifmo.soa.spacemarine.dto.SpaceMarineInputDto
import ru.ifmo.soa.spacemarine.dto.SpaceMarinePageDto
import ru.ifmo.soa.spacemarine.exception.InvalidParameterException
import ru.ifmo.soa.spacemarine.mapper.SpaceMarineMapper
import ru.ifmo.soa.spacemarine.query.DEFAULT_PAGE
import ru.ifmo.soa.spacemarine.query.DEFAULT_SIZE
import ru.ifmo.soa.spacemarine.service.SpaceMarineService
import java.net.URI

/**
 * Все девять операций спецификации.
 *
 * Разносить их по нескольким классам с одинаковым `@Path` нельзя: это серая зона JAX-RS.
 * Коллизии между `/{id}` и путями вроде `/count/by-chapter` нет: шаблон `{id}`
 * компилируется в `[^/]+?` и совпадает ровно с одним сегментом.
 */
@Path("/space-marines")
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
open class SpaceMarineController @Inject constructor(
    private val service: SpaceMarineService,
    private val queryParser: SpaceMarineQueryParser,
    private val patchReader: SpaceMarinePatchReader,
) {

    @GET
    open fun list(@Context uriInfo: UriInfo): SpaceMarinePageDto =
        SpaceMarineMapper.toDto(service.search(queryParser.parse(uriInfo)))

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    open fun create(input: SpaceMarineInputDto?): Response {
        val dto = SpaceMarineMapper.toDto(service.create(input ?: SpaceMarineInputDto()))
        // Location спецификацией не описан, но и не запрещён: добавляем как расширение.
        return Response.status(Response.Status.CREATED)
            .entity(dto)
            .location(URI.create("/space-marines/${dto.id}"))
            .build()
    }

    @GET
    @Path("/{id}")
    open fun getById(@PathParam("id") rawId: String): SpaceMarineDto =
        SpaceMarineMapper.toDto(service.getById(parseId(rawId)))

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    open fun replace(@PathParam("id") rawId: String, input: SpaceMarineInputDto?): SpaceMarineDto =
        SpaceMarineMapper.toDto(service.replace(parseId(rawId), input ?: SpaceMarineInputDto()))

    @PATCH
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    open fun patch(@PathParam("id") rawId: String, body: ObjectNode?): SpaceMarineDto =
        SpaceMarineMapper.toDto(service.patch(parseId(rawId), patchReader.read(body)))

    @DELETE
    @Path("/{id}")
    open fun delete(@PathParam("id") rawId: String): Response {
        service.delete(parseId(rawId))
        return Response.noContent().build()
    }

    /**
     * Параметры называются `name` и `parentLegion`, а не `chapterName` и `chapterParentLegion`,
     * как одноимённые фильтры листинга. Это различие задано спецификацией.
     */
    @GET
    @Path("/count/by-chapter")
    open fun countByChapter(
        @QueryParam("name") name: String?,
        @QueryParam("parentLegion") parentLegion: String?,
    ): CountResultDto {
        if (name.isNullOrBlank()) throw InvalidParameterException(Messages.paramRequired("name"))
        return CountResultDto(service.countByChapter(name, parentLegion))
    }

    @GET
    @Path("/count/by-health-greater-than")
    open fun countByHealthGreaterThan(@QueryParam("health") health: String?): CountResultDto {
        if (health.isNullOrBlank()) throw InvalidParameterException(Messages.paramRequired("health"))
        return CountResultDto(service.countByHealthGreaterThan(ParamParsers.finiteFloat("health", health)))
    }

    @GET
    @Path("/search/by-name-prefix")
    open fun findByNamePrefix(
        @QueryParam("prefix") prefix: String?,
        @QueryParam(PAGE_PARAM) page: String?,
        @QueryParam(SIZE_PARAM) size: String?,
    ): SpaceMarinePageDto {
        if (prefix.isNullOrBlank()) throw InvalidParameterException(Messages.paramRequired("prefix"))
        return SpaceMarineMapper.toDto(
            service.findByNamePrefix(
                prefix = prefix,
                page = ParamParsers.intWithMin(PAGE_PARAM, page, min = 0, default = DEFAULT_PAGE),
                size = ParamParsers.intWithMin(SIZE_PARAM, size, min = 1, default = DEFAULT_SIZE),
            ),
        )
    }

    /**
     * Идентификатор разбирается вручную, а тип параметра — строка.
     *
     * Неудачная конвертация `@PathParam` даёт по спецификации JAX-RS код 404, тогда как
     * спецификация API требует 400. Объявление `id: Int` вернуло бы на `/space-marines/abc`
     * неверный код, и ограничение шаблона регуляркой не помогло бы.
     */
    private fun parseId(rawId: String): Int = ParamParsers.positiveInt("id", rawId)
}
