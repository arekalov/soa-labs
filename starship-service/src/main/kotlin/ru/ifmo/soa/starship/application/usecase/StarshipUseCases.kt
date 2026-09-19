package ru.ifmo.soa.starship.application.usecase

import ru.ifmo.soa.starship.application.error.InvalidParameterException
import ru.ifmo.soa.starship.application.error.SpaceMarineNotFoundException
import ru.ifmo.soa.starship.application.error.SpaceMarineNotOnBoardException
import ru.ifmo.soa.starship.application.error.StarshipAlreadyExistsException
import ru.ifmo.soa.starship.application.error.StarshipNotFoundException
import ru.ifmo.soa.starship.application.port.SpaceMarineGateway
import ru.ifmo.soa.starship.application.port.StarshipRepository
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

/**
 * Сценарии второго сервиса. Классы намеренно без аннотаций Spring: бины объявлены
 * отдельной конфигурацией в адаптере, поэтому прикладной слой остаётся независимым
 * от фреймворка, и архитектурный тест может это проверить.
 */
class CreateStarship(
    private val repository: StarshipRepository,
) {
    fun execute(id: Long, name: String?): Starship {
        if (id <= 0) {
            throw InvalidParameterException("Параметр 'id' должен быть целым числом больше 0")
        }
        if (name.isNullOrBlank()) {
            throw InvalidParameterException("Параметр 'name' не может быть пустым")
        }

        // Сначала проверка, потом запись: у сущности с присвоенным идентификатором
        // сохранение уходит в merge и молча перезаписало бы чужой корабль.
        // Гонку добирает адаптер, перехватывая нарушение уникальности.
        if (repository.existsById(id)) throw StarshipAlreadyExistsException(id)

        return repository.create(Starship(id = id, name = name))
    }
}

class UnloadSpaceMarine(
    private val repository: StarshipRepository,
    private val spaceMarines: SpaceMarineGateway,
) {
    fun execute(starshipId: Long, spaceMarineId: Int): UnloadResult {
        if (starshipId <= 0) {
            throw InvalidParameterException("Параметр 'starship-id' должен быть целым числом больше 0")
        }
        if (spaceMarineId <= 0) {
            throw InvalidParameterException("Параметр 'space-marine-id' должен быть целым числом больше 0")
        }

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
