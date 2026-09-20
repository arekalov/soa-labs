package ru.ifmo.soa.starship.mapper

import org.springframework.data.domain.Page
import ru.ifmo.soa.starship.dto.StarshipDto
import ru.ifmo.soa.starship.dto.StarshipPageDto
import ru.ifmo.soa.starship.model.Starship

fun Starship.toDto(): StarshipDto = StarshipDto(
    id = requireNotNull(id) { "У сохранённого корабля обязан быть идентификатор" },
    name = name,
    marines = marines.sorted(),
)

fun Page<Starship>.toDto(): StarshipPageDto = StarshipPageDto(
    items = content.map { it.toDto() },
    page = number,
    size = size,
    totalElements = totalElements,
    totalPages = totalPages,
)
