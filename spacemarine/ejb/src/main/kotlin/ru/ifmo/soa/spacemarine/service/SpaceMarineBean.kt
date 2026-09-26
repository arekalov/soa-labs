package ru.ifmo.soa.spacemarine.service

import com.fasterxml.jackson.core.JsonProcessingException
import jakarta.ejb.Stateless
import jakarta.ejb.TransactionAttribute
import jakarta.ejb.TransactionAttributeType.SUPPORTS
import jakarta.inject.Inject
import ru.ifmo.soa.spacemarine.Messages
import ru.ifmo.soa.spacemarine.api.SpaceMarineRemote
import ru.ifmo.soa.spacemarine.api.jsonMapper
import ru.ifmo.soa.spacemarine.api.nowTruncated
import ru.ifmo.soa.spacemarine.dto.CountResultDto
import ru.ifmo.soa.spacemarine.dto.SpaceMarineDto
import ru.ifmo.soa.spacemarine.dto.SpaceMarineInputDto
import ru.ifmo.soa.spacemarine.dto.SpaceMarinePageDto
import ru.ifmo.soa.spacemarine.exception.InvalidParameterException
import ru.ifmo.soa.spacemarine.exception.SpaceMarineNotFoundException
import ru.ifmo.soa.spacemarine.mapper.applyFrom
import ru.ifmo.soa.spacemarine.mapper.toDto
import ru.ifmo.soa.spacemarine.mapper.toEntity
import ru.ifmo.soa.spacemarine.mapper.toInput
import ru.ifmo.soa.spacemarine.model.SpaceMarine
import ru.ifmo.soa.spacemarine.query.SpaceMarineQuery
import ru.ifmo.soa.spacemarine.repository.SpaceMarineRepository

/** Бизнес-компонент: правила, транзакции и работа с хранилищем. Транзакциями управляет контейнер. */
@Stateless
class SpaceMarineBean : SpaceMarineRemote {
    // Внедрение в поле, а не в конструктор: контейнер создаёт бин конструктором без
    // аргументов, и параметры конструктора остались бы незаполненными.
    @Inject
    private lateinit var repository: SpaceMarineRepository

    @TransactionAttribute(SUPPORTS)
    override fun search(query: SpaceMarineQuery): SpaceMarinePageDto = repository.search(query).toDto()

    @TransactionAttribute(SUPPORTS)
    override fun getById(id: Int): SpaceMarineDto = find(id).toDto()

    override fun create(input: SpaceMarineInputDto): SpaceMarineDto {
        SpaceMarineValidator.validate(input)
        return repository.save(input.toEntity(nowTruncated())).toDto()
    }

    override fun replace(id: Int, input: SpaceMarineInputDto): SpaceMarineDto = write(id) { input }

    // readerForUpdating трогает только поля, физически присутствующие в JSON: благодаря этому
    // «поля нет» и «поле равно null» различимы.
    override fun patch(id: Int, patchJson: String?): SpaceMarineDto = write(id) { current ->
        if (patchJson == null) {
            current
        } else {
            try {
                jsonMapper.readerForUpdating(current).readValue(patchJson)
            } catch (_: JsonProcessingException) {
                throw InvalidParameterException(Messages.MALFORMED_JSON)
            }
        }
    }

    override fun delete(id: Int) {
        if (!repository.deleteById(id)) throw SpaceMarineNotFoundException(id)
    }

    @TransactionAttribute(SUPPORTS)
    override fun countByChapter(name: String, parentLegion: String?) =
        CountResultDto(repository.countByChapter(name, parentLegion))

    @TransactionAttribute(SUPPORTS)
    override fun countByHealthGreaterThan(threshold: Float) =
        CountResultDto(repository.countByHealthGreaterThan(threshold))

    @TransactionAttribute(SUPPORTS)
    override fun findByNamePrefix(prefix: String, page: Int, size: Int): SpaceMarinePageDto =
        repository.findByNamePrefix(prefix, page, size).toDto()

    private fun find(id: Int): SpaceMarine =
        repository.findById(id) ?: throw SpaceMarineNotFoundException(id)

    private fun write(id: Int, merge: (SpaceMarineInputDto) -> SpaceMarineInputDto): SpaceMarineDto {
        val marine = find(id)
        val input = merge(marine.toInput())
        SpaceMarineValidator.validate(input)
        marine.applyFrom(input)
        return repository.save(marine).toDto()
    }
}
