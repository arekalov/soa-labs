package ru.ifmo.soa.spacemarine.dto

/**
 * Обёртка «поле присутствовало в запросе».
 *
 * Различает три состояния тела PATCH, которые иначе сливаются: поля нет (`null` вместо
 * обёртки), поле задано значением, поле явно обнулено (`Patched(null)`).
 */
@JvmInline
value class Patched<out T>(val value: T)
