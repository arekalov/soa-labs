package ru.ifmo.soa.spacemarine.domain.model

/**
 * Непроверенные входные данные.
 *
 * Все поля nullable намеренно: отсутствие обязательного поля — это нарушение ограничения
 * целостности класса (HTTP 422 с заполненным `details`), а не синтаксическая ошибка запроса (400).
 * Если бы поля были объявлены обязательными, отсутствующее значение отвалилось бы ещё при
 * десериализации и превратилось бы в 400, что расходится со спецификацией.
 */
data class SpaceMarineDraft(
    val name: String?,
    val coordinates: CoordinatesDraft?,
    val health: Float?,
    val loyal: Boolean?,
    val achievements: String?,
    val category: AstartesCategory?,
    val chapter: ChapterDraft?,
)

data class CoordinatesDraft(
    val x: Int?,
    val y: Double?,
)

data class ChapterDraft(
    val name: String?,
    val parentLegion: String?,
)
