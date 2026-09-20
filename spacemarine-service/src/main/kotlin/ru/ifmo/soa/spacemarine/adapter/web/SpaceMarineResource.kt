package ru.ifmo.soa.spacemarine.adapter.web

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
import ru.ifmo.soa.spacemarine.adapter.web.dto.CountResultDto
import ru.ifmo.soa.spacemarine.adapter.web.dto.DtoMapper
import ru.ifmo.soa.spacemarine.adapter.web.dto.SpaceMarineDto
import ru.ifmo.soa.spacemarine.adapter.web.dto.SpaceMarineInputDto
import ru.ifmo.soa.spacemarine.adapter.web.dto.SpaceMarinePageDto
import ru.ifmo.soa.spacemarine.adapter.web.error.InvalidParameterException
import ru.ifmo.soa.spacemarine.adapter.web.query.ParamParsers
import ru.ifmo.soa.spacemarine.adapter.web.query.SpaceMarineQueryParser
import ru.ifmo.soa.spacemarine.application.query.Paging
import ru.ifmo.soa.spacemarine.application.query.SpaceMarineQuery
import ru.ifmo.soa.spacemarine.application.usecase.CountSpaceMarinesByChapter
import ru.ifmo.soa.spacemarine.application.usecase.CountSpaceMarinesByHealthGreaterThan
import ru.ifmo.soa.spacemarine.application.usecase.CreateSpaceMarine
import ru.ifmo.soa.spacemarine.application.usecase.DeleteSpaceMarine
import ru.ifmo.soa.spacemarine.application.usecase.FindSpaceMarinesByNamePrefix
import ru.ifmo.soa.spacemarine.application.usecase.GetSpaceMarine
import ru.ifmo.soa.spacemarine.application.usecase.PatchSpaceMarine
import ru.ifmo.soa.spacemarine.application.usecase.SearchSpaceMarines
import ru.ifmo.soa.spacemarine.application.usecase.UpdateSpaceMarine

/**
 * Все девять операций спецификации собраны в одном корневом ресурсе.
 *
 * Разносить их по нескольким классам с одинаковым `@Path` нельзя: это серая зона JAX-RS,
 * и поведение зависит от реализации.
 *
 * Коллизии между `/{id}` и двухсегментными путями вроде `/count/by-chapter` нет:
 * шаблон `{id}` компилируется в `[^/]+?` и совпадает ровно с одним сегментом.
 */
@Path("/space-marines")
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
open class SpaceMarineResource @Inject constructor(
    private val createSpaceMarine: CreateSpaceMarine,
    private val getSpaceMarine: GetSpaceMarine,
    private val updateSpaceMarine: UpdateSpaceMarine,
    private val patchSpaceMarine: PatchSpaceMarine,
    private val deleteSpaceMarine: DeleteSpaceMarine,
    private val searchSpaceMarines: SearchSpaceMarines,
    private val countByChapter: CountSpaceMarinesByChapter,
    private val countByHealthGreaterThan: CountSpaceMarinesByHealthGreaterThan,
    private val findByNamePrefix: FindSpaceMarinesByNamePrefix,
    private val queryParser: SpaceMarineQueryParser,
    private val patchReader: SpaceMarinePatchReader,
) {

    // ---------------------------------------------------------------- CRUD

    /** Выборка с фильтрацией, сортировкой и постраничным выводом. */
    @GET
    open fun list(@Context uriInfo: UriInfo): SpaceMarinePageDto =
        DtoMapper.toDto(searchSpaceMarines.execute(queryParser.parse(uriInfo)))

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    open fun create(input: SpaceMarineInputDto?): Response {
        val dto = DtoMapper.toDto(createSpaceMarine.execute(DtoMapper.toDraft(input.orEmpty())))
        // Location не описан в спецификации, но и не запрещён: добавляем как RESTful-расширение.
        return Response.status(Response.Status.CREATED)
            .entity(dto)
            .location(java.net.URI.create("/space-marines/${dto.id}"))
            .build()
    }

    @GET
    @Path("/{id}")
    open fun getById(@PathParam("id") rawId: String): SpaceMarineDto =
        DtoMapper.toDto(getSpaceMarine.execute(parseId(rawId)))

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    open fun update(@PathParam("id") rawId: String, input: SpaceMarineInputDto?): SpaceMarineDto =
        DtoMapper.toDto(updateSpaceMarine.execute(parseId(rawId), DtoMapper.toDraft(input.orEmpty())))

    @PATCH
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    open fun patch(@PathParam("id") rawId: String, body: ObjectNode?): SpaceMarineDto =
        DtoMapper.toDto(patchSpaceMarine.execute(parseId(rawId), patchReader.read(body)))

    @DELETE
    @Path("/{id}")
    open fun delete(@PathParam("id") rawId: String): Response {
        deleteSpaceMarine.execute(parseId(rawId))
        // 204 и пустое тело: в спецификации у этого ответа нет секции content.
        return Response.noContent().build()
    }

    // ------------------------------------------------------- Доп. операции

    /**
     * Количество десантников заданного ордена.
     *
     * Параметры здесь называются `name` и `parentLegion` — не так, как одноимённые фильтры
     * в листинге (`chapterName` / `chapterParentLegion`). Это различие задано спецификацией,
     * поэтому разбор отдельный, а не общий с [SpaceMarineQueryParser].
     */
    @GET
    @Path("/count/by-chapter")
    open fun countByChapter(
        @QueryParam("name") name: String?,
        @QueryParam("parentLegion") parentLegion: String?,
    ): CountResultDto {
        if (name.isNullOrBlank()) {
            throw InvalidParameterException("Параметр 'name' обязателен и не может быть пустым")
        }
        return CountResultDto(countByChapter.execute(name, parentLegion))
    }

    /** Количество десантников, у которых `health` строго больше заданного. */
    @GET
    @Path("/count/by-health-greater-than")
    open fun countByHealthGreaterThan(@QueryParam("health") health: String?): CountResultDto {
        if (health.isNullOrBlank()) throw InvalidParameterException("Параметр 'health' обязателен")
        return CountResultDto(countByHealthGreaterThan.execute(ParamParsers.finiteFloat("health", health)))
    }

    /** Страница десантников, чьё имя начинается с заданной подстроки. */
    @GET
    @Path("/search/by-name-prefix")
    open fun findByNamePrefix(
        @QueryParam("prefix") prefix: String?,
        @QueryParam("page") page: String?,
        @QueryParam("size") size: String?,
    ): SpaceMarinePageDto {
        if (prefix.isNullOrBlank()) {
            throw InvalidParameterException("Параметр 'prefix' обязателен и не может быть пустым")
        }
        val paging = Paging(
            page = ParamParsers.intWithMin("page", page, min = 0, default = SpaceMarineQuery.DEFAULT_PAGE),
            size = ParamParsers.intWithMin("size", size, min = 1, default = SpaceMarineQuery.DEFAULT_SIZE),
        )
        return DtoMapper.toDto(findByNamePrefix.execute(prefix, paging))
    }

    /**
     * Идентификатор разбирается вручную, а тип параметра — строка.
     *
     * Это принципиально: по спецификации JAX-RS неудачная конвертация `@PathParam`
     * даёт 404, тогда как спецификация API требует 400. Объявление `id: Int` вернуло бы
     * на `/space-marines/abc` неверный код, и ограничение шаблона регуляркой не помогло бы.
     */
    private fun parseId(rawId: String): Int = ParamParsers.positiveInt("id", rawId)

    /** Пустое тело эквивалентно телу без полей: решение примет валидация домена. */
    private fun SpaceMarineInputDto?.orEmpty(): SpaceMarineInputDto = this ?: SpaceMarineInputDto()
}
