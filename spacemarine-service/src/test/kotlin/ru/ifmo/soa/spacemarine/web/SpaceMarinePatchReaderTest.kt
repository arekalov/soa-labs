package ru.ifmo.soa.spacemarine.web

import com.fasterxml.jackson.databind.node.ObjectNode
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import ru.ifmo.soa.spacemarine.adapter.web.JsonMappers
import ru.ifmo.soa.spacemarine.adapter.web.SpaceMarinePatchReader
import ru.ifmo.soa.spacemarine.adapter.web.error.MalformedRequestBodyException
import ru.ifmo.soa.spacemarine.domain.model.AstartesCategory
import ru.ifmo.soa.spacemarine.domain.model.ChapterDraft
import ru.ifmo.soa.spacemarine.domain.model.CoordinatesDraft
import ru.ifmo.soa.spacemarine.domain.model.SpaceMarineDraft
import ru.ifmo.soa.spacemarine.domain.model.SpaceMarineFactory
import ru.ifmo.soa.spacemarine.domain.validation.DomainValidationException
import java.time.Instant

/**
 * Проверяет главное свойство PATCH: различение «поля нет», «поле = null» и «неверный тип».
 */
class SpaceMarinePatchReaderTest {

    private val reader = SpaceMarinePatchReader()

    private fun patchOf(json: String): ObjectNode =
        JsonMappers.mapper.readTree(json) as ObjectNode

    private val existing = SpaceMarineFactory.create(
        SpaceMarineDraft(
            name = "Brother Marius",
            coordinates = CoordinatesDraft(10, 3.5),
            health = 85.5f,
            loyal = true,
            achievements = "Hero of Macragge",
            category = AstartesCategory.TACTICAL,
            chapter = ChapterDraft("Ultramarines", "XIII Legion"),
        ),
        creationDate = Instant.parse("2026-09-10T12:00:00Z"),
    ).copy(id = 1)

    /** Применяет патч целиком: чтение, наложение и проверка домена — как в рабочем сценарии. */
    private fun apply(json: String) =
        SpaceMarineFactory.update(existing, reader.read(patchOf(json)).applyTo(existing))

    @Test
    @DisplayName("пустое тело ничего не меняет")
    fun `empty patch changes nothing`() {
        val result = apply("{}")

        assertThat(result.name).isEqualTo(existing.name)
        assertThat(result.health).isEqualTo(existing.health)
        assertThat(result.achievements).isEqualTo(existing.achievements)
        assertThat(result.chapter).isEqualTo(existing.chapter)
    }

    @Test
    @DisplayName("явный null обнуляет поле, которое спецификация разрешает обнулять")
    fun `explicit null clears nullable field`() {
        assertThat(apply("""{"achievements": null}""").achievements).isNull()
        assertThat(apply("""{"chapter": null}""").chapter).isNull()
    }

    @Test
    @DisplayName("явный null в обязательном поле — нарушение целостности (422), а не сброс к прежнему значению")
    fun `explicit null on required field is a violation`() {
        assertThatThrownBy { apply("""{"name": null}""") }
            .isInstanceOf(DomainValidationException::class.java)
            .satisfies({ thrown ->
                assertThat((thrown as DomainValidationException).details)
                    .containsExactly("name: поле не может быть null")
            })
    }

    @Test
    @DisplayName("отсутствие поля и null различаются: без этого достижения нельзя было бы очистить")
    fun `absent differs from null`() {
        assertThat(apply("{}").achievements).isEqualTo("Hero of Macragge")
        assertThat(apply("""{"achievements": null}""").achievements).isNull()
    }

    @Test
    @DisplayName("неверный тип значения — 400, а недопустимое значение — 422")
    fun `type error is 400 and domain error is 422`() {
        assertThatThrownBy { apply("""{"health": "abc"}""") }
            .isInstanceOf(MalformedRequestBodyException::class.java)

        assertThatThrownBy { apply("""{"health": 0}""") }
            .isInstanceOf(DomainValidationException::class.java)
    }

    @Test
    @DisplayName("вложенный объект заменяется целиком, без слияния с текущим состоянием")
    fun `nested object is replaced wholly`() {
        val result = apply("""{"chapter": {"name": "Imperial Fists"}}""")

        assertThat(result.chapter?.name).isEqualTo("Imperial Fists")
        // Прежний легион не «протёк» из текущего состояния — объект заменён, а не слит
        assertThat(result.chapter?.parentLegion).isNull()
    }

    @Test
    @DisplayName("частично заданный вложенный объект отвергается доменом")
    fun `partial nested object is rejected`() {
        assertThatThrownBy { apply("""{"chapter": {"parentLegion": "XIII Legion"}}""") }
            .isInstanceOf(DomainValidationException::class.java)
    }

    @Test
    @DisplayName("readOnly-поля в теле игнорируются, а не вызывают ошибку")
    fun `readonly fields are ignored`() {
        val result = apply("""{"id": 999, "creationDate": "2000-01-01T00:00:00Z"}""")

        assertThat(result.id).isEqualTo(1)
        assertThat(result.creationDate).isEqualTo(existing.creationDate)
    }

    @Test
    @DisplayName("координаты принимают только числа")
    fun `coordinates must be numeric`() {
        assertThatThrownBy { apply("""{"coordinates": {"x": "ten", "y": 3.5}}""") }
            .isInstanceOf(MalformedRequestBodyException::class.java)

        assertThatThrownBy { apply("""{"coordinates": 5}""") }
            .isInstanceOf(MalformedRequestBodyException::class.java)
    }

    @Test
    @DisplayName("граница coordinates.y соблюдается и в PATCH")
    fun `patch respects coordinate bound`() {
        assertThat(apply("""{"coordinates": {"x": 1, "y": 12}}""").coordinates.y).isEqualTo(12.0)

        assertThatThrownBy { apply("""{"coordinates": {"x": 1, "y": 13}}""") }
            .isInstanceOf(DomainValidationException::class.java)
    }
}
