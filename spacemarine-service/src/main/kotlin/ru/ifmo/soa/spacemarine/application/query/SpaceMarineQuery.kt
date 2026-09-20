package ru.ifmo.soa.spacemarine.application.query

/** Одна ступень сортировки. Префикс `-` в запросе означает [descending]. */
data class SortSpec(
    val field: SpaceMarineField,
    val descending: Boolean,
)

/**
 * Разобранный запрос выборки: фильтры, сортировка, страница.
 *
 * Фильтры комбинируются по «И», сравнение — на точное равенство (так требует спецификация).
 * Порядок элементов [sort] задаёт приоритет ступеней сортировки.
 */
data class SpaceMarineQuery(
    val filters: Map<SpaceMarineField, Any>,
    val sort: List<SortSpec>,
    val page: Int,
    val size: Int,
) {
    init {
        require(page >= 0) { "page не может быть отрицательной" }
        require(size >= 1) { "size должен быть не меньше 1" }
    }

    /** Смещение первой записи страницы. `Long`, чтобы не переполниться на больших `page`. */
    val offset: Long get() = page.toLong() * size

    companion object {
        const val DEFAULT_PAGE = 0
        const val DEFAULT_SIZE = 20
    }
}

/**
 * Страница результатов.
 *
 * Пустая выборка — это полноценный результат (`200` с пустым `items`), а не `404`:
 * спецификация не описывает `404` для операции листинга.
 */
data class Page<T>(
    val items: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
) {
    val totalPages: Int
        get() = if (totalElements == 0L) 0 else ((totalElements + size - 1) / size).toInt()
}

