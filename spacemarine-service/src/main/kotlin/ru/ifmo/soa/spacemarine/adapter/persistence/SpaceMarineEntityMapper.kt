package ru.ifmo.soa.spacemarine.adapter.persistence

import ru.ifmo.soa.spacemarine.domain.model.Chapter
import ru.ifmo.soa.spacemarine.domain.model.Coordinates
import ru.ifmo.soa.spacemarine.domain.model.SpaceMarine

/** Перевод между персистентной и доменной моделями. */
object SpaceMarineEntityMapper {

    fun toDomain(entity: SpaceMarineEntity): SpaceMarine = SpaceMarine(
        id = entity.id,
        name = entity.name,
        coordinates = Coordinates(entity.coordinates.x, entity.coordinates.y),
        creationDate = entity.creationDate,
        health = entity.health,
        loyal = entity.loyal,
        achievements = entity.achievements,
        category = entity.category,
        chapter = entity.chapter.toDomain(),
    )

    /** Создаёт новую строку. Идентификатор не переносится — его присвоит база. */
    fun toNewEntity(marine: SpaceMarine): SpaceMarineEntity = SpaceMarineEntity(
        id = null,
        name = marine.name,
        coordinates = CoordinatesEmbeddable(marine.coordinates.x, marine.coordinates.y),
        creationDate = marine.creationDate,
        health = marine.health,
        loyal = marine.loyal,
        achievements = marine.achievements,
        category = marine.category,
        chapter = marine.chapter?.let { ChapterEmbeddable(it.name, it.parentLegion) },
    )

    /** Переносит изменяемые поля в управляемую сущность. `id` и `creationDate` не трогаем: оба `readOnly`. */
    fun applyTo(entity: SpaceMarineEntity, marine: SpaceMarine) {
        entity.name = marine.name
        entity.coordinates = CoordinatesEmbeddable(marine.coordinates.x, marine.coordinates.y)
        entity.health = marine.health
        entity.loyal = marine.loyal
        entity.achievements = marine.achievements
        entity.category = marine.category
        entity.chapter = marine.chapter?.let { ChapterEmbeddable(it.name, it.parentLegion) }
    }

    /**
     * Hibernate считает встроенный объект `null`, только если все его колонки `NULL`,
     * но в ряде версий возвращает «пустой» экземпляр. Поэтому проверяем обязательное поле,
     * а не только сам объект: орден без названия — это отсутствующий орден.
     */
    private fun ChapterEmbeddable?.toDomain(): Chapter? {
        val chapterName = this?.name ?: return null
        return Chapter(chapterName, parentLegion)
    }
}
