package ru.ifmo.soa.spacemarine.dto

import ru.ifmo.soa.spacemarine.model.AstartesCategory

/**
 * Частичное обновление. `id` и `creationDate` отсутствуют: спецификация помечает их `readOnly`.
 */
data class SpaceMarinePatchDto(
    val name: Patched<String?>? = null,
    val coordinates: Patched<CoordinatesDto?>? = null,
    val health: Patched<Float?>? = null,
    val loyal: Patched<Boolean?>? = null,
    val achievements: Patched<String?>? = null,
    val category: Patched<AstartesCategory?>? = null,
    val chapter: Patched<ChapterDto?>? = null,
)
