package ru.ifmo.soa.starship.repository

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.transaction.annotation.Transactional
import ru.ifmo.soa.starship.model.Starship

/**
 * Реализация фрагмента [StarshipInsert]; Spring Data подмешивает её в репозиторий по имени.
 *
 * `@Transactional` объявлена на классе, а не на методе: своим фрагментам Spring Data
 * транзакцию не открывает, а плагин kotlin-spring делает открытым для наследования
 * именно аннотированный класс — иначе CGLIB не построил бы прокси.
 */
@Transactional
class StarshipInsertImpl : StarshipInsert {

    @PersistenceContext
    private lateinit var em: EntityManager

    override fun insert(starship: Starship): Starship {
        em.persist(starship)
        // Нарушение первичного ключа должно всплыть здесь, а не при фиксации транзакции.
        em.flush()
        return starship
    }
}
