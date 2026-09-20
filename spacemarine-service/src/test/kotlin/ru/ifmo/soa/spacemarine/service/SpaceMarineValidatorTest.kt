package ru.ifmo.soa.spacemarine.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import ru.ifmo.soa.spacemarine.dto.ChapterDto
import ru.ifmo.soa.spacemarine.dto.CoordinatesDto
import ru.ifmo.soa.spacemarine.dto.SpaceMarineInputDto
import ru.ifmo.soa.spacemarine.exception.ValidationException
import ru.ifmo.soa.spacemarine.model.AstartesCategory

class SpaceMarineValidatorTest {

    private fun valid(
        name: String? = "Brother Marius",
        coordinates: CoordinatesDto? = CoordinatesDto(10, 3.5),
        health: Float? = 85.5f,
        loyal: Boolean? = true,
        achievements: String? = null,
        category: AstartesCategory? = AstartesCategory.TACTICAL,
        chapter: ChapterDto? = ChapterDto("Ultramarines", "XIII Legion"),
    ) = SpaceMarineInputDto(name, coordinates, health, loyal, achievements, category, chapter)

    private fun violationsOf(input: SpaceMarineInputDto): List<String> = runCatching {
        SpaceMarineValidator.validate(input)
    }.exceptionOrNull().let { (it as ValidationException).details }

    @Test
    @DisplayName("корректное тело проходит проверку")
    fun `accepts a valid input`() {
        assertThatCode { SpaceMarineValidator.validate(valid()) }.doesNotThrowAnyException()
    }

    @Test
    @DisplayName("орден необязателен")
    fun `chapter is optional`() {
        assertThatCode { SpaceMarineValidator.validate(valid(chapter = null)) }.doesNotThrowAnyException()
    }

    @Test
    @DisplayName("достижения могут отсутствовать")
    fun `achievements are optional`() {
        assertThatCode { SpaceMarineValidator.validate(valid(achievements = null)) }.doesNotThrowAnyException()
    }

    @Test
    @DisplayName("граничное значение координаты y допустимо")
    fun `y equal to the bound is allowed`() {
        assertThatCode { SpaceMarineValidator.validate(valid(coordinates = CoordinatesDto(1, 12.0))) }
            .doesNotThrowAnyException()
    }

    @Test
    @DisplayName("отсутствующее имя — нарушение, а не ошибка разбора")
    fun `null name is a violation`() {
        assertThat(violationsOf(valid(name = null))).containsExactly("name: поле не может быть null")
    }

    @Test
    @DisplayName("пустое имя отвергается")
    fun `blank name is a violation`() {
        assertThat(violationsOf(valid(name = "   "))).containsExactly("name: строка не может быть пустой")
    }

    @Test
    @DisplayName("здоровье должно быть строго больше нуля")
    fun `health must be positive`() {
        assertThat(violationsOf(valid(health = 0f))).containsExactly("health: значение должно быть больше 0")
        assertThat(violationsOf(valid(health = -1f))).containsExactly("health: значение должно быть больше 0")
    }

    @Test
    @DisplayName("NaN в здоровье не проскакивает мимо проверки")
    fun `health rejects NaN`() {
        assertThat(violationsOf(valid(health = Float.NaN))).containsExactly("health: значение должно быть больше 0")
    }

    @Test
    @DisplayName("координата y ограничена сверху")
    fun `y above the bound is a violation`() {
        assertThat(violationsOf(valid(coordinates = CoordinatesDto(1, 12.1))))
            .containsExactly("coordinates.y: максимальное значение поля — 12")
    }

    @Test
    @DisplayName("NaN в координате y отвергается как не число")
    fun `y rejects NaN`() {
        assertThat(violationsOf(valid(coordinates = CoordinatesDto(1, Double.NaN))))
            .containsExactly("coordinates.y: значение должно быть числом")
    }

    @Test
    @DisplayName("отсутствующие координаты дают одно нарушение, а не три")
    fun `null coordinates report once`() {
        assertThat(violationsOf(valid(coordinates = null)))
            .containsExactly("coordinates: поле не может быть null")
    }

    @Test
    @DisplayName("пустые поля координат перечисляются отдельно")
    fun `coordinate fields are reported separately`() {
        assertThat(violationsOf(valid(coordinates = CoordinatesDto(null, null))))
            .containsExactly("coordinates.x: поле не может быть null", "coordinates.y: поле не может быть null")
    }

    @Test
    @DisplayName("верность и категория обязательны")
    fun `loyal and category are required`() {
        assertThat(violationsOf(valid(loyal = null, category = null)))
            .containsExactly("loyal: поле не может быть null", "category: поле не может быть null")
    }

    @Test
    @DisplayName("у заданного ордена название обязано быть непустым")
    fun `chapter name must not be blank`() {
        assertThat(violationsOf(valid(chapter = ChapterDto(null, "Legion"))))
            .containsExactly("chapter.name: поле не может быть null")
        assertThat(violationsOf(valid(chapter = ChapterDto("  ", null))))
            .containsExactly("chapter.name: строка не может быть пустой")
    }

    @Test
    @DisplayName("сообщаются все нарушения сразу, а не первое попавшееся")
    fun `reports every violation at once`() {
        val violations = violationsOf(SpaceMarineInputDto())

        assertThat(violations).containsExactly(
            "name: поле не может быть null",
            "coordinates: поле не может быть null",
            "health: поле не может быть null",
            "loyal: поле не может быть null",
            "category: поле не может быть null",
        )
    }

    @Test
    @DisplayName("нельзя сообщить о нарушениях, не указав ни одного")
    fun `empty violation list is rejected`() {
        assertThatThrownBy { ValidationException(emptyList()) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }
}
