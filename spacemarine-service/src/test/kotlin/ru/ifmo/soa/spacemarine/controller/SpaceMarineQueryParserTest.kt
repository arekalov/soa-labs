package ru.ifmo.soa.spacemarine.controller

import jakarta.ws.rs.core.MultivaluedHashMap
import jakarta.ws.rs.core.MultivaluedMap
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import ru.ifmo.soa.spacemarine.exception.InvalidParameterException
import ru.ifmo.soa.spacemarine.model.AstartesCategory
import ru.ifmo.soa.spacemarine.query.SpaceMarineField
import java.time.Instant

class SpaceMarineQueryParserTest {

    private val parser = SpaceMarineQueryParser()

    private fun params(vararg pairs: Pair<String, String>): MultivaluedMap<String, String> {
        val map = MultivaluedHashMap<String, String>()
        pairs.forEach { (key, value) -> map.add(key, value) }
        return map
    }

    @Test
    @DisplayName("без параметров действуют значения по умолчанию")
    fun `defaults apply`() {
        val query = parser.parse(params())

        assertThat(query.filters).isEmpty()
        assertThat(query.sort).isEmpty()
        assertThat(query.page).isZero()
        assertThat(query.size).isEqualTo(20)
    }

    @Test
    @DisplayName("фильтры разбираются по типу поля")
    fun `filters are typed`() {
        val query = parser.parse(
            params(
                "id" to "7",
                "name" to "Marius",
                "coordinatesX" to "-3",
                "coordinatesY" to "12",
                "health" to "50.5",
                "loyal" to "true",
                "category" to "HELIX",
                "creationDate" to "2026-09-10T12:00:00Z",
            ),
        )

        assertThat(query.filters[SpaceMarineField.ID]).isEqualTo(7)
        assertThat(query.filters[SpaceMarineField.NAME]).isEqualTo("Marius")
        assertThat(query.filters[SpaceMarineField.COORDINATES_X]).isEqualTo(-3)
        assertThat(query.filters[SpaceMarineField.COORDINATES_Y]).isEqualTo(12.0)
        assertThat(query.filters[SpaceMarineField.HEALTH]).isEqualTo(50.5f)
        assertThat(query.filters[SpaceMarineField.LOYAL]).isEqualTo(true)
        assertThat(query.filters[SpaceMarineField.CATEGORY]).isEqualTo(AstartesCategory.HELIX)
        assertThat(query.filters[SpaceMarineField.CREATION_DATE])
            .isEqualTo(Instant.parse("2026-09-10T12:00:00Z"))
    }

    @Test
    @DisplayName("порядок значений sort задаёт приоритет ступеней")
    fun `sort keeps the given order`() {
        val query = parser.parse(params("sort" to "-health", "sort" to "name"))

        assertThat(query.sort.map { it.field })
            .containsExactly(SpaceMarineField.HEALTH, SpaceMarineField.NAME)
        assertThat(query.sort.map { it.descending }).containsExactly(true, false)
    }

    @Test
    @DisplayName("нечисловой идентификатор — 400, а не пустая выборка")
    fun `non numeric id is rejected`() {
        assertThatThrownBy { parser.parse(params("id" to "abc")) }
            .isInstanceOf(InvalidParameterException::class.java)
            .hasMessage("Параметр 'id' должен быть целым числом больше 0")
    }

    @Test
    @DisplayName("недопустимая верность отвергается, а не трактуется как false")
    fun `loyal accepts only true and false`() {
        assertThatThrownBy { parser.parse(params("loyal" to "maybe")) }
            .isInstanceOf(InvalidParameterException::class.java)
            .hasMessage("Параметр 'loyal' должен быть true или false")
    }

    @Test
    @DisplayName("координата y ограничена сверху и в фильтре")
    fun `coordinates y is bounded`() {
        assertThatThrownBy { parser.parse(params("coordinatesY" to "12.5")) }
            .isInstanceOf(InvalidParameterException::class.java)
            .hasMessage("Параметр 'coordinatesY' не может превышать 12")
    }

    @Test
    @DisplayName("повтор фильтра — ошибка, а не заведомо пустой результат")
    fun `duplicate filter is rejected`() {
        assertThatThrownBy { parser.parse(params("name" to "A", "name" to "B")) }
            .isInstanceOf(InvalidParameterException::class.java)
            .hasMessage("Параметр 'name' указан более одного раза")
    }

    @Test
    @DisplayName("неизвестное поле сортировки отвергается")
    fun `unknown sort field is rejected`() {
        assertThatThrownBy { parser.parse(params("sort" to "weight")) }
            .isInstanceOf(InvalidParameterException::class.java)
            .hasMessage("Параметр 'sort' содержит недопустимое значение 'weight'")
    }

    @Test
    @DisplayName("повтор поля в сортировке отвергается")
    fun `duplicate sort field is rejected`() {
        assertThatThrownBy { parser.parse(params("sort" to "name", "sort" to "-name")) }
            .isInstanceOf(InvalidParameterException::class.java)
            .hasMessage("Поле 'name' указано в параметре 'sort' более одного раза")
    }

    @Test
    @DisplayName("отрицательная страница и нулевой размер отвергаются")
    fun `page and size are bounded`() {
        assertThatThrownBy { parser.parse(params("page" to "-1")) }
            .isInstanceOf(InvalidParameterException::class.java)
            .hasMessage("Параметр 'page' должен быть целым числом не меньше 0")

        assertThatThrownBy { parser.parse(params("size" to "0")) }
            .isInstanceOf(InvalidParameterException::class.java)
            .hasMessage("Параметр 'size' должен быть целым числом не меньше 1")
    }

    @Test
    @DisplayName("пустое имя в фильтре отвергается")
    fun `blank name filter is rejected`() {
        assertThatThrownBy { parser.parse(params("name" to "   ")) }
            .isInstanceOf(InvalidParameterException::class.java)
            .hasMessage("Параметр 'name' не может быть пустым")
    }

    @Test
    @DisplayName("неизвестные параметры не мешают разбору")
    fun `unknown parameters are ignored`() {
        val query = parser.parse(params("unknown" to "value", "size" to "5"))

        assertThat(query.filters).isEmpty()
        assertThat(query.size).isEqualTo(5)
    }
}
