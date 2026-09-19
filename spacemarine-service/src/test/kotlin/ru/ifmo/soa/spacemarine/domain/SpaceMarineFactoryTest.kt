package ru.ifmo.soa.spacemarine.domain

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import ru.ifmo.soa.spacemarine.domain.model.AstartesCategory
import ru.ifmo.soa.spacemarine.domain.model.ChapterDraft
import ru.ifmo.soa.spacemarine.domain.model.CoordinatesDraft
import ru.ifmo.soa.spacemarine.domain.model.SpaceMarineDraft
import ru.ifmo.soa.spacemarine.domain.model.SpaceMarineFactory
import ru.ifmo.soa.spacemarine.domain.validation.DomainValidationException
import java.time.Instant
import java.time.temporal.ChronoUnit

class SpaceMarineFactoryTest {

    private fun validDraft(
        name: String? = "Brother Marius",
        coordinates: CoordinatesDraft? = CoordinatesDraft(10, 3.5),
        health: Float? = 85.5f,
        loyal: Boolean? = true,
        achievements: String? = "Hero of Macragge",
        category: AstartesCategory? = AstartesCategory.TACTICAL,
        chapter: ChapterDraft? = ChapterDraft("Ultramarines", "XIII Legion"),
    ) = SpaceMarineDraft(name, coordinates, health, loyal, achievements, category, chapter)

    @Test
    @DisplayName("корректные данные дают валидный агрегат")
    fun `creates from valid draft`() {
        val marine = SpaceMarineFactory.create(validDraft())

        assertThat(marine.id).isNull()
        assertThat(marine.name).isEqualTo("Brother Marius")
        assertThat(marine.coordinates.y).isEqualTo(3.5)
        assertThat(marine.category).isEqualTo(AstartesCategory.TACTICAL)
        assertThat(marine.chapter?.parentLegion).isEqualTo("XIII Legion")
    }

    @Test
    @DisplayName("нарушения собираются все сразу и совпадают с примерами спецификации")
    fun `reports every violation at once in spec wording`() {
        val draft = validDraft(health = -1f, coordinates = CoordinatesDraft(10, 13.0))

        assertThatThrownBy { SpaceMarineFactory.create(draft) }
            .isInstanceOf(DomainValidationException::class.java)
            .satisfies({ thrown ->
                val details = (thrown as DomainValidationException).details
                // Ровно те строки, что приведены в примере ответа 422 в спецификации
                assertThat(details).containsExactlyInAnyOrder(
                    "coordinates.y: максимальное значение поля — 12",
                    "health: значение должно быть больше 0",
                )
            })
    }

    @Test
    @DisplayName("граница coordinates.y = 12 допустима, 12.0001 — уже нет")
    fun `coordinate y boundary is inclusive`() {
        assertThat(SpaceMarineFactory.create(validDraft(coordinates = CoordinatesDraft(1, 12.0))).coordinates.y)
            .isEqualTo(12.0)

        assertThatThrownBy { SpaceMarineFactory.create(validDraft(coordinates = CoordinatesDraft(1, 12.0001))) }
            .isInstanceOf(DomainValidationException::class.java)
    }

    @Test
    @DisplayName("health = 0 отвергается: требуется строго больше нуля")
    fun `health must be strictly positive`() {
        assertThatThrownBy { SpaceMarineFactory.create(validDraft(health = 0f)) }
            .isInstanceOf(DomainValidationException::class.java)
            .satisfies({ thrown ->
                assertThat((thrown as DomainValidationException).details)
                    .containsExactly("health: значение должно быть больше 0")
            })
    }

    @Test
    @DisplayName("NaN не проходит ни в health, ни в координате")
    fun `rejects NaN`() {
        assertThatThrownBy { SpaceMarineFactory.create(validDraft(health = Float.NaN)) }
            .isInstanceOf(DomainValidationException::class.java)

        assertThatThrownBy { SpaceMarineFactory.create(validDraft(coordinates = CoordinatesDraft(1, Double.NaN))) }
            .isInstanceOf(DomainValidationException::class.java)
    }

    @Test
    @DisplayName("отсутствие обязательного поля — нарушение целостности, а не ошибка разбора")
    fun `missing required fields are violations`() {
        val empty = SpaceMarineDraft(null, null, null, null, null, null, null)

        assertThatThrownBy { SpaceMarineFactory.create(empty) }
            .isInstanceOf(DomainValidationException::class.java)
            .satisfies({ thrown ->
                assertThat((thrown as DomainValidationException).details).containsExactlyInAnyOrder(
                    "name: поле не может быть null",
                    "coordinates: поле не может быть null",
                    "health: поле не может быть null",
                    "loyal: поле не может быть null",
                    "category: поле не может быть null",
                )
            })
    }

    @Test
    @DisplayName("пустое имя отвергается, пустой achievements — нет")
    fun `blank name rejected but achievements may be anything`() {
        assertThatThrownBy { SpaceMarineFactory.create(validDraft(name = "   ")) }
            .isInstanceOf(DomainValidationException::class.java)
            .satisfies({ thrown ->
                assertThat((thrown as DomainValidationException).details)
                    .containsExactly("name: строка не может быть пустой")
            })

        assertThat(SpaceMarineFactory.create(validDraft(achievements = null)).achievements).isNull()
    }

    @Test
    @DisplayName("орден необязателен целиком, но если задан — обязан иметь непустое имя")
    fun `chapter is optional but must be well formed when present`() {
        assertThat(SpaceMarineFactory.create(validDraft(chapter = null)).chapter).isNull()

        assertThatThrownBy {
            SpaceMarineFactory.create(validDraft(chapter = ChapterDraft(null, "XIII Legion")))
        }.satisfies({ thrown ->
            assertThat((thrown as DomainValidationException).details)
                .containsExactly("chapter.name: поле не может быть null")
        })
    }

    @Test
    @DisplayName("дата создания срезается до миллисекунд — иначе фильтр по равенству не совпадёт")
    fun `creation date truncated to millis`() {
        val precise = Instant.parse("2026-09-10T12:00:00Z").plusNanos(123_456_789)

        val marine = SpaceMarineFactory.create(validDraft(), creationDate = precise)

        assertThat(marine.creationDate).isEqualTo(precise.truncatedTo(ChronoUnit.MILLIS))
        assertThat(marine.creationDate.nano % 1_000_000).isZero()
    }

    @Test
    @DisplayName("обновление сохраняет id и дату создания: оба поля readOnly")
    fun `update preserves readonly fields`() {
        val original = SpaceMarineFactory
            .create(validDraft(), creationDate = Instant.parse("2026-09-10T12:00:00Z"))
            .copy(id = 42)

        val updated = SpaceMarineFactory.update(original, validDraft(name = "Brother Cassius"))

        assertThat(updated.id).isEqualTo(42)
        assertThat(updated.creationDate).isEqualTo(original.creationDate)
        assertThat(updated.name).isEqualTo("Brother Cassius")
    }
}
