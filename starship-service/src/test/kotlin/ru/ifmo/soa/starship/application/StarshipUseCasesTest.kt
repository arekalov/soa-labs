package ru.ifmo.soa.starship.application

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import ru.ifmo.soa.starship.application.error.InvalidParameterException
import ru.ifmo.soa.starship.application.error.SpaceMarineAlreadyOnBoardException
import ru.ifmo.soa.starship.application.error.SpaceMarineNotFoundException
import ru.ifmo.soa.starship.application.error.SpaceMarineNotOnBoardException
import ru.ifmo.soa.starship.application.error.SpaceMarineServiceUnavailableException
import ru.ifmo.soa.starship.application.error.StarshipAlreadyExistsException
import ru.ifmo.soa.starship.application.error.StarshipNotFoundException
import ru.ifmo.soa.starship.application.error.StarshipValidationException
import ru.ifmo.soa.starship.application.port.SpaceMarineGateway
import ru.ifmo.soa.starship.application.port.StarshipRepository
import ru.ifmo.soa.starship.application.query.Page
import ru.ifmo.soa.starship.application.query.StarshipQuery
import ru.ifmo.soa.starship.application.usecase.BoardSpaceMarine
import ru.ifmo.soa.starship.application.usecase.CreateStarship
import ru.ifmo.soa.starship.application.usecase.CreateStarshipWithGeneratedId
import ru.ifmo.soa.starship.application.usecase.UnloadSpaceMarine
import ru.ifmo.soa.starship.domain.model.Starship

/**
 * Сценарии проверяются на подставных портах — без Spring, без базы и без сети.
 * Это и есть практическая отдача от того, что прикладной слой не знает о фреймворке.
 */
class StarshipUseCasesTest {

    private class FakeRepository(vararg ships: Starship) : StarshipRepository {
        val storage = ships.associateBy { it.id }.toMutableMap()
        var saved: Starship? = null

        override fun findById(id: Long): Starship? = storage[id]
        override fun existsById(id: Long): Boolean = id in storage
        override fun create(starship: Starship): Starship {
            storage[starship.id] = starship
            return starship
        }

        override fun save(starship: Starship): Starship {
            storage[starship.id] = starship
            saved = starship
            return starship
        }

        override fun nextId(): Long = (storage.keys.maxOrNull() ?: 0L) + 1
        override fun deleteById(id: Long): Boolean = storage.remove(id) != null
        override fun list(query: StarshipQuery): Page<Starship> =
            Page(storage.values.sortedBy { it.id }, query.page, query.size, storage.size.toLong())
    }

    private class FakeGateway(
        private val known: Set<Int> = emptySet(),
        private val failure: RuntimeException? = null,
    ) : SpaceMarineGateway {
        var calls = 0

        override fun exists(spaceMarineId: Int): Boolean {
            calls++
            failure?.let { throw it }
            return spaceMarineId in known
        }
    }

    // ------------------------------------------------------------- создание

    @Test
    @DisplayName("корабль создаётся с идентификатором из пути, а не сгенерированным")
    fun `creates with client supplied id`() {
        val repository = FakeRepository()

        val ship = CreateStarship(repository).execute(7L, "Macragge's Honour")

        assertThat(ship.id).isEqualTo(7L)
        assertThat(ship.name).isEqualTo("Macragge's Honour")
        assertThat(ship.marines).isEmpty()
    }

    @Test
    @DisplayName("повторный идентификатор даёт конфликт, а не молчаливую перезапись")
    fun `duplicate id is a conflict`() {
        val repository = FakeRepository(Starship(1L, "Existing"))

        assertThatThrownBy { CreateStarship(repository).execute(1L, "Another") }
            .isInstanceOf(StarshipAlreadyExistsException::class.java)

        // Существующий корабль не пострадал
        assertThat(repository.storage[1L]?.name).isEqualTo("Existing")
    }

    @Test
    @DisplayName("некорректные параметры отвергаются до обращения к хранилищу")
    fun `rejects invalid parameters`() {
        val create = CreateStarship(FakeRepository())

        assertThatThrownBy { create.execute(0L, "Ship") }
            .isInstanceOf(InvalidParameterException::class.java)
        assertThatThrownBy { create.execute(-1L, "Ship") }
            .isInstanceOf(InvalidParameterException::class.java)
        assertThatThrownBy { create.execute(1L, "  ") }
            .isInstanceOf(InvalidParameterException::class.java)
        assertThatThrownBy { create.execute(1L, null) }
            .isInstanceOf(InvalidParameterException::class.java)
    }

    @Test
    @DisplayName("создание без идентификатора берёт следующий свободный номер")
    fun `generated id follows the last one`() {
        val repository = FakeRepository(Starship(4L, "Old"))

        val ship = CreateStarshipWithGeneratedId(repository).execute("New")

        assertThat(ship.id).isEqualTo(5L)
        assertThatThrownBy { CreateStarshipWithGeneratedId(repository).execute("  ") }
            .isInstanceOf(StarshipValidationException::class.java)
    }

    // ------------------------------------------------------------- посадка

    @Test
    @DisplayName("посадка проверяет десантника в первом сервисе и добавляет его в экипаж")
    fun `boarding adds an existing marine`() {
        val repository = FakeRepository(Starship(1L, "Ship"))
        val gateway = FakeGateway(known = setOf(7))

        val ship = BoardSpaceMarine(repository, gateway).execute(1L, 7)

        assertThat(ship.marines).containsExactly(7)
        assertThat(gateway.calls).isEqualTo(1)
    }

    @Test
    @DisplayName("повторная посадка — конфликт, неизвестный десантник — 404")
    fun `boarding rejects duplicates and unknown marines`() {
        val repository = FakeRepository(Starship(1L, "Ship", setOf(7)))

        assertThatThrownBy { BoardSpaceMarine(repository, FakeGateway(known = setOf(7))).execute(1L, 7) }
            .isInstanceOf(SpaceMarineAlreadyOnBoardException::class.java)
        assertThatThrownBy { BoardSpaceMarine(repository, FakeGateway()).execute(1L, 8) }
            .isInstanceOf(SpaceMarineNotFoundException::class.java)
    }

    // ------------------------------------------------------------- высадка

    @Test
    @DisplayName("высадка снимает десантника с борта и возвращает описание результата")
    fun `unload removes marine from board`() {
        val repository = FakeRepository(Starship(1L, "Ship", setOf(10, 20)))
        val gateway = FakeGateway(known = setOf(10))

        val result = UnloadSpaceMarine(repository, gateway).execute(1L, 10)

        assertThat(result.starshipId).isEqualTo(1L)
        assertThat(result.spaceMarineId).isEqualTo(10)
        assertThat(result.message).isEqualTo("Десантник 10 высажен с корабля 1")
        assertThat(repository.saved?.marines).containsExactly(20)
    }

    @Test
    @DisplayName("несуществующий корабль — 404, первый сервис при этом не беспокоим")
    fun `missing starship does not reach upstream`() {
        val gateway = FakeGateway(known = setOf(10))

        assertThatThrownBy { UnloadSpaceMarine(FakeRepository(), gateway).execute(99L, 10) }
            .isInstanceOf(StarshipNotFoundException::class.java)

        assertThat(gateway.calls).isZero()
    }

    @Test
    @DisplayName("десантник не на этом корабле — 404, и сетевой вызов не делается")
    fun `marine not on board short circuits before network call`() {
        val repository = FakeRepository(Starship(1L, "Ship", setOf(20)))
        val gateway = FakeGateway(known = setOf(10))

        assertThatThrownBy { UnloadSpaceMarine(repository, gateway).execute(1L, 10) }
            .isInstanceOf(SpaceMarineNotOnBoardException::class.java)

        // Дешёвая локальная проверка идёт первой: незачем ходить по сети,
        // чтобы узнать то, что уже известно из своей базы.
        assertThat(gateway.calls).isZero()
    }

    @Test
    @DisplayName("десантника нет в первом сервисе — 404, состав экипажа не меняется")
    fun `unknown marine upstream is not found`() {
        val repository = FakeRepository(Starship(1L, "Ship", setOf(10)))
        val gateway = FakeGateway(known = emptySet())

        assertThatThrownBy { UnloadSpaceMarine(repository, gateway).execute(1L, 10) }
            .isInstanceOf(SpaceMarineNotFoundException::class.java)

        assertThat(gateway.calls).isEqualTo(1)
        assertThat(repository.saved).isNull()
    }

    @Test
    @DisplayName("недоступность первого сервиса прокидывается наверх и не теряет экипаж")
    fun `upstream failure propagates without mutating state`() {
        val repository = FakeRepository(Starship(1L, "Ship", setOf(10)))
        val gateway = FakeGateway(
            failure = SpaceMarineServiceUnavailableException("Сервис SpaceMarine недоступен"),
        )

        assertThatThrownBy { UnloadSpaceMarine(repository, gateway).execute(1L, 10) }
            .isInstanceOf(SpaceMarineServiceUnavailableException::class.java)

        // Ничего не записано: высадка либо происходит целиком, либо не происходит вовсе
        assertThat(repository.saved).isNull()
        assertThat(repository.storage[1L]?.marines).containsExactly(10)
    }

    @Test
    @DisplayName("идентификаторы проверяются до любых обращений наружу")
    fun `validates identifiers first`() {
        val unload = UnloadSpaceMarine(FakeRepository(), FakeGateway())

        assertThatThrownBy { unload.execute(0L, 10) }
            .isInstanceOf(InvalidParameterException::class.java)
        assertThatThrownBy { unload.execute(1L, 0) }
            .isInstanceOf(InvalidParameterException::class.java)
    }

    @Test
    @DisplayName("инварианты корабля держатся и при прямом создании")
    fun `domain invariants hold`() {
        assertThatThrownBy { Starship(0L, "Ship") }.isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy { Starship(1L, "") }.isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy { Starship(1L, "Ship", setOf(0)) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }
}
