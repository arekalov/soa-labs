package ru.ifmo.soa.spacemarine.adapter.web

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import jakarta.enterprise.context.ApplicationScoped
import ru.ifmo.soa.spacemarine.adapter.web.error.MalformedRequestBodyException
import ru.ifmo.soa.spacemarine.application.Patched
import ru.ifmo.soa.spacemarine.application.SpaceMarinePatch
import ru.ifmo.soa.spacemarine.domain.model.AstartesCategory
import ru.ifmo.soa.spacemarine.domain.model.ChapterDraft
import ru.ifmo.soa.spacemarine.domain.model.CoordinatesDraft

/**
 * Разбор тела PATCH.
 *
 * Тело читается как сырое дерево JSON, а не как DTO: только так различимы три состояния —
 * поля нет, поле задано, поле явно обнулено. При десериализации в DTO первые два слились бы,
 * и `{"achievements": null}` стало бы неотличимо от `{}`.
 *
 * Несоответствие типа — это 400 (тело не отвечает схеме). Предметная недопустимость значения
 * вскроется позже, при проверке домена, и даст 422.
 *
 * Методы названы `readXxx`, а не `asXxx`, намеренно: у `JsonNode` есть собственный
 * `asText(defaultValue)`, и одноимённое расширение им перекрывалось бы — член класса
 * всегда выигрывает у расширения. Вместо значения возвращалось имя поля.
 */
@ApplicationScoped
open class SpaceMarinePatchReader {

    open fun read(body: ObjectNode?): SpaceMarinePatch {
        // Пустое тело — корректный патч, который ничего не меняет.
        val node = body ?: return SpaceMarinePatch()

        return SpaceMarinePatch(
            name = node.patched("name") { readText(it, "name") },
            coordinates = node.patched("coordinates") { readCoordinates(it) },
            health = node.patched("health") { readFloat(it, "health") },
            loyal = node.patched("loyal") { readBoolean(it, "loyal") },
            achievements = node.patched("achievements") { readText(it, "achievements") },
            category = node.patched("category") { readCategory(it, "category") },
            chapter = node.patched("chapter") { readChapter(it) },
            // id и creationDate игнорируются: спецификация помечает их readOnly.
        )
    }

    /** Оборачивает значение, только если поле физически присутствует в теле. */
    private inline fun <T> ObjectNode.patched(field: String, read: (JsonNode) -> T): Patched<T>? =
        if (has(field)) Patched(read(get(field))) else null

    private fun readText(node: JsonNode, field: String): String? = when {
        node.isNull -> null
        node.isTextual -> node.textValue()
        else -> throw MalformedRequestBodyException("Поле '$field' должно быть строкой")
    }

    private fun readFloat(node: JsonNode, field: String): Float? = when {
        node.isNull -> null
        node.isNumber -> node.floatValue()
        else -> throw MalformedRequestBodyException("Поле '$field' должно быть числом")
    }

    private fun readBoolean(node: JsonNode, field: String): Boolean? = when {
        node.isNull -> null
        node.isBoolean -> node.booleanValue()
        else -> throw MalformedRequestBodyException("Поле '$field' должно быть true или false")
    }

    private fun readCategory(node: JsonNode, field: String): AstartesCategory? = when {
        node.isNull -> null
        !node.isTextual -> throw MalformedRequestBodyException("Поле '$field' должно быть строкой")
        else -> AstartesCategory.entries.firstOrNull { it.name == node.textValue() }
            ?: throw MalformedRequestBodyException(
                "Поле '$field' должно быть одним из: " +
                    AstartesCategory.entries.joinToString(", ") { it.name },
            )
    }

    /**
     * Вложенные объекты заменяются целиком, без рекурсивного слияния с текущим состоянием.
     *
     * Иначе пришлось бы решать, что означает частично заданный объект с обязательными полями;
     * замена целиком однозначна и проверяется теми же правилами домена. Решение отражено в отчёте.
     */
    private fun readCoordinates(node: JsonNode): CoordinatesDraft? = when {
        node.isNull -> null
        !node.isObject -> throw MalformedRequestBodyException("Поле 'coordinates' должно быть объектом")
        else -> CoordinatesDraft(
            x = readInt(node.get("x"), "coordinates.x"),
            y = readDouble(node.get("y"), "coordinates.y"),
        )
    }

    private fun readChapter(node: JsonNode): ChapterDraft? = when {
        node.isNull -> null
        !node.isObject -> throw MalformedRequestBodyException("Поле 'chapter' должно быть объектом")
        else -> ChapterDraft(
            name = node.get("name")?.let { readText(it, "chapter.name") },
            parentLegion = node.get("parentLegion")?.let { readText(it, "chapter.parentLegion") },
        )
    }

    private fun readInt(node: JsonNode?, field: String): Int? = when {
        node == null || node.isNull -> null
        node.isIntegralNumber -> node.intValue()
        else -> throw MalformedRequestBodyException("Поле '$field' должно быть целым числом")
    }

    private fun readDouble(node: JsonNode?, field: String): Double? = when {
        node == null || node.isNull -> null
        node.isNumber -> node.doubleValue()
        else -> throw MalformedRequestBodyException("Поле '$field' должно быть числом")
    }
}
