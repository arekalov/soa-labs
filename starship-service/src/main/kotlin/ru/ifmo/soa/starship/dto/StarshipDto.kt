package ru.ifmo.soa.starship.dto

/** Схема `Starship`. */
data class StarshipDto(
    val id: Long,
    val name: String,
    val marines: List<Int>,
)
