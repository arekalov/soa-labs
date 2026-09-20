package ru.ifmo.soa.spacemarine.query

/** Тип значения поля: определяет, как разбирать сырую строку из URL. */
enum class FieldType {
    POSITIVE_INT,
    INT,
    /** Вещественное с верхней границей. */
    BOUNDED_DOUBLE,
    POSITIVE_FLOAT,
    BOOLEAN,
    /** Момент времени в ISO-8601. */
    INSTANT,
    NON_BLANK_STRING,
    STRING,
    CATEGORY,
}
