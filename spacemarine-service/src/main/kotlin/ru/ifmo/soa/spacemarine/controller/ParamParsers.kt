package ru.ifmo.soa.spacemarine.controller

import ru.ifmo.soa.spacemarine.Messages
import ru.ifmo.soa.spacemarine.exception.InvalidParameterException
import ru.ifmo.soa.spacemarine.model.AstartesCategory
import ru.ifmo.soa.spacemarine.model.MAX_COORDINATE_Y
import ru.ifmo.soa.spacemarine.model.TIME_PRECISION
import ru.ifmo.soa.spacemarine.query.FieldType
import ru.ifmo.soa.spacemarine.query.SpaceMarineField
import java.time.Instant
import java.time.format.DateTimeParseException

/** Разбор сырых строк из URL. Любая неудача — 400: значение не соответствует схеме. */
object ParamParsers {

    fun parseFilter(field: SpaceMarineField, raw: String): Any {
        val name = field.apiName
        return when (field.type) {
            FieldType.POSITIVE_INT -> positiveInt(name, raw)
            FieldType.INT -> int(name, raw)
            FieldType.BOUNDED_DOUBLE -> boundedDouble(name, raw)
            FieldType.POSITIVE_FLOAT -> positiveFloat(name, raw)
            FieldType.BOOLEAN -> boolean(name, raw)
            FieldType.INSTANT -> instant(name, raw)
            FieldType.NON_BLANK_STRING -> nonBlank(name, raw)
            FieldType.STRING -> raw
            FieldType.CATEGORY -> category(name, raw)
        }
    }

    fun positiveInt(name: String, raw: String): Int =
        raw.toIntOrNull()?.takeIf { it > 0 }
            ?: throw InvalidParameterException(Messages.paramPositiveInt(name))

    /** Порог для сравнения не обязан быть положительным, важна только конечность. */
    fun finiteFloat(name: String, raw: String): Float =
        raw.toFloatOrNull()?.takeIf { it.isFinite() }
            ?: throw InvalidParameterException(Messages.paramNumber(name))

    /** Пустое значение заменяется на [default]. */
    fun intWithMin(name: String, raw: String?, min: Int, default: Int): Int {
        if (raw == null) return default
        return raw.toIntOrNull()?.takeIf { it >= min }
            ?: throw InvalidParameterException(Messages.paramIntWithMin(name, min))
    }

    private fun int(name: String, raw: String): Int =
        raw.toIntOrNull() ?: throw InvalidParameterException(Messages.paramInt(name))

    private fun boundedDouble(name: String, raw: String): Double {
        val value = raw.toDoubleOrNull()?.takeIf { it.isFinite() }
            ?: throw InvalidParameterException(Messages.paramNumber(name))
        if (value > MAX_COORDINATE_Y) {
            throw InvalidParameterException(Messages.paramMax(name, MAX_COORDINATE_Y.toInt()))
        }
        return value
    }

    private fun positiveFloat(name: String, raw: String): Float =
        raw.toFloatOrNull()?.takeIf { it.isFinite() && it > 0f }
            ?: throw InvalidParameterException(Messages.paramPositiveNumber(name))

    /**
     * Строгий разбор: штатный [String.toBoolean] трактует любую строку как `false`,
     * из-за чего `loyal=maybe` молча превратился бы в фильтр по `false` вместо 400.
     */
    private fun boolean(name: String, raw: String): Boolean = when (raw.lowercase()) {
        "true" -> true
        "false" -> false
        else -> throw InvalidParameterException(Messages.paramBoolean(name))
    }

    private fun instant(name: String, raw: String): Instant = try {
        Instant.parse(raw).truncatedTo(TIME_PRECISION)
    } catch (_: DateTimeParseException) {
        throw InvalidParameterException(Messages.paramInstant(name))
    }

    private fun nonBlank(name: String, raw: String): String =
        raw.takeIf { it.isNotBlank() } ?: throw InvalidParameterException(Messages.paramBlank(name))

    private fun category(name: String, raw: String): AstartesCategory =
        AstartesCategory.byNameOrNull(raw) ?: throw InvalidParameterException(Messages.paramOneOf(name))
}
