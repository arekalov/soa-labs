package ru.ifmo.soa.starship.dto

data class StarshipDto(
    val id: Long,
    val name: String,
    val marines: List<Int>,
)
