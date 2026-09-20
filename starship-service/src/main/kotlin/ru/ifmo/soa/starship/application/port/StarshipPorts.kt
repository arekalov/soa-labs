package ru.ifmo.soa.starship.application.port

import ru.ifmo.soa.starship.application.query.Page
import ru.ifmo.soa.starship.application.query.StarshipQuery
import ru.ifmo.soa.starship.domain.model.Starship

/**
 * Порт хранилища кораблей. Реализация живёт в адаптере персистентности.
 */
interface StarshipRepository {

    fun findById(id: Long): Starship?

    fun existsById(id: Long): Boolean

    /** Следующий свободный идентификатор — для создания без указания id клиентом. */
    fun nextId(): Long

    fun create(starship: Starship): Starship

    /** Сохраняет изменившееся состояние: название или состав экипажа. */
    fun save(starship: Starship): Starship

    /** @return `true`, если корабль существовал и был удалён. */
    fun deleteById(id: Long): Boolean

    fun list(query: StarshipQuery): Page<Starship>

    /** Корабль, на борту которого находится десантник, или `null`. Десантник бывает только на одном корабле. */
    fun findByMarine(spaceMarineId: Int): Starship?
}

/**
 * Порт обращения к первому сервису.
 *
 * Прикладной слой знает только о том, что десантника можно проверить на существование.
 * Как именно — по HTTPS с самоподписанным сертификатом, с каким таймаутом — решает адаптер.
 */
interface SpaceMarineGateway {

    /**
     * @return `true`, если десантник существует в первом сервисе.
     * @throws ru.ifmo.soa.starship.application.error.SpaceMarineServiceUnavailableException
     *         если первый сервис недоступен или нарушил контракт.
     */
    fun exists(spaceMarineId: Int): Boolean
}
