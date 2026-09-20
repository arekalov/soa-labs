package ru.ifmo.soa.spacemarine.mapper

import ru.ifmo.soa.spacemarine.dto.ChapterDto
import ru.ifmo.soa.spacemarine.dto.CoordinatesDto
import ru.ifmo.soa.spacemarine.dto.SpaceMarineDto
import ru.ifmo.soa.spacemarine.dto.SpaceMarineInputDto
import ru.ifmo.soa.spacemarine.dto.SpaceMarinePageDto
import ru.ifmo.soa.spacemarine.model.Chapter
import ru.ifmo.soa.spacemarine.model.Coordinates
import ru.ifmo.soa.spacemarine.model.SpaceMarine
import ru.ifmo.soa.spacemarine.query.Page
import java.time.Instant

fun SpaceMarine.toDto(): SpaceMarineDto = SpaceMarineDto(
    id = requireNotNull(id) { "У сохранённого десантника обязан быть идентификатор" },
    name = name,
    coordinates = CoordinatesDto(coordinates.x, coordinates.y),
    creationDate = creationDate,
    health = health,
    loyal = loyal,
    achievements = achievements,
    category = category,
    chapter = chapter.toDto(),
)

fun Page<SpaceMarine>.toDto(): SpaceMarinePageDto = SpaceMarinePageDto(
    items = items.map { it.toDto() },
    page = page,
    size = size,
    totalElements = totalElements,
    totalPages = totalPages,
)

fun SpaceMarine.toInput(): SpaceMarineInputDto = SpaceMarineInputDto(
    name = name,
    coordinates = CoordinatesDto(coordinates.x, coordinates.y),
    health = health,
    loyal = loyal,
    achievements = achievements,
    category = category,
    chapter = chapter.toDto(),
)

fun SpaceMarineInputDto.toEntity(creationDate: Instant): SpaceMarine =
    SpaceMarine(creationDate = creationDate).also { it.applyFrom(this) }

fun SpaceMarine.applyFrom(input: SpaceMarineInputDto) {
    val point = requireNotNull(input.coordinates) { "Координаты обязаны быть заполнены" }
    name = requireNotNull(input.name)
    coordinates = Coordinates(requireNotNull(point.x), requireNotNull(point.y))
    health = requireNotNull(input.health)
    loyal = requireNotNull(input.loyal)
    achievements = input.achievements
    category = requireNotNull(input.category)
    chapter = input.chapter?.let { Chapter(it.name, it.parentLegion) }
}

/** Hibernate возвращает «пустой» встроенный объект вместо null: орден без названия — отсутствующий орден. */
private fun Chapter?.toDto(): ChapterDto? {
    val chapterName = this?.name ?: return null
    return ChapterDto(chapterName, parentLegion)
}
