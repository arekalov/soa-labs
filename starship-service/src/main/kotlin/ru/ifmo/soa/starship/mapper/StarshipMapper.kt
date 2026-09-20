package ru.ifmo.soa.starship.mapper

import org.springframework.data.domain.Page
import ru.ifmo.soa.starship.dto.StarshipDto
import ru.ifmo.soa.starship.dto.StarshipPageDto
import ru.ifmo.soa.starship.model.Starship

object StarshipMapper {

    fun toDto(starship: Starship): StarshipDto = StarshipDto(
        id = requireNotNull(starship.id) { "У сохранённого корабля обязан быть идентификатор" },
        name = starship.name,
        marines = starship.marines.sorted(),
    )

    fun toDto(page: Page<Starship>): StarshipPageDto = StarshipPageDto(
        items = page.content.map(::toDto),
        page = page.number,
        size = page.size,
        totalElements = page.totalElements,
        totalPages = page.totalPages,
    )
}
