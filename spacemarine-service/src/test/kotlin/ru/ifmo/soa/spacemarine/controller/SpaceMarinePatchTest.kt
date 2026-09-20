package ru.ifmo.soa.spacemarine.controller

import com.fasterxml.jackson.databind.node.ObjectNode
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import ru.ifmo.soa.spacemarine.config.jsonMapper
import ru.ifmo.soa.spacemarine.dto.ChapterDto
import ru.ifmo.soa.spacemarine.dto.CoordinatesDto
import ru.ifmo.soa.spacemarine.dto.SpaceMarineInputDto
import ru.ifmo.soa.spacemarine.model.AstartesCategory

/**
 * Главное свойство PATCH: различимы «поля нет в теле» и «поле равно null».
 * Без этого очистить достижения или орден было бы нельзя.
 */
class SpaceMarinePatchTest {
    private fun current() = SpaceMarineInputDto(
        name = "Brother Marius",
        coordinates = CoordinatesDto(10, 3.5),
        health = 85.5f,
        loyal = true,
        achievements = "Terra",
        category = AstartesCategory.TACTICAL,
        chapter = ChapterDto("Ultramarines", "XIII Legion"),
    )

    private fun patch(json: String): SpaceMarineInputDto =
        jsonMapper.readerForUpdating(current()).readValue(jsonMapper.readTree(json) as ObjectNode)

    @Test
    @DisplayName("пустое тело ничего не меняет")
    fun `empty body changes nothing`() {
        val result = patch("{}")

        assertThat(result.name).isEqualTo("Brother Marius")
        assertThat(result.achievements).isEqualTo("Terra")
        assertThat(result.chapter?.name).isEqualTo("Ultramarines")
    }

    @Test
    @DisplayName("явный null очищает поле, отсутствие поля — нет")
    fun `explicit null clears the field`() {
        assertThat(patch("""{"achievements": null}""").achievements).isNull()
        assertThat(patch("""{"name": "Marius"}""").achievements).isEqualTo("Terra")
    }

    @Test
    @DisplayName("орден можно убрать целиком")
    fun `chapter can be cleared`() {
        assertThat(patch("""{"chapter": null}""").chapter).isNull()
    }

    @Test
    @DisplayName("меняются только перечисленные поля")
    fun `only listed fields change`() {
        val result = patch("""{"health": 42.5, "category": "HELIX"}""")

        assertThat(result.health).isEqualTo(42.5f)
        assertThat(result.category).isEqualTo(AstartesCategory.HELIX)
        assertThat(result.name).isEqualTo("Brother Marius")
        assertThat(result.loyal).isTrue()
    }

    @Test
    @DisplayName("вложенный объект заменяется целиком, а не сливается")
    fun `nested object is replaced`() {
        val result = patch("""{"coordinates": {"x": 1}}""")

        assertThat(result.coordinates?.x).isEqualTo(1)
        assertThat(result.coordinates?.y).isNull()
    }

    @Test
    @DisplayName("readOnly-поля в теле игнорируются")
    fun `read only fields are ignored`() {
        assertThat(patch("""{"id": 5, "creationDate": "2026-01-01T00:00:00Z"}""").name)
            .isEqualTo("Brother Marius")
    }

    @Test
    @DisplayName("несовпадение типа — ошибка разбора, а не нарушение ограничений")
    fun `type mismatch is a parse error`() {
        assertThatThrownBy { patch("""{"name": 42}""") }
            .isInstanceOf(com.fasterxml.jackson.core.JsonProcessingException::class.java)
        assertThatThrownBy { patch("""{"category": "PRIEST"}""") }
            .isInstanceOf(com.fasterxml.jackson.core.JsonProcessingException::class.java)
    }
}
