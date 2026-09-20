package ru.ifmo.soa.spacemarine.adapter.web.dto

import ru.ifmo.soa.spacemarine.application.query.Page
import ru.ifmo.soa.spacemarine.domain.model.ChapterDraft
import ru.ifmo.soa.spacemarine.domain.model.CoordinatesDraft
import ru.ifmo.soa.spacemarine.domain.model.SpaceMarine
import ru.ifmo.soa.spacemarine.domain.model.SpaceMarineDraft

/** Перевод между транспортными DTO и доменными типами. */
object DtoMapper {

    fun toDto(marine: SpaceMarine): SpaceMarineDto = SpaceMarineDto(
        // Объект пришёл из хранилища, идентификатор гарантированно присвоен
        id = requireNotNull(marine.id) { "У сохранённого десантника обязан быть идентификатор" },
        name = marine.name,
        coordinates = CoordinatesDto(marine.coordinates.x, marine.coordinates.y),
        creationDate = marine.creationDate,
        health = marine.health,
        loyal = marine.loyal,
        achievements = marine.achievements,
        category = marine.category,
        chapter = marine.chapter?.let { ChapterDto(it.name, it.parentLegion) },
    )

    fun toDto(page: Page<SpaceMarine>): SpaceMarinePageDto = SpaceMarinePageDto(
        items = page.items.map(::toDto),
        page = page.page,
        size = page.size,
        totalElements = page.totalElements,
        totalPages = page.totalPages,
    )

    /** Входной DTO превращается в черновик; проверять его будет домен. */
    fun toDraft(input: SpaceMarineInputDto): SpaceMarineDraft = SpaceMarineDraft(
        name = input.name,
        coordinates = input.coordinates?.let { CoordinatesDraft(it.x, it.y) },
        health = input.health,
        loyal = input.loyal,
        achievements = input.achievements,
        category = input.category,
        chapter = input.chapter?.let { ChapterDraft(it.name, it.parentLegion) },
    )
}
