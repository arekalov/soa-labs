package ru.ifmo.soa.spacemarine.controller

import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.core.MultivaluedMap
import jakarta.ws.rs.core.UriInfo
import ru.ifmo.soa.spacemarine.Messages
import ru.ifmo.soa.spacemarine.exception.InvalidParameterException
import ru.ifmo.soa.spacemarine.query.DEFAULT_PAGE
import ru.ifmo.soa.spacemarine.query.DEFAULT_SIZE
import ru.ifmo.soa.spacemarine.query.SortSpec
import ru.ifmo.soa.spacemarine.query.SpaceMarineField
import ru.ifmo.soa.spacemarine.query.SpaceMarineQuery

const val SORT_PARAM = "sort"
const val PAGE_PARAM = "page"
const val SIZE_PARAM = "size"

/**
 * Сборка [SpaceMarineQuery] из параметров URL.
 *
 * Разбор идёт через [UriInfo], а не через `@QueryParam`: при неудачной конвертации
 * JAX-RS выбрасывает собственное исключение с чужим текстом, и контроль над сообщением
 * теряется. Плюс только так гарантирован порядок повторяющихся значений `sort`,
 * который задаёт приоритет ступеней сортировки.
 */
@ApplicationScoped
open class SpaceMarineQueryParser {
    open fun parse(uriInfo: UriInfo): SpaceMarineQuery = parse(uriInfo.getQueryParameters(true))

    open fun parse(params: MultivaluedMap<String, String>): SpaceMarineQuery = SpaceMarineQuery(
        filters = parseFilters(params),
        sort = parseSort(params[SORT_PARAM]),
        page = ParamParsers.intWithMin(PAGE_PARAM, params.getFirst(PAGE_PARAM), min = 0, default = DEFAULT_PAGE),
        size = ParamParsers.intWithMin(SIZE_PARAM, params.getFirst(SIZE_PARAM), min = 1, default = DEFAULT_SIZE),
    )

    private fun parseFilters(params: MultivaluedMap<String, String>): Map<SpaceMarineField, Any> {
        val filters = LinkedHashMap<SpaceMarineField, Any>()
        for (field in SpaceMarineField.entries) {
            val values = params[field.apiName]?.takeIf { it.isNotEmpty() } ?: continue
            if (values.size > 1) {
                throw InvalidParameterException(Messages.paramDuplicated(field.apiName))
            }
            filters[field] = ParamParsers.parseFilter(field, values.first())
        }
        return filters
    }

    private fun parseSort(rawValues: List<String>?): List<SortSpec> {
        if (rawValues.isNullOrEmpty()) return emptyList()

        val result = mutableListOf<SortSpec>()
        val seen = mutableSetOf<SpaceMarineField>()

        for (token in rawValues) {
            if (token.isBlank()) throw InvalidParameterException(Messages.paramBlank(SORT_PARAM))

            val descending = token.startsWith('-')
            val field = SpaceMarineField.byApiNameOrNull(if (descending) token.substring(1) else token)
                ?: throw InvalidParameterException(Messages.paramUnknownValue(SORT_PARAM, token))

            if (!seen.add(field)) throw InvalidParameterException(Messages.sortFieldDuplicated(field.apiName))

            result += SortSpec(field, descending)
        }
        return result
    }
}
