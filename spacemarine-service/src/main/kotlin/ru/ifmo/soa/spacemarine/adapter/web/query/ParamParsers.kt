package ru.ifmo.soa.spacemarine.adapter.web.query

import ru.ifmo.soa.spacemarine.adapter.web.error.InvalidParameterException
import ru.ifmo.soa.spacemarine.application.query.FieldType
import ru.ifmo.soa.spacemarine.application.query.SpaceMarineField
import ru.ifmo.soa.spacemarine.domain.model.AstartesCategory
import ru.ifmo.soa.spacemarine.domain.model.SpaceMarineFactory
import java.time.Instant
import java.time.format.DateTimeParseException

/**
 * Разбор сырых строк из URL в типизированные значения.
 *
 * Любая неудача — это 400: параметр не соответствует формату, описанному в схеме.
 * Тексты сообщений подобраны под примеры из спецификации, например
 * `Параметр 'id' должен быть целым числом больше 0`.
 */
object ParamParsers {

    fun parseFilter(field: SpaceMarineField, raw: String): Any {
        val name = field.apiName
        return when (field.type) {
            FieldType.POSITIVE_INT -> positiveInt(name, raw)
            FieldType.INT -> int(name, raw)
            FieldType.BOUNDED_DOUBLE -> boundedDouble(name, raw, SpaceMarineFactory.MAX_COORDINATE_Y)
            FieldType.POSITIVE_FLOAT -> positiveFloat(name, raw)
            FieldType.BOOLEAN -> boolean(name, raw)
            FieldType.INSTANT -> instant(name, raw)
            FieldType.NON_BLANK_STRING -> nonBlank(name, raw)
            FieldType.STRING -> raw
            FieldType.CATEGORY -> category(name, raw)
        }
    }

    fun positiveInt(name: String, raw: String): Int {
        val value = raw.toIntOrNull()
            ?: throw InvalidParameterException("Параметр '$name' должен быть целым числом больше 0")
        if (value <= 0) {
            throw InvalidParameterException("Параметр '$name' должен быть целым числом больше 0")
        }
        return value
    }

    /** Любое конечное число: порог для сравнения не обязан быть положительным. */
    fun finiteFloat(name: String, raw: String): Float =
        raw.toFloatOrNull()?.takeIf { it.isFinite() }
            ?: throw InvalidParameterException("Параметр '$name' должен быть числом")

    fun int(name: String, raw: String): Int =
        raw.toIntOrNull()
            ?: throw InvalidParameterException("Параметр '$name' должен быть целым числом")

    /** Целое не меньше [min]; пустое значение заменяется на [default]. */
    fun intWithMin(name: String, raw: String?, min: Int, default: Int): Int {
        if (raw == null) return default
        val value = raw.toIntOrNull()
            ?: throw InvalidParameterException("Параметр '$name' должен быть целым числом не меньше $min")
        if (value < min) {
            throw InvalidParameterException("Параметр '$name' должен быть целым числом не меньше $min")
        }
        return value
    }

    private fun boundedDouble(name: String, raw: String, max: Double): Double {
        val value = raw.toDoubleOrNull()?.takeIf { it.isFinite() }
            ?: throw InvalidParameterException("Параметр '$name' должен быть числом")
        if (value > max) {
            throw InvalidParameterException(
                "Параметр '$name' не может превышать ${max.toInt()}",
            )
        }
        return value
    }

    private fun positiveFloat(name: String, raw: String): Float {
        val value = raw.toFloatOrNull()?.takeIf { it.isFinite() }
            ?: throw InvalidParameterException("Параметр '$name' должен быть числом больше 0")
        if (value <= 0f) {
            throw InvalidParameterException("Параметр '$name' должен быть числом больше 0")
        }
        return value
    }

    /**
     * Строгий разбор: допустимы только `true` и `false`.
     *
     * Штатный [String.toBoolean] трактует любую другую строку как `false`, из-за чего
     * `loyal=maybe` молча превратился бы в фильтр по `false` вместо ответа 400.
     */
    private fun boolean(name: String, raw: String): Boolean = when (raw.lowercase()) {
        "true" -> true
        "false" -> false
        else -> throw InvalidParameterException("Параметр '$name' должен быть true или false")
    }

    private fun instant(name: String, raw: String): Instant = try {
        Instant.parse(raw).truncatedTo(SpaceMarineFactory.TIME_PRECISION)
    } catch (_: DateTimeParseException) {
        throw InvalidParameterException(
            "Параметр '$name' должен быть датой и временем в формате ISO-8601, например 2026-09-10T12:00:00Z",
        )
    }

    private fun nonBlank(name: String, raw: String): String {
        if (raw.isBlank()) throw InvalidParameterException("Параметр '$name' не может быть пустым")
        return raw
    }

    private fun category(name: String, raw: String): AstartesCategory =
        AstartesCategory.entries.firstOrNull { it.name == raw }
            ?: throw InvalidParameterException(
                "Параметр '$name' должен быть одним из: " +
                    AstartesCategory.entries.joinToString(", ") { it.name },
            )
}
