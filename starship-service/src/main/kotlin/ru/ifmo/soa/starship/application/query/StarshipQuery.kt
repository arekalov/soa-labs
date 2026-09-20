package ru.ifmo.soa.starship.application.query

/** Поля, по которым можно сортировать список кораблей. */
enum class StarshipField(val apiName: String) {
    ID("id"),
    NAME("name"),
    ;

    companion object {
        fun byApiNameOrNull(apiName: String): StarshipField? = entries.firstOrNull { it.apiName == apiName }
    }
}

data class StarshipSort(
    val field: StarshipField,
    val descending: Boolean,
)

/** Фильтры списка: точное совпадение, объединяются по «И»; `null` — поле не фильтруется. */
data class StarshipFilter(
    val id: Long? = null,
    val name: String? = null,
) {
    fun matches(id: Long, name: String): Boolean =
        (this.id == null || this.id == id) && (this.name == null || this.name == name)

    companion object {
        val NONE = StarshipFilter()
    }
}

data class StarshipQuery(
    val filter: StarshipFilter = StarshipFilter.NONE,
    val sort: List<StarshipSort> = emptyList(),
    val page: Int = DEFAULT_PAGE,
    val size: Int = DEFAULT_SIZE,
) {
    init {
        require(page >= 0) { "page не может быть отрицательной" }
        require(size >= 1) { "size должен быть не меньше 1" }
    }

    companion object {
        const val DEFAULT_PAGE = 0
        const val DEFAULT_SIZE = 20
    }
}

data class Page<T>(
    val items: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
) {
    val totalPages: Int
        get() = if (totalElements == 0L) 0 else ((totalElements + size - 1) / size).toInt()
}
