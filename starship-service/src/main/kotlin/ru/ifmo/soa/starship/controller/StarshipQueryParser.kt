package ru.ifmo.soa.starship.controller

import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component
import ru.ifmo.soa.starship.Messages
import ru.ifmo.soa.starship.exception.InvalidParameterException
import ru.ifmo.soa.starship.query.DEFAULT_PAGE
import ru.ifmo.soa.starship.query.DEFAULT_SIZE
import ru.ifmo.soa.starship.query.StarshipField
import ru.ifmo.soa.starship.query.StarshipFilter
import ru.ifmo.soa.starship.query.StarshipQuery

const val SORT_PARAM = "sort"
const val PAGE_PARAM = "page"
const val SIZE_PARAM = "size"

/** Сборка [StarshipQuery] из параметров URL. Любое несоответствие схеме — 400. */
@Component
class StarshipQueryParser {
    fun parse(id: String?, name: String?, sort: List<String>?, page: String?, size: String?): StarshipQuery =
        StarshipQuery(
            filter = parseFilter(id, name),
            pageable = PageRequest.of(
                intWithMin(PAGE_PARAM, page, min = 0, default = DEFAULT_PAGE),
                intWithMin(SIZE_PARAM, size, min = 1, default = DEFAULT_SIZE),
                parseSort(sort),
            ),
        )

    private fun parseFilter(id: String?, name: String?) = StarshipFilter(
        id = id?.let {
            it.toLongOrNull()?.takeIf { value -> value > 0 }
                ?: throw InvalidParameterException(Messages.paramPositiveInt("id"))
        },
        name = name?.also {
            if (it.isBlank()) throw InvalidParameterException(Messages.paramBlank("name"))
        },
    )

    private fun parseSort(tokens: List<String>?): Sort {
        val seen = mutableSetOf<StarshipField>()
        val orders = (tokens ?: emptyList()).map { token ->
            val descending = token.startsWith('-')
            val field = StarshipField.byApiNameOrNull(if (descending) token.substring(1) else token)
                ?: throw InvalidParameterException(Messages.paramUnknownValue(SORT_PARAM, token))
            if (!seen.add(field)) throw InvalidParameterException(Messages.sortFieldDuplicated(field.apiName))

            if (descending) Sort.Order.desc(field.apiName) else Sort.Order.asc(field.apiName)
        }.toMutableList()

        // Без стабилизатора соседние страницы начнут давать дубли и пропуски.
        if (StarshipField.ID !in seen) orders += Sort.Order.asc(StarshipField.ID.apiName)

        return Sort.by(orders)
    }

    private fun intWithMin(name: String, raw: String?, min: Int, default: Int): Int {
        if (raw == null) return default
        return raw.toIntOrNull()?.takeIf { it >= min }
            ?: throw InvalidParameterException(Messages.paramIntWithMin(name, min))
    }
}
