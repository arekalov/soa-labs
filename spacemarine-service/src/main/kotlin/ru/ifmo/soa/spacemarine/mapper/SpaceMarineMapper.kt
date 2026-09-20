package ru.ifmo.soa.spacemarine.mapper

import ru.ifmo.soa.spacemarine.dto.ChapterDto
import ru.ifmo.soa.spacemarine.dto.CoordinatesDto
import ru.ifmo.soa.spacemarine.dto.Patched
import ru.ifmo.soa.spacemarine.dto.SpaceMarineDto
import ru.ifmo.soa.spacemarine.dto.SpaceMarineInputDto
import ru.ifmo.soa.spacemarine.dto.SpaceMarinePageDto
import ru.ifmo.soa.spacemarine.dto.SpaceMarinePatchDto
import ru.ifmo.soa.spacemarine.model.Chapter
import ru.ifmo.soa.spacemarine.model.Coordinates
import ru.ifmo.soa.spacemarine.model.SpaceMarine
import ru.ifmo.soa.spacemarine.query.Page
import java.time.Instant

/**
 * Перевод между сущностью и транспортными типами.
 *
 * Методы, собирающие сущность, рассчитывают на уже проверенный ввод: их вызывают
 * после [ru.ifmo.soa.spacemarine.service.SpaceMarineValidator].
 */
object SpaceMarineMapper {

    fun toDto(marine: SpaceMarine): SpaceMarineDto = SpaceMarineDto(
        id = requireNotNull(marine.id) { "У сохранённого десантника обязан быть идентификатор" },
        name = marine.name,
        coordinates = CoordinatesDto(marine.coordinates.x, marine.coordinates.y),
        creationDate = marine.creationDate,
        health = marine.health,
        loyal = marine.loyal,
        achievements = marine.achievements,
        category = marine.category,
        chapter = marine.chapter.toDto(),
    )

    fun toDto(page: Page<SpaceMarine>): SpaceMarinePageDto = SpaceMarinePageDto(
        items = page.items.map(::toDto),
        page = page.page,
        size = page.size,
        totalElements = page.totalElements,
        totalPages = page.totalPages,
    )

    fun toEntity(input: SpaceMarineInputDto, creationDate: Instant): SpaceMarine =
        SpaceMarine(creationDate = creationDate).also { applyTo(it, input) }

    /** `id` и `creationDate` не трогаем: оба помечены `readOnly`. */
    fun applyTo(marine: SpaceMarine, input: SpaceMarineInputDto) {
        marine.name = input.name!!
        marine.coordinates = Coordinates(input.coordinates!!.x!!, input.coordinates.y!!)
        marine.health = input.health!!
        marine.loyal = input.loyal!!
        marine.achievements = input.achievements
        marine.category = input.category!!
        marine.chapter = input.chapter?.let { Chapter(it.name, it.parentLegion) }
    }

    /**
     * Накладывает патч на текущее состояние и возвращает полное тело.
     *
     * Дальше оно проходит ту же проверку, что POST и PUT, — поэтому правила валидации
     * и формат `details` у всех трёх операций совпадают.
     *
     * Вложенные объекты заменяются целиком, без рекурсивного слияния: у обоих есть
     * обязательные поля, поэтому частично заданный объект был бы заведомо невалиден.
     */
    fun merge(marine: SpaceMarine, patch: SpaceMarinePatchDto): SpaceMarineInputDto = SpaceMarineInputDto(
        name = pick(patch.name, marine.name),
        coordinates = pick(patch.coordinates, CoordinatesDto(marine.coordinates.x, marine.coordinates.y)),
        health = pick(patch.health, marine.health),
        loyal = pick(patch.loyal, marine.loyal),
        achievements = pick(patch.achievements, marine.achievements),
        category = pick(patch.category, marine.category),
        chapter = pick(patch.chapter, marine.chapter.toDto()),
    )

    /**
     * Проверка на наличие обёртки, а не элвис-оператор: `patched?.value ?: fallback`
     * молча потерял бы явно переданный `null` и откатил поле к прежнему значению.
     */
    private fun <T> pick(patched: Patched<T>?, fallback: T): T =
        if (patched != null) patched.value else fallback

    /**
     * Hibernate считает встроенный объект `null`, только если все его колонки `NULL`,
     * но в ряде версий возвращает «пустой» экземпляр. Орден без названия — отсутствующий орден.
     */
    private fun Chapter?.toDto(): ChapterDto? {
        val chapterName = this?.name ?: return null
        return ChapterDto(chapterName, parentLegion)
    }
}
