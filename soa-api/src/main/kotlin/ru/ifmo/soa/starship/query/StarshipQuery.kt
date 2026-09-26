package ru.ifmo.soa.starship.query

const val DEFAULT_PAGE = 0
const val DEFAULT_SIZE = 20

/** Одна ступень сортировки. Префикс `-` в запросе означает [descending]. */
data class StarshipSort(
    val field: StarshipField,
    val descending: Boolean,
) : java.io.Serializable

/** Разобранный запрос списка: чем фильтруем и какую страницу в каком порядке отдаём. */
data class StarshipQuery(
    val filter: StarshipFilter,
    val sort: List<StarshipSort> = emptyList(),
    val page: Int = DEFAULT_PAGE,
    val size: Int = DEFAULT_SIZE,
) : java.io.Serializable {
    val offset: Long get() = page.toLong() * size
}
