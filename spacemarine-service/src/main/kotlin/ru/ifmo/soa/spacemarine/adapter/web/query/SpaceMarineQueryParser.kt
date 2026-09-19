package ru.ifmo.soa.spacemarine.adapter.web.query

import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.core.MultivaluedMap
import jakarta.ws.rs.core.UriInfo
import ru.ifmo.soa.spacemarine.adapter.web.error.InvalidParameterException
import ru.ifmo.soa.spacemarine.application.query.SortSpec
import ru.ifmo.soa.spacemarine.application.query.SpaceMarineField
import ru.ifmo.soa.spacemarine.application.query.SpaceMarineQuery

/**
 * Сборка [SpaceMarineQuery] из параметров URL.
 *
 * Разбор идёт через [UriInfo], а не через `@QueryParam`, по трём причинам:
 * при неудачной конвертации `@QueryParam` JAX-RS выбрасывает собственное исключение
 * с чужим текстом, и контроль над сообщением теряется; сырые строки позволяют выдавать
 * формулировки в точности как в спецификации; и только так гарантирован порядок
 * повторяющихся значений `sort`, который задаёт приоритет ступеней сортировки.
 */
@ApplicationScoped
open class SpaceMarineQueryParser {

    open fun parse(uriInfo: UriInfo): SpaceMarineQuery = parse(uriInfo.getQueryParameters(true))

    /**
     * Ядро разбора вынесено на карту параметров, а не на [UriInfo]: так его можно
     * проверить обычным юнит-тестом, не поднимая контейнер.
     */
    open fun parse(params: MultivaluedMap<String, String>): SpaceMarineQuery {
        return SpaceMarineQuery(
            filters = parseFilters(params),
            sort = parseSort(params[SORT_PARAM]),
            page = ParamParsers.intWithMin(
                PAGE_PARAM, params.getFirst(PAGE_PARAM), min = 0, default = SpaceMarineQuery.DEFAULT_PAGE,
            ),
            size = ParamParsers.intWithMin(
                SIZE_PARAM, params.getFirst(SIZE_PARAM), min = 1, default = SpaceMarineQuery.DEFAULT_SIZE,
            ),
        )
    }

    private fun parseFilters(params: MultivaluedMap<String, String>): Map<SpaceMarineField, Any> {
        val filters = LinkedHashMap<SpaceMarineField, Any>()

        for (field in SpaceMarineField.entries) {
            val values = params[field.apiName] ?: continue
            if (values.isEmpty()) continue
            if (values.size > 1) {
                // Фильтр — это точное равенство; два значения означали бы конъюнкцию
                // взаимоисключающих условий, то есть заведомо пустой результат.
                throw InvalidParameterException("Параметр '${field.apiName}' указан более одного раза")
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
            if (token.isBlank()) {
                throw InvalidParameterException("Параметр '$SORT_PARAM' не может быть пустым")
            }

            val descending = token.startsWith('-')
            val fieldName = if (descending) token.substring(1) else token
            val field = SpaceMarineField.byApiNameOrNull(fieldName)
                ?: throw InvalidParameterException(
                    "Параметр '$SORT_PARAM' содержит недопустимое значение '$token'",
                )

            if (!seen.add(field)) {
                // Повтор поля не имеет смысла: вторая ступень по тому же полю никогда
                // не применится, а молча её проглотить значило бы скрыть ошибку клиента.
                throw InvalidParameterException(
                    "Поле '${field.apiName}' указано в параметре '$SORT_PARAM' более одного раза",
                )
            }
            result += SortSpec(field, descending)
        }
        return result
    }

    private companion object {
        const val SORT_PARAM = "sort"
        const val PAGE_PARAM = "page"
        const val SIZE_PARAM = "size"
    }
}
