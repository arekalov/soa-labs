package ru.ifmo.soa.spacemarine.query

/**
 * Поля, по которым спецификация разрешает фильтровать и сортировать.
 */
enum class SpaceMarineField(
    val apiName: String,
    val type: FieldType,
    val entityPath: List<String>,
) {
    ID("id", FieldType.POSITIVE_INT, listOf("id")),
    NAME("name", FieldType.NON_BLANK_STRING, listOf("name")),
    COORDINATES_X("coordinatesX", FieldType.INT, listOf("coordinates", "x")),
    COORDINATES_Y("coordinatesY", FieldType.BOUNDED_DOUBLE, listOf("coordinates", "y")),
    CREATION_DATE("creationDate", FieldType.INSTANT, listOf("creationDate")),
    HEALTH("health", FieldType.POSITIVE_FLOAT, listOf("health")),
    LOYAL("loyal", FieldType.BOOLEAN, listOf("loyal")),
    ACHIEVEMENTS("achievements", FieldType.STRING, listOf("achievements")),
    CATEGORY("category", FieldType.CATEGORY, listOf("category")),
    CHAPTER_NAME("chapterName", FieldType.NON_BLANK_STRING, listOf("chapter", "name")),
    CHAPTER_PARENT_LEGION("chapterParentLegion", FieldType.STRING, listOf("chapter", "parentLegion")),
    ;

    companion object {
        private val BY_API_NAME = entries.associateBy { it.apiName }

        fun byApiNameOrNull(apiName: String): SpaceMarineField? = BY_API_NAME[apiName]
    }
}
