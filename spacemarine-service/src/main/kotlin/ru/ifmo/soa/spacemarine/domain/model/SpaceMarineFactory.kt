package ru.ifmo.soa.spacemarine.domain.model

import ru.ifmo.soa.spacemarine.domain.validation.ViolationCollector
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Единственная точка создания валидного [SpaceMarine].
 *
 * Здесь собраны все ограничения целостности из условия задачи. Держать их в домене, а не в
 * аннотациях DTO, — осознанное решение: правила принадлежат предметной области, а веб-слой
 * лишь переводит доменные нарушения в HTTP 422.
 */
object SpaceMarineFactory {

    /** Верхняя граница `coordinates.y`, включительно. */
    const val MAX_COORDINATE_Y: Double = 12.0

    /**
     * Точность хранения даты создания.
     *
     * Срез до миллисекунд обязателен: Postgres хранит `timestamp` с микросекундной точностью,
     * и без выравнивания фильтр по точному равенству `creationDate` не совпал бы никогда —
     * клиент оперирует миллисекундами из ISO-8601.
     */
    val TIME_PRECISION: ChronoUnit = ChronoUnit.MILLIS

    fun now(): Instant = Instant.now().truncatedTo(TIME_PRECISION)

    /**
     * Создаёт нового десантника. Идентификатор присвоит хранилище, дата создания — серверная.
     *
     * @throws ru.ifmo.soa.spacemarine.domain.validation.DomainValidationException
     *         если нарушен хотя бы один инвариант; сообщаются **все** нарушения сразу.
     */
    fun create(draft: SpaceMarineDraft, creationDate: Instant = now()): SpaceMarine =
        assemble(draft, id = null, creationDate = creationDate.truncatedTo(TIME_PRECISION))

    /**
     * Применяет новое состояние к существующему десантнику.
     *
     * `id` и `creationDate` переносятся из [existing] и не могут быть изменены клиентом —
     * спецификация помечает оба поля как `readOnly`.
     */
    fun update(existing: SpaceMarine, draft: SpaceMarineDraft): SpaceMarine =
        assemble(draft, id = existing.id, creationDate = existing.creationDate)

    private fun assemble(draft: SpaceMarineDraft, id: Int?, creationDate: Instant): SpaceMarine {
        val violations = ViolationCollector()

        val name = draft.name
        if (name == null) {
            violations.add("name", "поле не может быть null")
        } else if (name.isBlank()) {
            violations.add("name", "строка не может быть пустой")
        }

        val coordinates = validateCoordinates(draft.coordinates, violations)

        val health = draft.health
        if (health == null) {
            violations.add("health", "поле не может быть null")
        } else if (!(health > 0f)) {
            // Условие записано через отрицание намеренно: оно отсекает и NaN, который иначе
            // прошёл бы проверку `health <= 0` и попал бы в базу.
            violations.add("health", "значение должно быть больше 0")
        }

        val loyal = draft.loyal
        if (loyal == null) violations.add("loyal", "поле не может быть null")

        val category = draft.category
        if (category == null) violations.add("category", "поле не может быть null")

        val chapter = validateChapter(draft.chapter, violations)

        violations.failIfAny()

        return SpaceMarine(
            id = id,
            name = name!!,
            coordinates = coordinates!!,
            creationDate = creationDate,
            health = health!!,
            loyal = loyal!!,
            achievements = draft.achievements,
            category = category!!,
            chapter = chapter,
        )
    }

    private fun validateCoordinates(draft: CoordinatesDraft?, violations: ViolationCollector): Coordinates? {
        if (draft == null) {
            violations.add("coordinates", "поле не может быть null")
            return null
        }

        val x = draft.x
        if (x == null) violations.add("coordinates.x", "поле не может быть null")

        val y = draft.y
        when {
            y == null -> violations.add("coordinates.y", "поле не может быть null")
            y.isNaN() -> violations.add("coordinates.y", "значение должно быть числом")
            y > MAX_COORDINATE_Y -> violations.add("coordinates.y", "максимальное значение поля — 12")
        }

        return if (x != null && y != null && !y.isNaN() && y <= MAX_COORDINATE_Y) Coordinates(x, y) else null
    }

    private fun validateChapter(draft: ChapterDraft?, violations: ViolationCollector): Chapter? {
        // Орден целиком необязателен — спецификация помечает его nullable.
        if (draft == null) return null

        val name = draft.name
        if (name == null) {
            violations.add("chapter.name", "поле не может быть null")
        } else if (name.isBlank()) {
            violations.add("chapter.name", "строка не может быть пустой")
        }

        return if (name != null && name.isNotBlank()) Chapter(name, draft.parentLegion) else null
    }
}
