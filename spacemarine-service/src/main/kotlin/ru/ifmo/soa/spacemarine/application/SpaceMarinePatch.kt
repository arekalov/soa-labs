package ru.ifmo.soa.spacemarine.application

import ru.ifmo.soa.spacemarine.domain.model.AstartesCategory
import ru.ifmo.soa.spacemarine.domain.model.ChapterDraft
import ru.ifmo.soa.spacemarine.domain.model.CoordinatesDraft
import ru.ifmo.soa.spacemarine.domain.model.SpaceMarine
import ru.ifmo.soa.spacemarine.domain.model.SpaceMarineDraft

/**
 * Обёртка «поле присутствовало в запросе».
 *
 * Нужна, чтобы различить три состояния PATCH-тела, которые иначе сливаются в одно:
 * поля нет (`null` вместо обёртки), поле задано значением, поле явно обнулено
 * (`Patched(null)`). Без этого `{"achievements": null}` не отличить от `{}`.
 */
@JvmInline
value class Patched<out T>(val value: T)

/**
 * Частичное обновление десантника.
 *
 * `id` и `creationDate` отсутствуют намеренно: спецификация помечает их `readOnly`,
 * клиент изменить их не может.
 */
data class SpaceMarinePatch(
    val name: Patched<String?>? = null,
    val coordinates: Patched<CoordinatesDraft?>? = null,
    val health: Patched<Float?>? = null,
    val loyal: Patched<Boolean?>? = null,
    val achievements: Patched<String?>? = null,
    val category: Patched<AstartesCategory?>? = null,
    val chapter: Patched<ChapterDraft?>? = null,
) {
    /**
     * Накладывает патч на текущее состояние и возвращает полный черновик.
     *
     * Дальше черновик проходит ту же проверку, что POST и PUT, — поэтому правила валидации
     * и формат `details` у всех трёх операций совпадают.
     *
     * Вложенные объекты (`coordinates`, `chapter`) заменяются **целиком**, без рекурсивного
     * слияния: у обоих есть обязательные поля, поэтому частично заданный объект был бы
     * заведомо невалиден. Решение зафиксировано в отчёте — спецификация его не оговаривает.
     */
    fun applyTo(current: SpaceMarine): SpaceMarineDraft = SpaceMarineDraft(
        name = pick(name, current.name),
        coordinates = pick(coordinates, current.coordinates.toDraft()),
        health = pick(health, current.health),
        loyal = pick(loyal, current.loyal),
        achievements = pick(achievements, current.achievements),
        category = pick(category, current.category),
        chapter = pick(chapter, current.chapter?.toDraft()),
    )

    private companion object {
        /**
         * Проверка именно на наличие обёртки, а не элвис-оператором: `patched?.value ?: fallback`
         * молча потерял бы явно переданный `null` и откатил поле к прежнему значению.
         */
        fun <T> pick(patched: Patched<T>?, fallback: T): T =
            if (patched != null) patched.value else fallback
    }
}

private fun ru.ifmo.soa.spacemarine.domain.model.Coordinates.toDraft() = CoordinatesDraft(x, y)

private fun ru.ifmo.soa.spacemarine.domain.model.Chapter.toDraft() = ChapterDraft(name, parentLegion)
