package ru.ifmo.soa.starship.dto

/** Схема `StarshipInput`: тело создания и переименования. Поле nullable — проверку делает сервис. */
data class StarshipInputDto(
    val name: String? = null,
)
