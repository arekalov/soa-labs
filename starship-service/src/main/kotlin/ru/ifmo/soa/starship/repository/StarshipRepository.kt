package ru.ifmo.soa.starship.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import ru.ifmo.soa.starship.model.Starship

/**
 * Хранилище кораблей на Spring Data.
 *
 * [JpaSpecificationExecutor] даёт постраничную выборку с динамическими фильтрами,
 * а фрагмент [StarshipInsert] — вставку с заранее известным идентификатором.
 */
interface StarshipRepository :
    JpaRepository<Starship, Long>,
    JpaSpecificationExecutor<Starship>,
    StarshipInsert {
    fun findFirstByMarinesContains(spaceMarineId: Int): Starship?

    @Query(value = "select nextval(pg_get_serial_sequence('starship', 'id'))", nativeQuery = true)
    fun nextId(): Long
}
