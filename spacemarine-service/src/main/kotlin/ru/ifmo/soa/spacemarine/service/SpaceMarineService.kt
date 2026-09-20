package ru.ifmo.soa.spacemarine.service

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import ru.ifmo.soa.spacemarine.dto.SpaceMarineInputDto
import ru.ifmo.soa.spacemarine.exception.SpaceMarineNotFoundException
import ru.ifmo.soa.spacemarine.mapper.applyFrom
import ru.ifmo.soa.spacemarine.mapper.toEntity
import ru.ifmo.soa.spacemarine.mapper.toInput
import ru.ifmo.soa.spacemarine.model.SpaceMarine
import ru.ifmo.soa.spacemarine.model.nowTruncated
import ru.ifmo.soa.spacemarine.query.Page
import ru.ifmo.soa.spacemarine.query.SpaceMarineQuery
import ru.ifmo.soa.spacemarine.repository.SpaceMarineRepository

/** Операции над коллекцией десантников: проверки, границы транзакций, работа с хранилищем. */
@ApplicationScoped
open class SpaceMarineService @Inject constructor(
    private val repository: SpaceMarineRepository,
) {
    @Transactional(Transactional.TxType.SUPPORTS)
    open fun search(query: SpaceMarineQuery): Page<SpaceMarine> = repository.search(query)

    @Transactional(Transactional.TxType.SUPPORTS)
    open fun getById(id: Int): SpaceMarine =
        repository.findById(id) ?: throw SpaceMarineNotFoundException(id)

    @Transactional
    open fun create(input: SpaceMarineInputDto): SpaceMarine {
        SpaceMarineValidator.validate(input)
        return repository.save(input.toEntity(nowTruncated()))
    }

    @Transactional
    open fun replace(id: Int, input: SpaceMarineInputDto): SpaceMarine {
        val marine = getById(id)
        SpaceMarineValidator.validate(input)
        marine.applyFrom(input)
        return repository.save(marine)
    }

    @Transactional
    open fun patch(id: Int, merge: (SpaceMarineInputDto) -> SpaceMarineInputDto): SpaceMarine {
        val marine = getById(id)
        val input = merge(marine.toInput())
        SpaceMarineValidator.validate(input)
        marine.applyFrom(input)
        return repository.save(marine)
    }

    @Transactional
    open fun delete(id: Int) {
        if (!repository.deleteById(id)) throw SpaceMarineNotFoundException(id)
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    open fun countByChapter(name: String, parentLegion: String?): Long =
        repository.countByChapter(name, parentLegion)

    @Transactional(Transactional.TxType.SUPPORTS)
    open fun countByHealthGreaterThan(threshold: Float): Long =
        repository.countByHealthGreaterThan(threshold)

    @Transactional(Transactional.TxType.SUPPORTS)
    open fun findByNamePrefix(prefix: String, page: Int, size: Int): Page<SpaceMarine> =
        repository.findByNamePrefix(prefix, page, size)
}
