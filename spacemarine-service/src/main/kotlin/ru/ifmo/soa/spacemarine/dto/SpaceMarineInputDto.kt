package ru.ifmo.soa.spacemarine.dto

import ru.ifmo.soa.spacemarine.model.AstartesCategory

/**
 * Тело создания и полной замены. Схема `SpaceMarineInput`.
 *
 * Все поля nullable намеренно. Объяви их обязательными — Jackson упал бы на отсутствующем
 * поле и вернул 400, тогда как спецификация требует 422 с перечнем нарушений.
 */
data class SpaceMarineInputDto(
    val name: String? = null,
    val coordinates: CoordinatesDto? = null,
    val health: Float? = null,
    val loyal: Boolean? = null,
    val achievements: String? = null,
    val category: AstartesCategory? = null,
    val chapter: ChapterDto? = null,
)
