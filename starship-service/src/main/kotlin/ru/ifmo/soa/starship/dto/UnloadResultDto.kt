package ru.ifmo.soa.starship.dto

data class UnloadResultDto(
    val starshipId: Long,
    val spaceMarineId: Int,
    val message: String,
)
