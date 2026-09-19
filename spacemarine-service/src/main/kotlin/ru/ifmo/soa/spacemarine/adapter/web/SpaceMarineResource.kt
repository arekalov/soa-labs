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
import ru.ifmo.soa.spacemarine.adapter.web.dto.IdGroupDto
import ru.ifmo.soa.spacemarine.adapter.web.dto.SpaceMarineDto
import ru.ifmo.soa.spacemarine.adapter.web.dto.SpaceMarineInputDto
import ru.ifmo.soa.spacemarine.adapter.web.dto.SpaceMarinePageDto
import ru.ifmo.soa.spacemarine.adapter.web.error.InvalidParameterException
import ru.ifmo.soa.spacemarine.adapter.web.query.ParamParsers
import ru.ifmo.soa.spacemarine.adapter.web.query.SpaceMarineQueryParser
import ru.ifmo.soa.spacemarine.application.usecase.CountSpaceMarinesByChapter
import ru.ifmo.soa.spacemarine.application.usecase.CreateSpaceMarine
import ru.ifmo.soa.spacemarine.application.usecase.DeleteSpaceMarine
import ru.ifmo.soa.spacemarine.application.usecase.FindSpaceMarineWithMinHealth
import ru.ifmo.soa.spacemarine.application.usecase.GetSpaceMarine
import ru.ifmo.soa.spacemarine.application.usecase.GroupSpaceMarinesById
import ru.ifmo.soa.spacemarine.application.usecase.PatchSpaceMarine
import ru.ifmo.soa.spacemarine.application.usecase.SearchSpaceMarines
import ru.ifmo.soa.spacemarine.application.usecase.UpdateSpaceMarine

/**
 * Все девять операций спецификации собраны в одном корневом ресурсе.
 *
 * Разносить их по нескольким классам с одинаковым `@Path` нельзя: это серая зона JAX-RS,
 * и поведение зависит от реализации.
 *
 * Коллизии между `/{id}` и `/health/min` нет: шаблон `{id}` компилируется в `[^/]+?`
 * и совпадает ровно с одним сегментом, а `health/min` — это два сегмента.
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
    private val findWithMinHealth: FindSpaceMarineWithMinHealth,
    private val groupById: GroupSpaceMarinesById,
    private val countByChapter: CountSpaceMarinesByChapter,
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

    /** Любой десантник с минимальным `health`. На пустой коллекции — 404. */
    @GET
    @Path("/health/min")
    open fun minHealth(): SpaceMarineDto = DtoMapper.toDto(findWithMinHealth.execute())

    /** Группировка по `id`. Массив возвращается на верхнем уровне, без объекта-обёртки. */
    @GET
    @Path("/groups/by-id")
    open fun groupsById(): List<IdGroupDto> = groupById.execute().map(DtoMapper::toDto)

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
