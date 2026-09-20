package ru.ifmo.soa.starship.query

/** Поля, по которым разрешено сортировать список кораблей. */
enum class StarshipField(val apiName: String) {
    ID("id"),
    NAME("name"),
    ;

    companion object {
        fun byApiNameOrNull(apiName: String): StarshipField? = entries.firstOrNull { it.apiName == apiName }
    }
}
