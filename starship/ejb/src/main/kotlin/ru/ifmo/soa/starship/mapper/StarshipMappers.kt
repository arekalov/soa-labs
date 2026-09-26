package ru.ifmo.soa.starship.mapper

import ru.ifmo.soa.starship.dto.StarshipDto
import ru.ifmo.soa.starship.dto.StarshipPageDto
import ru.ifmo.soa.starship.model.Starship
import ru.ifmo.soa.starship.repository.Page

fun Starship.toDto(): StarshipDto = StarshipDto(
    id = requireNotNull(id) { "У сохранённого корабля обязан быть идентификатор" },
    name = name,
    marines = marines.sorted(),
)

fun Page<Starship>.toDto(): StarshipPageDto = StarshipPageDto(
    items = items.map { it.toDto() },
    page = page,
    size = size,
    totalElements = totalElements,
    totalPages = totalPages,
)
