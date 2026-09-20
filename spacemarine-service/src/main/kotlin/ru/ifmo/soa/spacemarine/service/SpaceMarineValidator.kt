package ru.ifmo.soa.spacemarine.service

import ru.ifmo.soa.spacemarine.Messages
import ru.ifmo.soa.spacemarine.dto.ChapterDto
import ru.ifmo.soa.spacemarine.dto.CoordinatesDto
import ru.ifmo.soa.spacemarine.dto.SpaceMarineInputDto
import ru.ifmo.soa.spacemarine.exception.ValidationException
import ru.ifmo.soa.spacemarine.model.MAX_COORDINATE_Y

/**
 * Ограничения целостности из условия задачи.
 *
 * Проверяются все поля сразу, а не до первого отказа: спецификация требует вернуть
 * массив `details` со всеми нарушениями.
 */
object SpaceMarineValidator {

    fun validate(input: SpaceMarineInputDto) {
        val violations = mutableListOf<String>()

        when {
            input.name == null -> violations += violation("name", Messages.FIELD_NULL)
            input.name.isBlank() -> violations += violation("name", Messages.FIELD_BLANK)
        }

        validateCoordinates(input.coordinates, violations)

        when {
            input.health == null -> violations += violation("health", Messages.FIELD_NULL)
            // Через отрицание намеренно: так отсекается и NaN, который прошёл бы проверку `health <= 0`.
            !(input.health > 0f) -> violations += violation("health", Messages.FIELD_NOT_POSITIVE)
        }

        if (input.loyal == null) violations += violation("loyal", Messages.FIELD_NULL)
        if (input.category == null) violations += violation("category", Messages.FIELD_NULL)

        validateChapter(input.chapter, violations)

        if (violations.isNotEmpty()) throw ValidationException(violations)
    }

    private fun validateCoordinates(coordinates: CoordinatesDto?, violations: MutableList<String>) {
        if (coordinates == null) {
            violations += violation("coordinates", Messages.FIELD_NULL)
            return
        }
        if (coordinates.x == null) violations += violation("coordinates.x", Messages.FIELD_NULL)
        when {
            coordinates.y == null -> violations += violation("coordinates.y", Messages.FIELD_NULL)
            coordinates.y.isNaN() -> violations += violation("coordinates.y", Messages.FIELD_NOT_NUMBER)
            coordinates.y > MAX_COORDINATE_Y -> violations += violation("coordinates.y", Messages.FIELD_Y_TOO_BIG)
        }
    }

    /** Орден целиком необязателен, но если он задан, название обязано быть непустым. */
    private fun validateChapter(chapter: ChapterDto?, violations: MutableList<String>) {
        if (chapter == null) return
        when {
            chapter.name == null -> violations += violation("chapter.name", Messages.FIELD_NULL)
            chapter.name.isBlank() -> violations += violation("chapter.name", Messages.FIELD_BLANK)
        }
    }

    private fun violation(path: String, message: String) = Messages.violation(path, message)
}
