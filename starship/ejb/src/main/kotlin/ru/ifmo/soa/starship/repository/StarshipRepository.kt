package ru.ifmo.soa.starship.repository

import jakarta.enterprise.context.ApplicationScoped
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.persistence.criteria.Order
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import ru.ifmo.soa.starship.model.Starship
import ru.ifmo.soa.starship.query.StarshipField
import ru.ifmo.soa.starship.query.StarshipFilter
import ru.ifmo.soa.starship.query.StarshipQuery

private const val PERSISTENCE_UNIT = "starshipPU"

/** Страница выборки. Пустая — полноценный результат, а не 404. */
data class Page<T>(
    val items: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
) {
    val totalPages: Int
        get() = if (totalElements == 0L) 0 else ((totalElements + size - 1) / size).toInt()
}

@ApplicationScoped
open class StarshipRepository {
    // Приватное намеренно: у protected-свойства Kotlin генерирует финальный геттер,
    // и Weld отказывается строить прокси для бина (WELD-001480).
    @PersistenceContext(unitName = PERSISTENCE_UNIT)
    private lateinit var em: EntityManager

    open fun findById(id: Long): Starship? = em.find(Starship::class.java, id)

    open fun existsById(id: Long): Boolean = count { root ->
        arrayOf(em.criteriaBuilder.equal(root.get<Long>("id"), id))
    } > 0

    /** Именно persist, а не merge: merge на существующем id молча перезаписал бы чужую строку. */
    open fun insert(starship: Starship): Starship {
        em.persist(starship)
        em.flush()
        return starship
    }

    open fun save(starship: Starship): Starship {
        val saved = em.merge(starship)
        em.flush()
        return saved
    }

    open fun deleteById(id: Long): Boolean {
        val starship = em.find(Starship::class.java, id) ?: return false
        em.remove(starship)
        return true
    }

    open fun nextId(): Long = em
        .createNativeQuery("select nextval(pg_get_serial_sequence('starship', 'id'))")
        .singleResult
        .let { (it as Number).toLong() }

    open fun findFirstByMarine(spaceMarineId: Int): Starship? {
        val cb = em.criteriaBuilder
        val cq = cb.createQuery(Starship::class.java)
        val root = cq.from(Starship::class.java)
        cq.select(root).where(cb.isMember(spaceMarineId, root.get("marines")))
        return em.createQuery(cq).setMaxResults(1).resultList.firstOrNull()
    }

    open fun search(query: StarshipQuery): Page<Starship> {
        val cb = em.criteriaBuilder
        val total = count { root -> predicates(root, query.filter) }
        if (total == 0L || query.offset >= total) return Page(emptyList(), query.page, query.size, total)

        val cq = cb.createQuery(Starship::class.java)
        val root = cq.from(Starship::class.java)
        cq.select(root).where(*predicates(root, query.filter)).orderBy(orders(root, query))

        val items = em.createQuery(cq)
            .setFirstResult(query.offset.toInt())
            .setMaxResults(query.size)
            .resultList

        return Page(items, query.page, query.size, total)
    }

    private fun count(conditions: (Root<Starship>) -> Array<Predicate>): Long {
        val cb = em.criteriaBuilder
        val cq = cb.createQuery(Long::class.javaObjectType)
        val root = cq.from(Starship::class.java)
        cq.select(cb.count(root)).where(*conditions(root))
        return em.createQuery(cq).singleResult
    }

    private fun predicates(root: Root<Starship>, filter: StarshipFilter): Array<Predicate> {
        val cb = em.criteriaBuilder
        return listOfNotNull(
            filter.id?.let { cb.equal(root.get<Long>("id"), it) },
            filter.name?.let { cb.equal(root.get<String>("name"), it) },
        ).toTypedArray()
    }

    private fun orders(root: Root<Starship>, query: StarshipQuery): List<Order> {
        val cb = em.criteriaBuilder
        val result = query.sort.mapTo(mutableListOf<Order>()) { spec ->
            val path = root.get<Any>(spec.field.apiName)
            if (spec.descending) cb.desc(path) else cb.asc(path)
        }
        // Без стабилизатора соседние страницы начнут давать дубли и пропуски.
        if (query.sort.none { it.field == StarshipField.ID }) result += cb.asc(root.get<Long>("id"))
        return result
    }
}
