package ru.ifmo.soa.spacemarine.controller

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import jakarta.enterprise.context.ApplicationScoped
import ru.ifmo.soa.spacemarine.Messages
import ru.ifmo.soa.spacemarine.dto.ChapterDto
import ru.ifmo.soa.spacemarine.dto.CoordinatesDto
import ru.ifmo.soa.spacemarine.dto.Patched
import ru.ifmo.soa.spacemarine.dto.SpaceMarinePatchDto
import ru.ifmo.soa.spacemarine.exception.MalformedRequestBodyException
import ru.ifmo.soa.spacemarine.model.AstartesCategory

/**
 * Разбор тела PATCH.
 *
 * Тело читается как сырое дерево JSON, а не как DTO: только так различимы три состояния —
 * поля нет, поле задано, поле явно обнулено. При десериализации в DTO первые два слились бы.
 *
 * Методы названы `readXxx`, а не `asXxx`: у `JsonNode` есть собственный `asText(default)`,
 * и одноимённое расширение им перекрывалось бы — член класса всегда выигрывает у расширения.
 */
@ApplicationScoped
open class SpaceMarinePatchReader {

    open fun read(body: ObjectNode?): SpaceMarinePatchDto {
        // Пустое тело — корректный патч, который ничего не меняет.
        val node = body ?: return SpaceMarinePatchDto()

        return SpaceMarinePatchDto(
            name = node.patched("name") { readText(it, "name") },
            coordinates = node.patched("coordinates") { readCoordinates(it) },
            health = node.patched("health") { readFloat(it, "health") },
            loyal = node.patched("loyal") { readBoolean(it, "loyal") },
            achievements = node.patched("achievements") { readText(it, "achievements") },
            category = node.patched("category") { readCategory(it, "category") },
            chapter = node.patched("chapter") { readChapter(it) },
        )
    }

    private inline fun <T> ObjectNode.patched(field: String, read: (JsonNode) -> T): Patched<T>? =
        if (has(field)) Patched(read(get(field))) else null

    private fun readText(node: JsonNode, field: String): String? = when {
        node.isNull -> null
        node.isTextual -> node.textValue()
        else -> throw MalformedRequestBodyException(Messages.bodyString(field))
    }

    private fun readFloat(node: JsonNode, field: String): Float? = when {
        node.isNull -> null
        node.isNumber -> node.floatValue()
        else -> throw MalformedRequestBodyException(Messages.bodyNumber(field))
    }

    private fun readBoolean(node: JsonNode, field: String): Boolean? = when {
        node.isNull -> null
        node.isBoolean -> node.booleanValue()
        else -> throw MalformedRequestBodyException(Messages.bodyBoolean(field))
    }

    private fun readCategory(node: JsonNode, field: String): AstartesCategory? = when {
        node.isNull -> null
        !node.isTextual -> throw MalformedRequestBodyException(Messages.bodyString(field))
        else -> AstartesCategory.byNameOrNull(node.textValue())
            ?: throw MalformedRequestBodyException(Messages.bodyOneOf(field))
    }

    private fun readCoordinates(node: JsonNode): CoordinatesDto? = when {
        node.isNull -> null
        !node.isObject -> throw MalformedRequestBodyException(Messages.bodyObject("coordinates"))
        else -> CoordinatesDto(
            x = readInt(node.get("x"), "coordinates.x"),
            y = readDouble(node.get("y"), "coordinates.y"),
        )
    }

    private fun readChapter(node: JsonNode): ChapterDto? = when {
        node.isNull -> null
        !node.isObject -> throw MalformedRequestBodyException(Messages.bodyObject("chapter"))
        else -> ChapterDto(
            name = node.get("name")?.let { readText(it, "chapter.name") },
            parentLegion = node.get("parentLegion")?.let { readText(it, "chapter.parentLegion") },
        )
    }

    private fun readInt(node: JsonNode?, field: String): Int? = when {
        node == null || node.isNull -> null
        node.isIntegralNumber -> node.intValue()
        else -> throw MalformedRequestBodyException(Messages.bodyInt(field))
    }

    private fun readDouble(node: JsonNode?, field: String): Double? = when {
        node == null || node.isNull -> null
        node.isNumber -> node.doubleValue()
        else -> throw MalformedRequestBodyException(Messages.bodyNumber(field))
    }
}
