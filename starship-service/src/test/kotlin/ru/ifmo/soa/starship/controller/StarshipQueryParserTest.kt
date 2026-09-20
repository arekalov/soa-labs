package ru.ifmo.soa.starship.controller

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Sort
import ru.ifmo.soa.starship.exception.InvalidParameterException

class StarshipQueryParserTest {
    private val parser = StarshipQueryParser()

    private fun parse(
        id: String? = null,
        name: String? = null,
        sort: List<String>? = null,
        page: String? = null,
        size: String? = null,
    ) = parser.parse(id, name, sort, page, size)

    @Test
    @DisplayName("без параметров действуют значения по умолчанию, порядок — по возрастанию id")
    fun `defaults apply`() {
        val query = parse()

        assertThat(query.filter.id).isNull()
        assertThat(query.filter.name).isNull()
        assertThat(query.pageable.pageNumber).isZero()
        assertThat(query.pageable.pageSize).isEqualTo(20)
        assertThat(query.pageable.sort).isEqualTo(Sort.by(Sort.Order.asc("id")))
    }

    @Test
    @DisplayName("фильтры разбираются на точное совпадение")
    fun `filters are exact`() {
        val query = parse(id = "3", name = "Fist of Dorn")

        assertThat(query.filter.id).isEqualTo(3L)
        assertThat(query.filter.name).isEqualTo("Fist of Dorn")
    }

    @Test
    @DisplayName("порядок значений sort задаёт приоритет ступеней")
    fun `sort keeps the given order`() {
        val query = parse(sort = listOf("-name", "id"))

        assertThat(query.pageable.sort)
            .isEqualTo(Sort.by(Sort.Order.desc("name"), Sort.Order.asc("id")))
    }

    @Test
    @DisplayName("к сортировке без id добавляется стабилизатор")
    fun `sort gets a tie breaker`() {
        val query = parse(sort = listOf("name"))

        assertThat(query.pageable.sort)
            .isEqualTo(Sort.by(Sort.Order.asc("name"), Sort.Order.asc("id")))
    }

    @Test
    @DisplayName("нечисловой идентификатор отвергается")
    fun `non numeric id is rejected`() {
        assertThatThrownBy { parse(id = "abc") }
            .isInstanceOf(InvalidParameterException::class.java)
            .hasMessage("Параметр 'id' должен быть целым числом больше 0")
    }

    @Test
    @DisplayName("пустое название в фильтре отвергается")
    fun `blank name filter is rejected`() {
        assertThatThrownBy { parse(name = " ") }
            .isInstanceOf(InvalidParameterException::class.java)
            .hasMessage("Параметр 'name' не может быть пустым")
    }

    @Test
    @DisplayName("неизвестное поле сортировки отвергается")
    fun `unknown sort field is rejected`() {
        assertThatThrownBy { parse(sort = listOf("crew")) }
            .isInstanceOf(InvalidParameterException::class.java)
            .hasMessage("Параметр 'sort' содержит недопустимое значение 'crew'")
    }

    @Test
    @DisplayName("повтор поля в сортировке отвергается")
    fun `duplicate sort field is rejected`() {
        assertThatThrownBy { parse(sort = listOf("name", "-name")) }
            .isInstanceOf(InvalidParameterException::class.java)
            .hasMessage("Поле 'name' указано в параметре 'sort' более одного раза")
    }

    @Test
    @DisplayName("отрицательная страница и нулевой размер отвергаются")
    fun `page and size are bounded`() {
        assertThatThrownBy { parse(page = "-1") }
            .isInstanceOf(InvalidParameterException::class.java)
            .hasMessage("Параметр 'page' должен быть целым числом не меньше 0")

        assertThatThrownBy { parse(size = "0") }
            .isInstanceOf(InvalidParameterException::class.java)
            .hasMessage("Параметр 'size' должен быть целым числом не меньше 1")
    }
}
