package ru.ifmo.soa.spacemarine.dto

import ru.ifmo.soa.spacemarine.model.AstartesCategory
import java.time.Instant

/**
 * Десантник в ответе. Схема `SpaceMarine`.
 *
 * `creationDate` — [Instant], а не `OffsetDateTime`: первый сериализуется как
 * `2026-09-10T12:00:00Z`, ровно как в примере спецификации, второй дал бы `+00:00`.
 */
data class SpaceMarineDto(
    val id: Int,
    val name: String,
    val coordinates: CoordinatesDto,
    val creationDate: Instant,
    val health: Float,
    val loyal: Boolean,
    val achievements: String?,
    val category: AstartesCategory,
    val chapter: ChapterDto?,
)
