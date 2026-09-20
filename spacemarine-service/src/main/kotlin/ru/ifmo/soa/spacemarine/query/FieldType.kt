package ru.ifmo.soa.spacemarine.query

/** Тип значения поля: определяет, как разбирать сырую строку из URL. */
enum class FieldType {
    POSITIVE_INT,
    INT,
    BOUNDED_DOUBLE,
    POSITIVE_FLOAT,
    BOOLEAN,
    INSTANT,
    NON_BLANK_STRING,
    STRING,
    CATEGORY,
}
