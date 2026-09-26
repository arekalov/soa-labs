package ru.ifmo.soa.spacemarine.api

import jakarta.ejb.Remote
import ru.ifmo.soa.spacemarine.dto.CountResultDto
import ru.ifmo.soa.spacemarine.dto.SpaceMarineDto
import ru.ifmo.soa.spacemarine.dto.SpaceMarineInputDto
import ru.ifmo.soa.spacemarine.dto.SpaceMarinePageDto
import ru.ifmo.soa.spacemarine.query.SpaceMarineQuery

/**
 * Бизнес-интерфейс сервиса десантников.
 *
 * Тело PATCH передаётся строкой, а не лямбдой слияния: через Remote едут только
 * значения, а накладывать патч нужно внутри транзакции, где прочитано текущее состояние.
 */
@Remote
interface SpaceMarineRemote {
    fun search(query: SpaceMarineQuery): SpaceMarinePageDto
    fun getById(id: Int): SpaceMarineDto
    fun create(input: SpaceMarineInputDto): SpaceMarineDto
    fun replace(id: Int, input: SpaceMarineInputDto): SpaceMarineDto
    fun patch(id: Int, patchJson: String?): SpaceMarineDto
    fun delete(id: Int)
    fun countByChapter(name: String, parentLegion: String?): CountResultDto
    fun countByHealthGreaterThan(threshold: Float): CountResultDto
    fun findByNamePrefix(prefix: String, page: Int, size: Int): SpaceMarinePageDto
}
