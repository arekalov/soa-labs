package ru.ifmo.soa.spacemarine.adapter.web.dto

import com.fasterxml.jackson.annotation.JsonInclude
import ru.ifmo.soa.spacemarine.domain.model.AstartesCategory
import java.time.Instant

/**
 * Сведения об ошибке. Схема `Error` из спецификации.
 *
 * `details` не сериализуется при отсутствии: в схеме поле необязательное.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ErrorDto(
    val code: Int,
    val message: String,
    val details: List<String>? = null,
)

data class CoordinatesDto(
    val x: Int? = null,
    val y: Double? = null,
)

data class ChapterDto(
    val name: String? = null,
    val parentLegion: String? = null,
)

/**
 * Десантник в ответе. Все поля заполнены: объект приходит из домена, а значит уже валиден.
 *
 * `creationDate` — [Instant], а не `OffsetDateTime`: первый сериализуется как
 * `2026-09-10T12:00:00Z`, ровно как в примере спецификации, тогда как второй дал бы `+00:00`.
 */
data class SpaceMarineDto(
    val id: Int,
    val name: String,
    val coordinates: CoordinatesDto,
    val creationDate: Instant,
    val health: Float,
    val loyal: Boolean,
    val achievements: String?,
    val category: AstartesCategory,
    val chapter: ChapterDto?,
)

/**
 * Тело запроса на создание и полное обновление. Схемы `SpaceMarineInput`.
 *
 * Все поля nullable намеренно. Если объявить их обязательными, Jackson упадёт на отсутствующем
 * поле ещё при разборе тела и вернёт 400, тогда как спецификация требует 422 с перечнем
 * нарушенных ограничений. Проверку выполняет домен, а не десериализатор.
 */
data class SpaceMarineInputDto(
    val name: String? = null,
    val coordinates: CoordinatesDto? = null,
    val health: Float? = null,
    val loyal: Boolean? = null,
    val achievements: String? = null,
    val category: AstartesCategory? = null,
    val chapter: ChapterDto? = null,
)

/** Страница выборки. Схема `SpaceMarinePage`. */
data class SpaceMarinePageDto(
    val items: List<SpaceMarineDto>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)

/** Результат подсчёта. Схема `CountResult`. */
data class CountResultDto(
    val count: Long,
)
