package ru.ifmo.soa.starship.service

import jakarta.ejb.Stateless
import jakarta.ejb.TransactionAttribute
import jakarta.ejb.TransactionAttributeType.SUPPORTS
import jakarta.inject.Inject
import ru.ifmo.soa.starship.Messages
import ru.ifmo.soa.starship.api.StarshipRemote
import ru.ifmo.soa.starship.client.SpaceMarineClient
import ru.ifmo.soa.starship.dto.StarshipDto
import ru.ifmo.soa.starship.dto.StarshipPageDto
import ru.ifmo.soa.starship.exception.InvalidParameterException
import ru.ifmo.soa.starship.exception.SpaceMarineAlreadyOnBoardException
import ru.ifmo.soa.starship.exception.SpaceMarineNotFoundException
import ru.ifmo.soa.starship.exception.SpaceMarineNotOnBoardException
import ru.ifmo.soa.starship.exception.SpaceMarineServiceUnavailableException
import ru.ifmo.soa.starship.exception.StarshipAlreadyExistsException
import ru.ifmo.soa.starship.exception.StarshipNotFoundException
import ru.ifmo.soa.starship.exception.StarshipValidationException
import ru.ifmo.soa.starship.mapper.toDto
import ru.ifmo.soa.starship.model.Starship
import ru.ifmo.soa.starship.query.StarshipQuery
import ru.ifmo.soa.starship.repository.StarshipRepository

/** Операции над кораблями. Транзакциями управляет контейнер. */
@Stateless
class StarshipBean : StarshipRemote {
    // Внедрение в поле, а не в конструктор: контейнер создаёт бин конструктором без
    // аргументов, и параметры конструктора остались бы незаполненными.
    @Inject
    private lateinit var repository: StarshipRepository

    @Inject
    private lateinit var spaceMarines: SpaceMarineClient

    @TransactionAttribute(SUPPORTS)
    override fun list(query: StarshipQuery): StarshipPageDto {
        val page = repository.search(query)
        return page.copy(items = page.items.map(::reconcile)).toDto()
    }

    @TransactionAttribute(SUPPORTS)
    override fun getById(id: Long): StarshipDto {
        requirePositive(id, "id")
        return reconcile(find(id)).toDto()
    }

    override fun create(name: String?): StarshipDto =
        repository.insert(Starship(id = repository.nextId(), name = requireName(name))).toDto()

    override fun createWithId(id: Long, name: String?): StarshipDto {
        requirePositive(id, "id")
        if (name.isNullOrBlank()) throw InvalidParameterException(Messages.paramBlank("name"))
        if (repository.existsById(id)) throw StarshipAlreadyExistsException(id)
        return repository.insert(Starship(id = id, name = name)).toDto()
    }

    override fun rename(id: Long, name: String?): StarshipDto {
        requirePositive(id, "id")
        val starship = find(id)
        starship.name = requireName(name)
        return repository.save(starship).toDto()
    }

    override fun delete(id: Long) {
        requirePositive(id, "id")
        if (!repository.deleteById(id)) throw StarshipNotFoundException(id)
    }

    override fun board(starshipId: Long, spaceMarineId: Int): StarshipDto {
        requirePositive(starshipId, "starship-id")
        requirePositive(spaceMarineId.toLong(), "space-marine-id")
        val starship = find(starshipId)

        // Десантник — один человек: быть на двух кораблях сразу он не может.
        repository.findFirstByMarine(spaceMarineId)?.let { occupied ->
            throw SpaceMarineAlreadyOnBoardException(requireNotNull(occupied.id), spaceMarineId)
        }
        if (!spaceMarines.exists(spaceMarineId)) throw SpaceMarineNotFoundException(spaceMarineId)

        starship.marines += spaceMarineId
        return repository.save(starship).toDto()
    }

    override fun unload(starshipId: Long, spaceMarineId: Int): StarshipDto {
        requirePositive(starshipId, "starship-id")
        requirePositive(spaceMarineId.toLong(), "space-marine-id")
        val starship = find(starshipId)

        // Локальная проверка до сетевого вызова: если десантника нет на борту,
        // беспокоить первый сервис незачем.
        if (!starship.hasOnBoard(spaceMarineId)) {
            throw SpaceMarineNotOnBoardException(starshipId, spaceMarineId)
        }
        if (!spaceMarines.exists(spaceMarineId)) throw SpaceMarineNotFoundException(spaceMarineId)

        starship.marines -= spaceMarineId
        return repository.save(starship).toDto()
    }

    private fun find(id: Long): Starship = repository.findById(id) ?: throw StarshipNotFoundException(id)

    private fun requirePositive(value: Long, paramName: String) {
        if (value <= 0) throw InvalidParameterException(Messages.paramPositiveInt(paramName))
    }

    private fun requireName(name: String?): String {
        if (name.isNullOrBlank()) throw StarshipValidationException(listOf(Messages.NAME_BLANK))
        return name
    }

    /** Удалённые в первом сервисе десантники снимаются с борта; при его недоступности экипаж отдаётся как сохранён. */
    private fun reconcile(starship: Starship): Starship {
        val alive = try {
            starship.marines.filterTo(mutableSetOf()) { spaceMarines.exists(it) }
        } catch (_: SpaceMarineServiceUnavailableException) {
            return starship
        }
        if (alive.size == starship.marines.size) return starship

        starship.marines = alive
        return repository.save(starship)
    }
}
