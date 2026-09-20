package ru.ifmo.soa.spacemarine.repository

import jakarta.enterprise.context.ApplicationScoped
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.Order
import jakarta.persistence.criteria.Path
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import ru.ifmo.soa.spacemarine.model.Chapter
import ru.ifmo.soa.spacemarine.model.SpaceMarine
import ru.ifmo.soa.spacemarine.query.Page
import ru.ifmo.soa.spacemarine.query.SortSpec
import ru.ifmo.soa.spacemarine.query.SpaceMarineField
import ru.ifmo.soa.spacemarine.query.SpaceMarineQuery

private const val PERSISTENCE_UNIT = "spaceMarinePU"
private const val LIKE_ESCAPE = '\\'

/**
 * Доступ к таблице десантников на JPA Criteria API.
 *
 * Spring Data здесь неприменима: сервис разворачивается на WildFly и построен на CDI,
 * контекста Spring в нём нет.
 */
@ApplicationScoped
open class SpaceMarineRepository {
    // Приватное намеренно: у protected-свойства Kotlin генерирует финальный геттер,
    // и Weld отказывается строить прокси для бина (WELD-001480).
    @PersistenceContext(unitName = PERSISTENCE_UNIT)
    private lateinit var em: EntityManager

    open fun findById(id: Int): SpaceMarine? = em.find(SpaceMarine::class.java, id)

    open fun save(marine: SpaceMarine): SpaceMarine {
        val saved = if (marine.id == null) marine.also(em::persist) else em.merge(marine)
        em.flush()
        return saved
    }

    open fun deleteById(id: Int): Boolean {
        val marine = em.find(SpaceMarine::class.java, id) ?: return false
        em.remove(marine)
        return true
    }

    open fun search(query: SpaceMarineQuery): Page<SpaceMarine> {
        val cb = em.criteriaBuilder
        val total = count { root -> predicates(cb, root, query.filters) }
        if (isPageBeyond(total, query.offset)) return Page(emptyList(), query.page, query.size, total)

        val cq = cb.createQuery(SpaceMarine::class.java)
        val root = cq.from(SpaceMarine::class.java)
        cq.select(root)
            .where(*predicates(cb, root, query.filters))
            .orderBy(orders(cb, root, query.sort))

        val items = em.createQuery(cq)
            .setFirstResult(query.offset.toInt())
            .setMaxResults(query.size)
            .resultList

        return Page(items, query.page, query.size, total)
    }

    open fun countByChapter(name: String, parentLegion: String?): Long = count { root ->
        val cb = em.criteriaBuilder
        val chapter = root.get<Chapter>("chapter")
        val conditions = mutableListOf(cb.equal(chapter.get<String>("name"), name))
        if (parentLegion != null) {
            conditions += cb.equal(chapter.get<String>("parentLegion"), parentLegion)
        }
        conditions.toTypedArray()
    }

    open fun countByHealthGreaterThan(threshold: Float): Long = count { root ->
        arrayOf(em.criteriaBuilder.greaterThan(root.get<Float>("health"), threshold))
    }

    open fun findByNamePrefix(prefix: String, page: Int, size: Int): Page<SpaceMarine> {
        val cb = em.criteriaBuilder
        val pattern = escapeLike(prefix) + "%"
        val total = count { root -> arrayOf(cb.like(root.get<String>("name"), pattern, LIKE_ESCAPE)) }
        val offset = page.toLong() * size
        if (isPageBeyond(total, offset)) return Page(emptyList(), page, size, total)

        val cq = cb.createQuery(SpaceMarine::class.java)
        val root = cq.from(SpaceMarine::class.java)
        cq.select(root)
            .where(cb.like(root.get<String>("name"), pattern, LIKE_ESCAPE))
            .orderBy(cb.asc(root.get<Int>("id")))

        val items = em.createQuery(cq)
            .setFirstResult(offset.toInt())
            .setMaxResults(size)
            .resultList

        return Page(items, page, size, total)
    }

    private fun count(conditions: (Root<SpaceMarine>) -> Array<Predicate>): Long {
        val cb = em.criteriaBuilder
        val cq = cb.createQuery(Long::class.javaObjectType)
        val root = cq.from(SpaceMarine::class.java)
        cq.select(cb.count(root)).where(*conditions(root))
        return em.createQuery(cq).singleResult
    }

    private fun isPageBeyond(total: Long, offset: Long): Boolean =
        total == 0L || offset >= total || offset > Int.MAX_VALUE

    /** Префикс — буквальная строка: иначе «50%» нашёл бы всё, что начинается с «50». */
    private fun escapeLike(value: String): String = value
        .replace("\\", "\\\\")
        .replace("%", "\\%")
        .replace("_", "\\_")

    private fun predicates(
        cb: CriteriaBuilder,
        root: Root<SpaceMarine>,
        filters: Map<SpaceMarineField, Any>,
    ): Array<Predicate> = filters
        .map { (field, value) -> cb.equal(root.resolve<Any>(field), value) }
        .toTypedArray()

    private fun orders(cb: CriteriaBuilder, root: Root<SpaceMarine>, sort: List<SortSpec>): List<Order> {
        val result = sort.mapTo(mutableListOf<Order>()) { spec ->
            val path = root.resolve<Any>(spec.field)
            if (spec.descending) cb.desc(path) else cb.asc(path)
        }
        // Без стабилизатора порядок строк с равными ключами не гарантирован,
        // и соседние страницы начнут давать дубли и пропуски.
        if (sort.none { it.field == SpaceMarineField.ID }) {
            result += cb.asc(root.get<Int>("id"))
        }
        return result
    }

    private fun <T> Root<SpaceMarine>.resolve(field: SpaceMarineField): Path<T> {
        var path: Path<*> = this
        for (segment in field.entityPath) {
            path = path.get<Any>(segment)
        }
        @Suppress("UNCHECKED_CAST")
        return path as Path<T>
    }
}
