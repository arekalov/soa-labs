package ru.ifmo.soa.spacemarine.query

/** Одна ступень сортировки. Префикс `-` в запросе означает [descending]. */
data class SortSpec(
    val field: SpaceMarineField,
    val descending: Boolean,
)
