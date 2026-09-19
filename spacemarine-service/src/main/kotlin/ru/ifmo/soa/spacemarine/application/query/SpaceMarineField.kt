package ru.ifmo.soa.spacemarine.application.query

/**
 * Справочник полей, по которым спецификация разрешает фильтровать и сортировать.
 *
 * Один источник истины: добавление поля здесь автоматически расширяет и набор фильтров,
 * и допустимые значения `sort`. Именно поэтому 22 значения сортировки из спецификации
 * не перечислены руками — они выводятся как 11 полей × 2 направления.
 *
 * Класс намеренно не знает ничего о JPA: отображение поля в путь сущности живёт
 * в адаптере персистентности. Иначе внутренний слой потянул бы за собой Hibernate.
 */
enum class SpaceMarineField(
    /** Имя параметра в HTTP-запросе. */
    val apiName: String,
    /** Тип значения — определяет, как разбирать сырую строку из URL. */
    val type: FieldType,
) {
    ID("id", FieldType.POSITIVE_INT),
    NAME("name", FieldType.NON_BLANK_STRING),
    COORDINATES_X("coordinatesX", FieldType.INT),
    COORDINATES_Y("coordinatesY", FieldType.BOUNDED_DOUBLE),
    CREATION_DATE("creationDate", FieldType.INSTANT),
    HEALTH("health", FieldType.POSITIVE_FLOAT),
    LOYAL("loyal", FieldType.BOOLEAN),
    ACHIEVEMENTS("achievements", FieldType.STRING),
    CATEGORY("category", FieldType.CATEGORY),
    CHAPTER_NAME("chapterName", FieldType.NON_BLANK_STRING),
    CHAPTER_PARENT_LEGION("chapterParentLegion", FieldType.STRING),
    ;

    companion object {
        private val BY_API_NAME: Map<String, SpaceMarineField> = entries.associateBy { it.apiName }

        fun byApiNameOrNull(apiName: String): SpaceMarineField? = BY_API_NAME[apiName]

        /**
         * Все 22 допустимых значения параметра `sort`: имя поля и оно же с префиксом `-`.
         * Любое значение вне этого множества обязано давать 400.
         */
        val SORT_TOKENS: Set<String> =
            entries.flatMap { listOf(it.apiName, "-${it.apiName}") }.toSet()
    }
}

/** Тип значения поля. Разбор сырых строк выполняет веб-слой, опираясь на этот признак. */
enum class FieldType {
    /** Целое строго больше нуля. */
    POSITIVE_INT,
    INT,
    /** Вещественное с верхней границей (для `coordinatesY` — 12 включительно). */
    BOUNDED_DOUBLE,
    /** Вещественное строго больше нуля. */
    POSITIVE_FLOAT,
    BOOLEAN,
    /** Момент времени в ISO-8601, например `2026-09-10T12:00:00Z`. */
    INSTANT,
    /** Непустая строка. */
    NON_BLANK_STRING,
    STRING,
    /** Значение перечисления `AstartesCategory`. */
    CATEGORY,
}
