package ru.ifmo.soa.starship.service

import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.Page
import org.springframework.stereotype.Service
import ru.ifmo.soa.starship.Messages
import ru.ifmo.soa.starship.client.SpaceMarineClient
import ru.ifmo.soa.starship.exception.InvalidParameterException
import ru.ifmo.soa.starship.exception.SpaceMarineAlreadyOnBoardException
import ru.ifmo.soa.starship.exception.SpaceMarineNotFoundException
import ru.ifmo.soa.starship.exception.SpaceMarineNotOnBoardException
import ru.ifmo.soa.starship.exception.SpaceMarineServiceUnavailableException
import ru.ifmo.soa.starship.exception.StarshipAlreadyExistsException
import ru.ifmo.soa.starship.exception.StarshipNotFoundException
import ru.ifmo.soa.starship.exception.StarshipValidationException
import ru.ifmo.soa.starship.model.Starship
import ru.ifmo.soa.starship.query.StarshipQuery
import ru.ifmo.soa.starship.repository.StarshipRepository
import ru.ifmo.soa.starship.repository.StarshipSpecifications

private fun requirePositive(value: Long, paramName: String) {
    if (value <= 0) throw InvalidParameterException(Messages.paramPositiveInt(paramName))
}

private fun requireName(name: String?): String {
    if (name.isNullOrBlank()) throw StarshipValidationException(listOf(Messages.NAME_BLANK))
    return name
}

/**
 * Операции над кораблями.
 *
 * Общей транзакции на метод намеренно нет: каждый вызов репозитория выполняется в своей,
 * поэтому обращение к первому сервису заведомо оказывается вне транзакции БД и не держит
 * соединение из общего на курс пула.
 */
@Service
class StarshipService(
    private val repository: StarshipRepository,
    private val spaceMarines: SpaceMarineClient,
) {

    fun list(query: StarshipQuery): Page<Starship> =
        repository.findAll(StarshipSpecifications.of(query.filter), query.pageable).map(::reconcile)

    fun getById(id: Long): Starship {
        requirePositive(id, "id")
        return reconcile(repository.findById(id).orElseThrow { StarshipNotFoundException(id) })
    }

    /** Операция из спецификации ЛР1: идентификатор задаёт клиент. */
    fun createWithId(id: Long, name: String?): Starship {
        requirePositive(id, "id")
        if (name.isNullOrBlank()) throw InvalidParameterException(Messages.paramBlank("name"))

        // Проверка до записи даёт понятный 409 без исключения на обычном пути;
        // гонку добирает нарушение первичного ключа при вставке.
        if (repository.existsById(id)) throw StarshipAlreadyExistsException(id)

        return try {
            repository.insert(Starship(id = id, name = name))
        } catch (_: DataIntegrityViolationException) {
            throw StarshipAlreadyExistsException(id)
        }
    }

    /** Базовое создание: номер выдаёт последовательность. */
    fun create(name: String?): Starship =
        repository.insert(Starship(id = repository.nextId(), name = requireName(name)))

    fun rename(id: Long, name: String?): Starship {
        val starship = getById(id)
        starship.name = requireName(name)
        return repository.save(starship)
    }

    fun delete(id: Long) {
        requirePositive(id, "id")
        if (!repository.existsById(id)) throw StarshipNotFoundException(id)
        repository.deleteById(id)
    }

    fun board(starshipId: Long, spaceMarineId: Int): Starship {
        requirePositive(starshipId, "starship-id")
        requirePositive(spaceMarineId.toLong(), "space-marine-id")

        val starship = repository.findById(starshipId).orElseThrow { StarshipNotFoundException(starshipId) }

        // Десантник — один человек: быть на двух кораблях сразу он не может.
        repository.findFirstByMarinesContains(spaceMarineId)?.let { occupied ->
            throw SpaceMarineAlreadyOnBoardException(requireNotNull(occupied.id), spaceMarineId)
        }
        // Иначе на борту оказался бы фантом, которого в первом сервисе нет.
        if (!spaceMarines.exists(spaceMarineId)) throw SpaceMarineNotFoundException(spaceMarineId)

        starship.marines += spaceMarineId
        return repository.save(starship)
    }

    fun unload(starshipId: Long, spaceMarineId: Int): Starship {
        requirePositive(starshipId, "starship-id")
        requirePositive(spaceMarineId.toLong(), "space-marine-id")

        val starship = repository.findById(starshipId).orElseThrow { StarshipNotFoundException(starshipId) }

        // Локальная проверка идёт до сетевого вызова: если десантника нет на борту,
        // беспокоить первый сервис незачем.
        if (!starship.hasOnBoard(spaceMarineId)) {
            throw SpaceMarineNotOnBoardException(starshipId, spaceMarineId)
        }
        // Спецификация предписывает проверить существование десантника в первом сервисе.
        if (!spaceMarines.exists(spaceMarineId)) throw SpaceMarineNotFoundException(spaceMarineId)

        starship.marines -= spaceMarineId
        return repository.save(starship)
    }

    /**
     * Сверка экипажа с первым сервисом.
     *
     * Внешнего ключа между сервисами нет, и об удалении десантника нас никто не известит.
     * Поэтому при чтении состав проверяется, исчезнувшие снимаются с борта, результат
     * сохраняется. Если первый сервис недоступен, экипаж отдаётся как есть: чтение
     * не должно ломаться из-за чужого простоя.
     */
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
