package ru.ifmo.soa.starship.dto

/** Схема `UnloadResult`. Поля в camelCase, хотя параметры пути записаны через дефис. */
data class UnloadResultDto(
    val starshipId: Long,
    val spaceMarineId: Int,
    val message: String,
)
