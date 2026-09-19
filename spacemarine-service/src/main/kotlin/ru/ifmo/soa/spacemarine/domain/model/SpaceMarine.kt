package ru.ifmo.soa.spacemarine.domain.model

import java.time.Instant

/**
 * Категория десантника. Значения и порядок зафиксированы спецификацией
 * (`AstartesCategory` в `docs/spacemarine-service.yaml`).
 */
enum class AstartesCategory {
    SCOUT,
    SUPPRESSOR,
    TACTICAL,
    HELIX,
}

/**
 * Координаты. Оба поля обязательны, `y` ограничен сверху значением 12 включительно.
 */
data class Coordinates(
    val x: Int,
    val y: Double,
)

/**
 * Орден. Название обязательно и непусто, родительский легион может отсутствовать.
 */
data class Chapter(
    val name: String,
    val parentLegion: String?,
)

/**
 * Десантник — корень агрегата.
 *
 * Класс намеренно не содержит ни одной аннотации фреймворка: доменный слой ничего не знает
 * ни о JPA, ни о JAX-RS. Персистентная модель живёт отдельно в `adapter.persistence`.
 *
 * Экземпляр этого типа считается заведомо валидным: единственный способ его получить —
 * [SpaceMarineFactory], которая проверяет все инварианты. Поэтому поля здесь не nullable
 * там, где спецификация требует обязательности.
 */
data class SpaceMarine(
    /** `null` до сохранения: идентификатор присваивает хранилище. */
    val id: Int?,
    val name: String,
    val coordinates: Coordinates,
    val creationDate: Instant,
    val health: Float,
    val loyal: Boolean,
    val achievements: String?,
    val category: AstartesCategory,
    val chapter: Chapter?,
) {
    init {
        // Страховка от обхода фабрики через copy(): инварианты должны держаться всегда.
        require(id == null || id > 0) { "id должен быть больше 0" }
    }
}
