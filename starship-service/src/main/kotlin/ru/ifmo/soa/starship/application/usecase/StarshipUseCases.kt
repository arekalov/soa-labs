package ru.ifmo.soa.starship.application.usecase

import ru.ifmo.soa.starship.application.error.InvalidParameterException
import ru.ifmo.soa.starship.application.error.SpaceMarineAlreadyOnBoardException
import ru.ifmo.soa.starship.application.error.SpaceMarineNotFoundException
import ru.ifmo.soa.starship.application.error.SpaceMarineNotOnBoardException
import ru.ifmo.soa.starship.application.error.StarshipAlreadyExistsException
import ru.ifmo.soa.starship.application.error.StarshipNotFoundException
import ru.ifmo.soa.starship.application.error.StarshipValidationException
import ru.ifmo.soa.starship.application.port.SpaceMarineGateway
import ru.ifmo.soa.starship.application.port.StarshipRepository
import ru.ifmo.soa.starship.application.query.Page
import ru.ifmo.soa.starship.application.query.StarshipQuery
import ru.ifmo.soa.starship.domain.model.Starship

/**
 * Результат высадки. Поля названы в camelCase, как в схеме `UnloadResult`,
 * хотя параметры пути в спецификации записаны через дефис.
 */
data class UnloadResult(
    val starshipId: Long,
    val spaceMarineId: Int,
    val message: String,
)

private fun requirePositive(value: Long, paramName: String) {
    if (value <= 0) throw InvalidParameterException("Параметр '$paramName' должен быть целым числом больше 0")
}

private fun requireName(name: String?): String {
    if (name.isNullOrBlank()) throw StarshipValidationException(listOf("name: строка не может быть пустой"))
    return name
}

/**
 * Сценарии второго сервиса. Классы намеренно без аннотаций Spring: бины объявлены
 * отдельной конфигурацией в адаптере, поэтому прикладной слой остаётся независимым
 * от фреймворка, и архитектурный тест может это проверить.
 */

/** Операция из спецификации ЛР1: идентификатор задаёт клиент. */
class CreateStarship(
    private val repository: StarshipRepository,
) {
    fun execute(id: Long, name: String?): Starship {
        requirePositive(id, "id")
        if (name.isNullOrBlank()) throw InvalidParameterException("Параметр 'name' не может быть пустым")

        // Сначала проверка, потом запись: у сущности с присвоенным идентификатором
        // сохранение уходит в merge и молча перезаписало бы чужой корабль.
        // Гонку добирает адаптер, перехватывая нарушение уникальности.
        if (repository.existsById(id)) throw StarshipAlreadyExistsException(id)

        return repository.create(Starship(id = id, name = name))
    }
}

/** Базовое создание: идентификатор выдаёт хранилище. */
class CreateStarshipWithGeneratedId(
    private val repository: StarshipRepository,
) {
    fun execute(name: String?): Starship =
        repository.create(Starship(id = repository.nextId(), name = requireName(name)))
}

class ListStarships(
    private val repository: StarshipRepository,
) {
    fun execute(query: StarshipQuery): Page<Starship> = repository.list(query)
}

class GetStarship(
    private val repository: StarshipRepository,
) {
    fun execute(id: Long): Starship {
        requirePositive(id, "id")
        return repository.findById(id) ?: throw StarshipNotFoundException(id)
    }
}

class RenameStarship(
    private val repository: StarshipRepository,
) {
    fun execute(id: Long, name: String?): Starship {
        requirePositive(id, "id")
        val starship = repository.findById(id) ?: throw StarshipNotFoundException(id)
        return repository.save(starship.rename(requireName(name)))
    }
}

class DeleteStarship(
    private val repository: StarshipRepository,
) {
    fun execute(id: Long) {
        requirePositive(id, "id")
        if (!repository.deleteById(id)) throw StarshipNotFoundException(id)
    }
}

/** Посадка десантника: спецификацией ЛР1 не предусмотрена, добавлена как базовая операция. */
class BoardSpaceMarine(
    private val repository: StarshipRepository,
    private val spaceMarines: SpaceMarineGateway,
) {
    fun execute(starshipId: Long, spaceMarineId: Int): Starship {
        requirePositive(starshipId, "starship-id")
        requirePositive(spaceMarineId.toLong(), "space-marine-id")

        val starship = repository.findById(starshipId) ?: throw StarshipNotFoundException(starshipId)
        if (starship.hasOnBoard(spaceMarineId)) {
            throw SpaceMarineAlreadyOnBoardException(starshipId, spaceMarineId)
        }
        // Десантник должен существовать в первом сервисе — иначе на борту оказался бы фантом.
        if (!spaceMarines.exists(spaceMarineId)) {
            throw SpaceMarineNotFoundException(spaceMarineId)
        }
        return repository.save(starship.board(spaceMarineId))
    }
}

class UnloadSpaceMarine(
    private val repository: StarshipRepository,
    private val spaceMarines: SpaceMarineGateway,
) {
    fun execute(starshipId: Long, spaceMarineId: Int): UnloadResult {
        requirePositive(starshipId, "starship-id")
        requirePositive(spaceMarineId.toLong(), "space-marine-id")

        val starship = repository.findById(starshipId) ?: throw StarshipNotFoundException(starshipId)

        // Локальная проверка идёт до сетевого вызова: если десантника нет на борту,
        // беспокоить первый сервис незачем.
        if (!starship.hasOnBoard(spaceMarineId)) {
            throw SpaceMarineNotOnBoardException(starshipId, spaceMarineId)
        }

        // Спецификация предписывает обратиться к GET /space-marines/{id} первого сервиса
        // для проверки существования десантника.
        if (!spaceMarines.exists(spaceMarineId)) {
            throw SpaceMarineNotFoundException(spaceMarineId)
        }

        repository.save(starship.unload(spaceMarineId))

        return UnloadResult(
            starshipId = starshipId,
            spaceMarineId = spaceMarineId,
            message = "Десантник $spaceMarineId высажен с корабля $starshipId",
        )
    }
}
