package ru.ifmo.soa.spacemarine.dto

import ru.ifmo.soa.spacemarine.model.AstartesCategory

/**
 * Тело создания, полной замены и результат наложения PATCH. Схема `SpaceMarineInput`.
 *
 * Поля nullable намеренно. Объяви их обязательными — Jackson упал бы на отсутствующем поле
 * и вернул 400, тогда как спецификация требует 422 с перечнем нарушений.
 *
 * Поля изменяемые, потому что на этот объект накладывается тело PATCH: обновляется
 * существующий экземпляр, и меняются только те поля, что физически есть в JSON.
 */
class SpaceMarineInputDto(
    var name: String? = null,
    var coordinates: CoordinatesDto? = null,
    var health: Float? = null,
    var loyal: Boolean? = null,
    var achievements: String? = null,
    var category: AstartesCategory? = null,
    var chapter: ChapterDto? = null,
)
