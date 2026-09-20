package ru.ifmo.soa.spacemarine.adapter.persistence

import jakarta.enterprise.context.ApplicationScoped
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.Order
import jakarta.persistence.criteria.Path
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import ru.ifmo.soa.spacemarine.application.port.SpaceMarineRepository
import ru.ifmo.soa.spacemarine.application.query.Page
import ru.ifmo.soa.spacemarine.application.query.Paging
import ru.ifmo.soa.spacemarine.application.query.SpaceMarineField
import ru.ifmo.soa.spacemarine.application.query.SpaceMarineQuery
import ru.ifmo.soa.spacemarine.application.query.SortSpec
import ru.ifmo.soa.spacemarine.domain.model.SpaceMarine

/**
 * Реализация порта хранилища на JPA Criteria API.
 *
 * Динамическая фильтрация по 11 полям, многоуровневая сортировка и пагинация —
 * самая объёмная часть сервиса, поэтому она собрана здесь целиком.
 */
@ApplicationScoped
open class JpaSpaceMarineRepository : SpaceMarineRepository {

    @PersistenceContext(unitName = PERSISTENCE_UNIT)
    protected lateinit var em: EntityManager

    override fun findById(id: Int): SpaceMarine? =
        em.find(SpaceMarineEntity::class.java, id)?.let(SpaceMarineEntityMapper::toDomain)

    override fun create(marine: SpaceMarine): SpaceMarine {
        val entity = SpaceMarineEntityMapper.toNewEntity(marine)
        em.persist(entity)
        // flush нужен сразу: идентификатор генерирует база, а вернуть его требуется в ответе 201
        em.flush()
        return SpaceMarineEntityMapper.toDomain(entity)
    }

    override fun update(marine: SpaceMarine): SpaceMarine {
        val id = requireNotNull(marine.id) { "Обновление требует идентификатор" }
        val entity = em.find(SpaceMarineEntity::class.java, id)
            ?: error("Элемент с id=$id исчез между чтением и записью")
        SpaceMarineEntityMapper.applyTo(entity, marine)
        em.flush()
        return SpaceMarineEntityMapper.toDomain(entity)
    }

    override fun deleteById(id: Int): Boolean {
        val entity = em.find(SpaceMarineEntity::class.java, id) ?: return false
        em.remove(entity)
        return true
    }

    override fun search(query: SpaceMarineQuery): Page<SpaceMarine> {
        val cb = em.criteriaBuilder

        val total = countMatching(cb, query)

        // Ранний выход бережёт лишний запрос и заодно прикрывает переполнение setFirstResult,
        // который принимает только Int.
        if (total == 0L || query.offset >= total || query.offset > Int.MAX_VALUE) {
            return Page(emptyList(), query.page, query.size, total)
        }

        val dataQuery = cb.createQuery(SpaceMarineEntity::class.java)
        val root = dataQuery.from(SpaceMarineEntity::class.java)
        dataQuery.select(root)
            .where(*predicates(cb, root, query.filters))
            .orderBy(orders(cb, root, query.sort))

        val items = em.createQuery(dataQuery)
            .setFirstResult(query.offset.toInt())
            .setMaxResults(query.size)
            .resultList
            .map(SpaceMarineEntityMapper::toDomain)

        return Page(items, query.page, query.size, total)
    }

    override fun countByChapter(name: String, parentLegion: String?): Long {
        val cb = em.criteriaBuilder
        val cq = cb.createQuery(Long::class.javaObjectType)
        val root = cq.from(SpaceMarineEntity::class.java)
        val chapter = root.get<ChapterEmbeddable>("chapter")

        val conditions = mutableListOf<Predicate>(
            cb.equal(chapter.get<String>("name"), name),
        )
        // Отсутствие параметра означает «любой легион»: спецификация помечает его необязательным
        // и семантику пропуска не описывает. Решение зафиксировано в отчёте.
        if (parentLegion != null) {
            conditions += cb.equal(chapter.get<String>("parentLegion"), parentLegion)
        }

        cq.select(cb.count(root)).where(*conditions.toTypedArray())
        return em.createQuery(cq).singleResult
    }

    override fun countByHealthGreaterThan(threshold: Float): Long {
        val cb = em.criteriaBuilder
        val cq = cb.createQuery(Long::class.javaObjectType)
        val root = cq.from(SpaceMarineEntity::class.java)
        cq.select(cb.count(root)).where(cb.greaterThan(root.get<Float>("health"), threshold))
        return em.createQuery(cq).singleResult
    }

    override fun findByNamePrefix(prefix: String, paging: Paging): Page<SpaceMarine> {
        val cb = em.criteriaBuilder
        // Спецсимволы LIKE экранируем: префикс — буквальная строка, а не шаблон,
        // иначе "50%" нашёл бы всё, что начинается с "50".
        val pattern = prefix.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%"

        val countQuery = cb.createQuery(Long::class.javaObjectType)
        val countRoot = countQuery.from(SpaceMarineEntity::class.java)
        countQuery.select(cb.count(countRoot)).where(cb.like(countRoot.get<String>("name"), pattern, '\\'))
        val total = em.createQuery(countQuery).singleResult
        if (total == 0L || paging.offset >= total || paging.offset > Int.MAX_VALUE) {
            return Page(emptyList(), paging.page, paging.size, total)
        }

        val cq = cb.createQuery(SpaceMarineEntity::class.java)
        val root = cq.from(SpaceMarineEntity::class.java)
        cq.select(root)
            .where(cb.like(root.get<String>("name"), pattern, '\\'))
            .orderBy(cb.asc(root.get<Int>("id")))
        val items = em.createQuery(cq)
            .setFirstResult(paging.offset.toInt())
            .setMaxResults(paging.size)
            .resultList
            .map(SpaceMarineEntityMapper::toDomain)
        return Page(items, paging.page, paging.size, total)
    }

    private fun countMatching(cb: CriteriaBuilder, query: SpaceMarineQuery): Long {
        val cq = cb.createQuery(Long::class.javaObjectType)
        val root = cq.from(SpaceMarineEntity::class.java)
        // Тот же набор предикатов, что и в выборке: иначе totalElements разойдётся с items
        cq.select(cb.count(root)).where(*predicates(cb, root, query.filters))
        return em.createQuery(cq).singleResult
    }

    private fun predicates(
        cb: CriteriaBuilder,
        root: Root<SpaceMarineEntity>,
        filters: Map<SpaceMarineField, Any>,
    ): Array<Predicate> = filters
        .map { (field, value) -> cb.equal(root.resolve<Any>(field), value) }
        .toTypedArray()

    private fun orders(
        cb: CriteriaBuilder,
        root: Root<SpaceMarineEntity>,
        sort: List<SortSpec>,
    ): List<Order> {
        val result = sort.mapTo(mutableListOf<Order>()) { spec ->
            val path = root.resolve<Any>(spec.field)
            if (spec.descending) cb.desc(path) else cb.asc(path)
        }

        // Стабилизатор: без него Postgres не гарантирует порядок строк с равными ключами,
        // и соседние страницы начнут давать дубли и пропуски. Если сортировка уже идёт
        // по id, добавлять его повторно незачем.
        if (sort.none { it.field == SpaceMarineField.ID }) {
            result += cb.asc(root.get<Int>("id"))
        }
        return result
    }

    /** Разворачивает поле API в путь внутри сущности, в том числе внутрь встроенных объектов. */
    private fun <T> Root<SpaceMarineEntity>.resolve(field: SpaceMarineField): Path<T> {
        var path: Path<*> = this
        for (segment in entityPathOf(field)) {
            path = path.get<Any>(segment)
        }
        @Suppress("UNCHECKED_CAST")
        return path as Path<T>
    }

    companion object {
        const val PERSISTENCE_UNIT = "spaceMarinePU"

        /**
         * Отображение полей API в пути сущности.
         *
         * Живёт здесь, а не в справочнике полей: знание о том, что координаты хранятся
         * встроенным объектом, принадлежит слою персистентности.
         *
         * Намеренно `when`, а не карта: при добавлении поля в перечисление компилятор
         * потребует дописать ветку, тогда как забытый ключ карты упал бы только в рантайме.
         */
        private fun entityPathOf(field: SpaceMarineField): List<String> = when (field) {
            SpaceMarineField.ID -> listOf("id")
            SpaceMarineField.NAME -> listOf("name")
            SpaceMarineField.COORDINATES_X -> listOf("coordinates", "x")
            SpaceMarineField.COORDINATES_Y -> listOf("coordinates", "y")
            SpaceMarineField.CREATION_DATE -> listOf("creationDate")
            SpaceMarineField.HEALTH -> listOf("health")
            SpaceMarineField.LOYAL -> listOf("loyal")
            SpaceMarineField.ACHIEVEMENTS -> listOf("achievements")
            SpaceMarineField.CATEGORY -> listOf("category")
            SpaceMarineField.CHAPTER_NAME -> listOf("chapter", "name")
            SpaceMarineField.CHAPTER_PARENT_LEGION -> listOf("chapter", "parentLegion")
        }
    }
}
