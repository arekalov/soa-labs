package ru.ifmo.soa.spacemarine.application.usecase

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import ru.ifmo.soa.spacemarine.application.SpaceMarinePatch
import ru.ifmo.soa.spacemarine.application.error.SpaceMarineNotFoundException
import ru.ifmo.soa.spacemarine.application.port.SpaceMarineRepository
import ru.ifmo.soa.spacemarine.application.query.Page
import ru.ifmo.soa.spacemarine.application.query.SpaceMarineQuery
import ru.ifmo.soa.spacemarine.domain.model.SpaceMarine
import ru.ifmo.soa.spacemarine.domain.model.SpaceMarineDraft
import ru.ifmo.soa.spacemarine.domain.model.SpaceMarineFactory

/**
 * Девять сценариев — ровно по числу операций в спецификации.
 *
 * Слой оперирует доменными типами и портами; ни JPA, ни HTTP здесь не упоминаются.
 * Аннотации CDI допустимы: это стандарт внедрения зависимостей, а не деталь фреймворка,
 * и направление зависимостей они не нарушают.
 */

@ApplicationScoped
open class CreateSpaceMarine @Inject constructor(
    private val repository: SpaceMarineRepository,
) {
    /** Идентификатор и дата создания присваиваются сервером, значения клиента игнорируются. */
    @Transactional
    open fun execute(draft: SpaceMarineDraft): SpaceMarine =
        repository.create(SpaceMarineFactory.create(draft))
}

@ApplicationScoped
open class GetSpaceMarine @Inject constructor(
    private val repository: SpaceMarineRepository,
) {
    @Transactional(Transactional.TxType.SUPPORTS)
    open fun execute(id: Int): SpaceMarine =
        repository.findById(id) ?: throw SpaceMarineNotFoundException(id)
}

@ApplicationScoped
open class UpdateSpaceMarine @Inject constructor(
    private val repository: SpaceMarineRepository,
) {
    @Transactional
    open fun execute(id: Int, draft: SpaceMarineDraft): SpaceMarine {
        val existing = repository.findById(id) ?: throw SpaceMarineNotFoundException(id)
        return repository.update(SpaceMarineFactory.update(existing, draft))
    }
}

@ApplicationScoped
open class PatchSpaceMarine @Inject constructor(
    private val repository: SpaceMarineRepository,
) {
    /**
     * Патч разворачивается в полный черновик и проходит ту же проверку, что POST и PUT.
     * Благодаря этому правила валидации и формат `details` одинаковы у всех трёх операций.
     */
    @Transactional
    open fun execute(id: Int, patch: SpaceMarinePatch): SpaceMarine {
        val existing = repository.findById(id) ?: throw SpaceMarineNotFoundException(id)
        return repository.update(SpaceMarineFactory.update(existing, patch.applyTo(existing)))
    }
}

@ApplicationScoped
open class DeleteSpaceMarine @Inject constructor(
    private val repository: SpaceMarineRepository,
) {
    @Transactional
    open fun execute(id: Int) {
        if (!repository.deleteById(id)) throw SpaceMarineNotFoundException(id)
    }
}

@ApplicationScoped
open class SearchSpaceMarines @Inject constructor(
    private val repository: SpaceMarineRepository,
) {
    /** Пустая выборка — это корректный результат, а не ошибка: вернётся 200 с пустым `items`. */
    @Transactional(Transactional.TxType.SUPPORTS)
    open fun execute(query: SpaceMarineQuery): Page<SpaceMarine> = repository.search(query)
}

@ApplicationScoped
open class CountSpaceMarinesByChapter @Inject constructor(
    private val repository: SpaceMarineRepository,
) {
    @Transactional(Transactional.TxType.SUPPORTS)
    open fun execute(name: String, parentLegion: String?): Long =
        repository.countByChapter(name, parentLegion)
}

@ApplicationScoped
open class CountSpaceMarinesByHealthGreaterThan @Inject constructor(
    private val repository: SpaceMarineRepository,
) {
    @Transactional(Transactional.TxType.SUPPORTS)
    open fun execute(threshold: Float): Long = repository.countByHealthGreaterThan(threshold)
}

@ApplicationScoped
open class FindSpaceMarinesByNamePrefix @Inject constructor(
    private val repository: SpaceMarineRepository,
) {
    @Transactional(Transactional.TxType.SUPPORTS)
    open fun execute(prefix: String): List<SpaceMarine> = repository.findByNamePrefix(prefix)
}
