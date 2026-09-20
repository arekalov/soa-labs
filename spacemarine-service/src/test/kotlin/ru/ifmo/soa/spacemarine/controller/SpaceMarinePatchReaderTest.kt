package ru.ifmo.soa.spacemarine.controller

import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import ru.ifmo.soa.spacemarine.exception.MalformedRequestBodyException
import ru.ifmo.soa.spacemarine.model.AstartesCategory

class SpaceMarinePatchReaderTest {

    private val mapper = JsonMapper.builder().addModule(KotlinModule.Builder().build()).build()
    private val reader = SpaceMarinePatchReader()

    private fun read(json: String) = reader.read(mapper.readTree(json) as ObjectNode)

    @Test
    @DisplayName("пустое тело — патч, который ничего не меняет")
    fun `empty body changes nothing`() {
        val patch = read("{}")

        assertThat(patch.name).isNull()
        assertThat(patch.coordinates).isNull()
        assertThat(patch.health).isNull()
        assertThat(patch.chapter).isNull()
    }

    @Test
    @DisplayName("отсутствующее тело равнозначно пустому")
    fun `null body changes nothing`() {
        assertThat(reader.read(null).name).isNull()
    }

    @Test
    @DisplayName("явный null отличается от отсутствия поля")
    fun `explicit null differs from an absent field`() {
        val patch = read("""{"achievements": null}""")

        assertThat(patch.achievements).isNotNull()
        assertThat(patch.achievements?.value).isNull()
        assertThat(patch.name).isNull()
    }

    @Test
    @DisplayName("значения читаются по типам")
    fun `reads typed values`() {
        val patch = read(
            """{"name":"Marius","health":42.5,"loyal":false,"category":"HELIX"}""",
        )

        assertThat(patch.name?.value).isEqualTo("Marius")
        assertThat(patch.health?.value).isEqualTo(42.5f)
        assertThat(patch.loyal?.value).isEqualTo(false)
        assertThat(patch.category?.value).isEqualTo(AstartesCategory.HELIX)
    }

    @Test
    @DisplayName("вложенные объекты читаются целиком")
    fun `reads nested objects`() {
        val patch = read("""{"coordinates":{"x":1,"y":2.5},"chapter":{"name":"Fists","parentLegion":null}}""")

        assertThat(patch.coordinates?.value?.x).isEqualTo(1)
        assertThat(patch.coordinates?.value?.y).isEqualTo(2.5)
        assertThat(patch.chapter?.value?.name).isEqualTo("Fists")
        assertThat(patch.chapter?.value?.parentLegion).isNull()
    }

    @Test
    @DisplayName("несовпадение типа — ошибка разбора, а не нарушение ограничений")
    fun `type mismatch is a parse error`() {
        assertThatThrownBy { read("""{"name": 42}""") }
            .isInstanceOf(MalformedRequestBodyException::class.java)
            .hasMessage("Поле 'name' должно быть строкой")

        assertThatThrownBy { read("""{"health": "много"}""") }
            .isInstanceOf(MalformedRequestBodyException::class.java)
            .hasMessage("Поле 'health' должно быть числом")

        assertThatThrownBy { read("""{"loyal": "да"}""") }
            .isInstanceOf(MalformedRequestBodyException::class.java)
            .hasMessage("Поле 'loyal' должно быть true или false")

        assertThatThrownBy { read("""{"coordinates": 5}""") }
            .isInstanceOf(MalformedRequestBodyException::class.java)
            .hasMessage("Поле 'coordinates' должно быть объектом")

        assertThatThrownBy { read("""{"coordinates": {"x": 1.5}}""") }
            .isInstanceOf(MalformedRequestBodyException::class.java)
            .hasMessage("Поле 'coordinates.x' должно быть целым числом")
    }

    @Test
    @DisplayName("неизвестная категория отвергается при разборе")
    fun `unknown category is rejected`() {
        assertThatThrownBy { read("""{"category": "PRIEST"}""") }
            .isInstanceOf(MalformedRequestBodyException::class.java)
            .hasMessageContaining("SCOUT, SUPPRESSOR, TACTICAL, HELIX")
    }

    @Test
    @DisplayName("readOnly-поля в теле игнорируются")
    fun `read only fields are ignored`() {
        val patch = read("""{"id": 5, "creationDate": "2026-01-01T00:00:00Z", "name": "Marius"}""")

        assertThat(patch.name?.value).isEqualTo("Marius")
    }
}
