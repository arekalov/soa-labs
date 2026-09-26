package ru.ifmo.soa.spacemarine.controller

import com.fasterxml.jackson.databind.node.ObjectNode
import jakarta.ejb.EJB
import jakarta.enterprise.context.RequestScoped
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
import ru.ifmo.soa.spacemarine.api.SpaceMarineRemote
import ru.ifmo.soa.spacemarine.dto.CountResultDto
import ru.ifmo.soa.spacemarine.dto.SpaceMarineDto
import ru.ifmo.soa.spacemarine.dto.SpaceMarineInputDto
import ru.ifmo.soa.spacemarine.dto.SpaceMarinePageDto
import ru.ifmo.soa.spacemarine.exception.InvalidParameterException
import ru.ifmo.soa.spacemarine.query.DEFAULT_PAGE
import ru.ifmo.soa.spacemarine.query.DEFAULT_SIZE
import java.net.URI

@Path("/space-marines")
@Produces(MediaType.APPLICATION_JSON)
@RequestScoped
open class SpaceMarineController {
    @EJB
    private lateinit var service: SpaceMarineRemote

    @Inject
    private lateinit var queryParser: SpaceMarineQueryParser

    @GET
    open fun list(@Context uriInfo: UriInfo): SpaceMarinePageDto =
        service.search(queryParser.parse(uriInfo))

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    open fun create(input: SpaceMarineInputDto?): Response {
        val dto = service.create(input ?: SpaceMarineInputDto())
        return Response.status(Response.Status.CREATED)
            .entity(dto)
            .location(URI.create("/space-marines/${dto.id}"))
            .build()
    }

    @GET
    @Path("/{id}")
    open fun getById(@PathParam("id") rawId: String): SpaceMarineDto = service.getById(parseId(rawId))

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    open fun replace(@PathParam("id") rawId: String, input: SpaceMarineInputDto?): SpaceMarineDto =
        service.replace(parseId(rawId), input ?: SpaceMarineInputDto())

    // Тело уходит строкой: лямбда слияния через Remote не передаётся, а накладывать патч
    // нужно внутри транзакции бина, где прочитано текущее состояние.
    @PATCH
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    open fun patch(@PathParam("id") rawId: String, body: ObjectNode?): SpaceMarineDto =
        service.patch(parseId(rawId), body?.toString())

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
        return service.countByChapter(name, parentLegion)
    }

    @GET
    @Path("/count/by-health-greater-than")
    open fun countByHealthGreaterThan(@QueryParam("health") health: String?): CountResultDto {
        if (health.isNullOrBlank()) throw InvalidParameterException(Messages.paramRequired("health"))
        return service.countByHealthGreaterThan(ParamParsers.finiteFloat("health", health))
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
        )
    }

    // Неудачную конвертацию @PathParam JAX-RS превращает в 404, а спецификация требует 400.
    private fun parseId(rawId: String): Int = ParamParsers.positiveInt("id", rawId)
}
