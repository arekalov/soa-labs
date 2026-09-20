package ru.ifmo.soa.starship.query

/** Фильтры списка: точное совпадение, объединяются по «И»; `null` — поле не фильтруется. */
data class StarshipFilter(
    val id: Long? = null,
    val name: String? = null,
) {
    companion object {
        val NONE = StarshipFilter()
    }
}
