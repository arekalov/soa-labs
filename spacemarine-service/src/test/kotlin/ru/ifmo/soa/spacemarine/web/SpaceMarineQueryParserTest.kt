package ru.ifmo.soa.spacemarine.web

import jakarta.ws.rs.core.MultivaluedHashMap
import jakarta.ws.rs.core.MultivaluedMap
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import ru.ifmo.soa.spacemarine.adapter.web.error.InvalidParameterException
import ru.ifmo.soa.spacemarine.adapter.web.query.SpaceMarineQueryParser
import ru.ifmo.soa.spacemarine.application.query.SortSpec
import ru.ifmo.soa.spacemarine.application.query.SpaceMarineField
import ru.ifmo.soa.spacemarine.application.query.SpaceMarineQuery
import ru.ifmo.soa.spacemarine.domain.model.AstartesCategory
import java.time.Instant

class SpaceMarineQueryParserTest {

    private val parser = SpaceMarineQueryParser()

    private fun params(vararg pairs: Pair<String, String>): MultivaluedMap<String, String> {
        val map = MultivaluedHashMap<String, String>()
        pairs.forEach { (key, value) -> map.add(key, value) }
        return map
    }

    @Test
    @DisplayName("без параметров действуют значения по умолчанию из спецификации")
    fun `defaults match specification`() {
        val query = parser.parse(params())

        assertThat(query.page).isEqualTo(SpaceMarineQuery.DEFAULT_PAGE).isZero()
        assertThat(query.size).isEqualTo(SpaceMarineQuery.DEFAULT_SIZE).isEqualTo(20)
        assertThat(query.filters).isEmpty()
        assertThat(query.sort).isEmpty()
    }

    @Test
    @DisplayName("порядок значений sort задаёт приоритет ступеней сортировки")
    fun `sort order is preserved`() {
        val query = parser.parse(params("sort" to "-health", "sort" to "name"))

        assertThat(query.sort).containsExactly(
            SortSpec(SpaceMarineField.HEALTH, descending = true),
            SortSpec(SpaceMarineField.NAME, descending = false),
        )
    }

    @Test
    @DisplayName("все 22 допустимых значения sort принимаются")
    fun `accepts every documented sort token`() {
        SpaceMarineField.SORT_TOKENS.forEach { token ->
            val query = parser.parse(params("sort" to token))
            assertThat(query.sort).hasSize(1)
        }
        assertThat(SpaceMarineField.SORT_TOKENS).hasSize(22)
    }

    @ParameterizedTest
    @ValueSource(strings = ["hp", "-hp", "HEALTH", "--health", "coordinates.x", " "])
    @DisplayName("недопустимое значение sort даёт 400, а не тихо игнорируется")
    fun `rejects unknown sort token`(token: String) {
        assertThatThrownBy { parser.parse(params("sort" to token)) }
            .isInstanceOf(InvalidParameterException::class.java)
    }

    @Test
    @DisplayName("повтор поля в sort отвергается: вторая ступень всё равно не применилась бы")
    fun `rejects duplicate sort field`() {
        assertThatThrownBy { parser.parse(params("sort" to "health", "sort" to "-health")) }
            .isInstanceOf(InvalidParameterException::class.java)
            .hasMessageContaining("более одного раза")
    }

    @Test
    @DisplayName("фильтры разбираются в типизированные значения")
    fun `parses typed filters`() {
        val query = parser.parse(
            params(
                "id" to "5",
                "name" to "Brother Marius",
                "coordinatesX" to "10",
                "coordinatesY" to "3.5",
                "creationDate" to "2026-09-10T12:00:00Z",
                "health" to "85.5",
                "loyal" to "true",
                "category" to "TACTICAL",
                "chapterName" to "Ultramarines",
            ),
        )

        assertThat(query.filters).containsEntry(SpaceMarineField.ID, 5)
        assertThat(query.filters).containsEntry(SpaceMarineField.COORDINATES_X, 10)
        assertThat(query.filters).containsEntry(SpaceMarineField.COORDINATES_Y, 3.5)
        assertThat(query.filters).containsEntry(SpaceMarineField.HEALTH, 85.5f)
        assertThat(query.filters).containsEntry(SpaceMarineField.LOYAL, true)
        assertThat(query.filters).containsEntry(SpaceMarineField.CATEGORY, AstartesCategory.TACTICAL)
        assertThat(query.filters)
            .containsEntry(SpaceMarineField.CREATION_DATE, Instant.parse("2026-09-10T12:00:00Z"))
    }

    @ParameterizedTest
    @ValueSource(strings = ["abc", "0", "-1", "3.5", ""])
    @DisplayName("фильтр id обязан быть целым больше нуля")
    fun `id filter must be positive integer`(raw: String) {
        assertThatThrownBy { parser.parse(params("id" to raw)) }
            .isInstanceOf(InvalidParameterException::class.java)
            .hasMessage("Параметр 'id' должен быть целым числом больше 0")
    }

    @Test
    @DisplayName("loyal принимает только true и false: иначе фильтр молча стал бы false")
    fun `loyal filter is strict`() {
        assertThat(parser.parse(params("loyal" to "false")).filters)
            .containsEntry(SpaceMarineField.LOYAL, false)

        assertThatThrownBy { parser.parse(params("loyal" to "maybe")) }
            .isInstanceOf(InvalidParameterException::class.java)
    }

    @Test
    @DisplayName("ограничения схемы проверяются и на фильтрах, а не только на теле")
    fun `filters respect schema bounds`() {
        assertThatThrownBy { parser.parse(params("coordinatesY" to "13")) }
            .isInstanceOf(InvalidParameterException::class.java)

        assertThatThrownBy { parser.parse(params("health" to "-1")) }
            .isInstanceOf(InvalidParameterException::class.java)

        assertThatThrownBy { parser.parse(params("category" to "WIZARD")) }
            .isInstanceOf(InvalidParameterException::class.java)
    }

    @Test
    @DisplayName("один и тот же фильтр дважды — ошибка, а не пустая выборка")
    fun `rejects repeated filter`() {
        assertThatThrownBy { parser.parse(params("name" to "A", "name" to "B")) }
            .isInstanceOf(InvalidParameterException::class.java)
            .hasMessageContaining("более одного раза")
    }

    @Test
    @DisplayName("границы пагинации: page от нуля, size от единицы")
    fun `pagination bounds`() {
        assertThat(parser.parse(params("page" to "0", "size" to "1")).size).isEqualTo(1)

        assertThatThrownBy { parser.parse(params("page" to "-1")) }
            .isInstanceOf(InvalidParameterException::class.java)

        assertThatThrownBy { parser.parse(params("size" to "0")) }
            .isInstanceOf(InvalidParameterException::class.java)
    }

    @Test
    @DisplayName("смещение считается в Long: большой page не должен переполнять Int")
    fun `offset does not overflow`() {
        val query = parser.parse(params("page" to "${Int.MAX_VALUE}", "size" to "20"))

        assertThat(query.offset).isEqualTo(Int.MAX_VALUE.toLong() * 20)
        assertThat(query.offset).isGreaterThan(Int.MAX_VALUE.toLong())
    }
}
