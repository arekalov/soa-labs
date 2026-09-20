package ru.ifmo.soa.starship.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
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
import ru.ifmo.soa.starship.query.StarshipFilter
import ru.ifmo.soa.starship.query.StarshipQuery
import ru.ifmo.soa.starship.repository.StarshipRepository
import java.util.Optional

/**
 * Сценарии проверяются на подставном хранилище и подставном клиенте первого сервиса —
 * без Spring, без базы и без сети.
 */
class StarshipServiceTest {

    private val repository: StarshipRepository = mock()
    private val spaceMarines: SpaceMarineClient = mock()
    private val service = StarshipService(repository, spaceMarines)

    private fun ship(id: Long, name: String = "Ship", vararg marines: Int) =
        Starship(id, name, marines.toMutableSet())

    private fun storedShip(id: Long, name: String = "Ship", vararg marines: Int): Starship {
        val starship = ship(id, name, *marines)
        whenever(repository.findById(id)).doReturn(Optional.of(starship))
        whenever(repository.existsById(id)).doReturn(true)
        whenever(repository.save(any<Starship>())).thenAnswer { it.arguments[0] as Starship }
        return starship
    }

    // ------------------------------------------------------------- создание

    @Test
    @DisplayName("корабль создаётся с идентификатором из пути, а не сгенерированным")
    fun `creates with client supplied id`() {
        whenever(repository.existsById(7L)).doReturn(false)
        whenever(repository.insert(any())).thenAnswer { it.arguments[0] as Starship }

        val created = service.createWithId(7L, "Macragge's Honour")

        assertThat(created.id).isEqualTo(7L)
        assertThat(created.name).isEqualTo("Macragge's Honour")
        assertThat(created.marines).isEmpty()
        verify(repository, never()).save(any<Starship>())
    }

    @Test
    @DisplayName("повторный идентификатор даёт конфликт, а не молчаливую перезапись")
    fun `duplicate id is a conflict`() {
        whenever(repository.existsById(1L)).doReturn(true)

        assertThatThrownBy { service.createWithId(1L, "Another") }
            .isInstanceOf(StarshipAlreadyExistsException::class.java)

        verify(repository, never()).insert(any())
    }

    @Test
    @DisplayName("некорректные параметры отвергаются до обращения к хранилищу")
    fun `rejects invalid parameters`() {
        assertThatThrownBy { service.createWithId(0L, "Ship") }
            .isInstanceOf(InvalidParameterException::class.java)
        assertThatThrownBy { service.createWithId(-1L, "Ship") }
            .isInstanceOf(InvalidParameterException::class.java)
        assertThatThrownBy { service.createWithId(1L, "  ") }
            .isInstanceOf(InvalidParameterException::class.java)
        assertThatThrownBy { service.createWithId(1L, null) }
            .isInstanceOf(InvalidParameterException::class.java)

        verify(repository, never()).insert(any())
    }

    @Test
    @DisplayName("создание без идентификатора берёт номер из последовательности")
    fun `generated id comes from the sequence`() {
        whenever(repository.nextId()).doReturn(5L)
        whenever(repository.insert(any())).thenAnswer { it.arguments[0] as Starship }

        assertThat(service.create("New").id).isEqualTo(5L)
    }

    @Test
    @DisplayName("пустое название — нарушение ограничений, а не ошибка параметра")
    fun `blank name violates constraints`() {
        assertThatThrownBy { service.create("  ") }
            .isInstanceOf(StarshipValidationException::class.java)
    }

    // ---------------------------------------------------------- чтение и сверка

    @Test
    @DisplayName("удалённый в первом сервисе десантник снимается с борта при чтении")
    fun `reading drops marines that no longer exist`() {
        val starship = storedShip(1L, marines = intArrayOf(10, 20))
        whenever(spaceMarines.exists(10)).doReturn(true)
        whenever(spaceMarines.exists(20)).doReturn(false)

        assertThat(service.getById(1L).marines).containsExactly(10)
        verify(repository).save(starship)
    }

    @Test
    @DisplayName("если состав актуален, ничего не сохраняется")
    fun `reading an intact crew writes nothing`() {
        storedShip(1L, marines = intArrayOf(10))
        whenever(spaceMarines.exists(10)).doReturn(true)

        service.getById(1L)

        verify(repository, never()).save(any<Starship>())
    }

    @Test
    @DisplayName("при недоступном первом сервисе список читается как есть")
    fun `listing survives upstream outage`() {
        val starship = ship(1L, "Ship", 10, 20)
        whenever(repository.findAll(any<org.springframework.data.jpa.domain.Specification<Starship>>(), any<PageRequest>()))
            .doReturn(PageImpl(listOf(starship)))
        whenever(spaceMarines.exists(any())).doThrow(SpaceMarineServiceUnavailableException("недоступен"))

        val page = service.list(StarshipQuery(StarshipFilter.NONE, PageRequest.of(0, 20)))

        assertThat(page.content.single().marines).containsExactlyInAnyOrder(10, 20)
        verify(repository, never()).save(any<Starship>())
    }

    @Test
    @DisplayName("несуществующий корабль — 404")
    fun `missing starship is not found`() {
        whenever(repository.findById(99L)).doReturn(Optional.empty())

        assertThatThrownBy { service.getById(99L) }.isInstanceOf(StarshipNotFoundException::class.java)
    }

    // ------------------------------------------------------------- посадка

    @Test
    @DisplayName("посадка проверяет десантника в первом сервисе и добавляет его в экипаж")
    fun `boarding adds an existing marine`() {
        storedShip(1L)
        whenever(repository.findFirstByMarinesContains(7)).doReturn(null)
        whenever(spaceMarines.exists(7)).doReturn(true)

        assertThat(service.board(1L, 7).marines).containsExactly(7)
        verify(spaceMarines).exists(7)
    }

    @Test
    @DisplayName("повторная посадка на тот же корабль — конфликт")
    fun `boarding the same ship twice is a conflict`() {
        val starship = storedShip(1L, marines = intArrayOf(7))
        whenever(repository.findFirstByMarinesContains(7)).doReturn(starship)

        assertThatThrownBy { service.board(1L, 7) }
            .isInstanceOf(SpaceMarineAlreadyOnBoardException::class.java)

        verify(spaceMarines, never()).exists(any())
    }

    @Test
    @DisplayName("десантник с другого корабля не садится: конфликт называет корабль, где он сейчас")
    fun `marine cannot be on two ships at once`() {
        storedShip(2L, "Beta")
        whenever(repository.findFirstByMarinesContains(7)).doReturn(ship(1L, "Alpha", 7))

        assertThatThrownBy { service.board(2L, 7) }
            .isInstanceOf(SpaceMarineAlreadyOnBoardException::class.java)
            .hasMessageContaining("корабле с id=1")

        verify(spaceMarines, never()).exists(any())
        verify(repository, never()).save(any<Starship>())
    }

    @Test
    @DisplayName("неизвестный первому сервису десантник — 404")
    fun `boarding an unknown marine is not found`() {
        storedShip(1L)
        whenever(repository.findFirstByMarinesContains(8)).doReturn(null)
        whenever(spaceMarines.exists(8)).doReturn(false)

        assertThatThrownBy { service.board(1L, 8) }
            .isInstanceOf(SpaceMarineNotFoundException::class.java)

        verify(repository, never()).save(any<Starship>())
    }

    // ------------------------------------------------------------- высадка

    @Test
    @DisplayName("высадка снимает десантника с борта")
    fun `unload removes marine from board`() {
        val starship = storedShip(1L, marines = intArrayOf(10, 20))
        whenever(spaceMarines.exists(10)).doReturn(true)

        assertThat(service.unload(1L, 10).marines).containsExactly(20)
        verify(repository).save(starship)
    }

    @Test
    @DisplayName("несуществующий корабль — 404, первый сервис при этом не беспокоим")
    fun `missing starship does not reach upstream`() {
        whenever(repository.findById(99L)).doReturn(Optional.empty())

        assertThatThrownBy { service.unload(99L, 10) }.isInstanceOf(StarshipNotFoundException::class.java)

        verify(spaceMarines, never()).exists(any())
    }

    @Test
    @DisplayName("десантник не на этом корабле — 404, и сетевой вызов не делается")
    fun `marine not on board short circuits before network call`() {
        storedShip(1L, marines = intArrayOf(20))

        assertThatThrownBy { service.unload(1L, 10) }
            .isInstanceOf(SpaceMarineNotOnBoardException::class.java)

        verify(spaceMarines, never()).exists(any())
    }

    @Test
    @DisplayName("десантника нет в первом сервисе — 404, состав экипажа не меняется")
    fun `unknown marine upstream is not found`() {
        storedShip(1L, marines = intArrayOf(10))
        whenever(spaceMarines.exists(10)).doReturn(false)

        assertThatThrownBy { service.unload(1L, 10) }
            .isInstanceOf(SpaceMarineNotFoundException::class.java)

        verify(repository, never()).save(any<Starship>())
    }

    @Test
    @DisplayName("недоступность первого сервиса прокидывается наверх и не теряет экипаж")
    fun `upstream failure propagates without mutating state`() {
        val starship = storedShip(1L, marines = intArrayOf(10))
        whenever(spaceMarines.exists(10)).doThrow(SpaceMarineServiceUnavailableException("недоступен"))

        assertThatThrownBy { service.unload(1L, 10) }
            .isInstanceOf(SpaceMarineServiceUnavailableException::class.java)

        verify(repository, never()).save(any<Starship>())
        assertThat(starship.marines).containsExactly(10)
    }

    @Test
    @DisplayName("идентификаторы проверяются до любых обращений наружу")
    fun `validates identifiers first`() {
        assertThatThrownBy { service.unload(0L, 10) }.isInstanceOf(InvalidParameterException::class.java)
        assertThatThrownBy { service.unload(1L, 0) }.isInstanceOf(InvalidParameterException::class.java)

        verify(repository, never()).findById(any())
    }

    // ----------------------------------------------------- переименование и удаление

    @Test
    @DisplayName("переименование меняет только название")
    fun `rename changes the name only`() {
        storedShip(1L, "Old", 10)
        whenever(spaceMarines.exists(10)).doReturn(true)

        val renamed = service.rename(1L, "New")

        assertThat(renamed.name).isEqualTo("New")
        assertThat(renamed.marines).containsExactly(10)
    }

    @Test
    @DisplayName("пустое название при переименовании — 422")
    fun `rename rejects a blank name`() {
        storedShip(1L, "Old")

        assertThatThrownBy { service.rename(1L, " ") }
            .isInstanceOf(StarshipValidationException::class.java)
    }

    @Test
    @DisplayName("удаление несуществующего корабля — 404")
    fun `delete of a missing starship is not found`() {
        whenever(repository.existsById(42L)).doReturn(false)

        assertThatThrownBy { service.delete(42L) }.isInstanceOf(StarshipNotFoundException::class.java)

        verify(repository, never()).deleteById(any())
    }
}
