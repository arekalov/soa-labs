package ru.ifmo.soa.starship.query

import org.springframework.data.domain.Pageable

const val DEFAULT_PAGE = 0
const val DEFAULT_SIZE = 20

/** Разобранный запрос списка: чем фильтруем и какую страницу в каком порядке отдаём. */
data class StarshipQuery(
    val filter: StarshipFilter,
    val pageable: Pageable,
)
