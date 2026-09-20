package ru.ifmo.soa.spacemarine.query

/** Пустая выборка — полноценный результат (200 с пустым `items`), а не 404. */
data class Page<T>(
    val items: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
) {
    val totalPages: Int
        get() = if (totalElements == 0L) 0 else ((totalElements + size - 1) / size).toInt()
}
