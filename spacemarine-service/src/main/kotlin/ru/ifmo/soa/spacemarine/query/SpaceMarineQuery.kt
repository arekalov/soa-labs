package ru.ifmo.soa.spacemarine.query

const val DEFAULT_PAGE = 0
const val DEFAULT_SIZE = 20

/**
 * Разобранный запрос выборки. Фильтры комбинируются по «И», сравнение — на точное
 * равенство. Порядок [sort] задаёт приоритет ступеней.
 */
data class SpaceMarineQuery(
    val filters: Map<SpaceMarineField, Any> = emptyMap(),
    val sort: List<SortSpec> = emptyList(),
    val page: Int = DEFAULT_PAGE,
    val size: Int = DEFAULT_SIZE,
) {
    init {
        require(page >= 0) { "page не может быть отрицательной" }
        require(size >= 1) { "size должен быть не меньше 1" }
    }

    /** `Long`, чтобы не переполниться на больших номерах страниц. */
    val offset: Long get() = page.toLong() * size
}
