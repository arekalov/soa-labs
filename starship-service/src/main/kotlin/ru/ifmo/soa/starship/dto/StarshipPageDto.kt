package ru.ifmo.soa.starship.dto

/** Схема `StarshipPage`. */
data class StarshipPageDto(
    val items: List<StarshipDto>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)
