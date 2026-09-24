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
import ru.ifmo.soa.spacemarine.config.jsonMapper
import ru.ifmo.soa.spacemarine.exception.InvalidParameterException
import ru.ifmo.soa.spacemarine.mapper.toDto
import ru.ifmo.soa.spacemarine.query.DEFAULT_PAGE
import ru.ifmo.soa.spacemarine.query.DEFAULT_SIZE
import ru.ifmo.soa.spacemarine.service.SpaceMarineService
import java.net.URI

@Path("/space-marines")
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
open class SpaceMarineController @Inject constructor(
    private val service: SpaceMarineService,
    private val queryParser: SpaceMarineQueryParser,
) {
    @GET
    open fun list(@Context uriInfo: UriInfo): SpaceMarinePageDto =
        service.search(queryParser.parse(uriInfo)).toDto()

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    open fun create(input: SpaceMarineInputDto?): Response {
        val dto = service.create(input ?: SpaceMarineInputDto()).toDto()
        return Response.status(Response.Status.CREATED)
            .entity(dto)
            .location(URI.create("/space-marines/${dto.id}"))
            .build()
    }

    @GET
    @Path("/{id}")
    open fun getById(@PathParam("id") rawId: String): SpaceMarineDto =
        service.getById(parseId(rawId)).toDto()

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    open fun replace(@PathParam("id") rawId: String, input: SpaceMarineInputDto?): SpaceMarineDto =
        service.replace(parseId(rawId), input ?: SpaceMarineInputDto()).toDto()

    @PATCH
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    open fun patch(@PathParam("id") rawId: String, body: ObjectNode?): SpaceMarineDto =
        service.patch(parseId(rawId)) { current -> applyPatch(body, current) }.toDto()

    @DELETE
    @Path("/{id}")
    open fun delete(@PathParam("id") rawId: String): Response {
        service.delete(parseId(rawId))
        return Response.noContent().build()
    }

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
        return service.findByNamePrefix(
            prefix = prefix,
            page = ParamParsers.intWithMin(PAGE_PARAM, page, min = 0, default = DEFAULT_PAGE),
            size = ParamParsers.intWithMin(SIZE_PARAM, size, min = 1, default = DEFAULT_SIZE),
        ).toDto()
    }

    // Тип параметра — строка: неудачную конвертацию @PathParam JAX-RS превращает в 404,
    // тогда как спецификация требует 400.
    private fun parseId(rawId: String): Int = ParamParsers.positiveInt("id", rawId)

    // readerForUpdating трогает только поля, физически присутствующие в JSON. Благодаря этому
    // «поля нет» и «поле равно null» различимы: первое не меняет ничего, второе очищает значение.
    private fun applyPatch(body: ObjectNode?, current: SpaceMarineInputDto): SpaceMarineInputDto =
        if (body == null) current else jsonMapper.readerForUpdating(current).readValue(body)
}
