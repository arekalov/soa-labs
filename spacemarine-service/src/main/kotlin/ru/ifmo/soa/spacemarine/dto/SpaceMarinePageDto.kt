package ru.ifmo.soa.spacemarine.dto

/** Схема `SpaceMarinePage`. */
data class SpaceMarinePageDto(
    val items: List<SpaceMarineDto>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)
