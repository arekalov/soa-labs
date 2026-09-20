package ru.ifmo.soa.starship.adapter.persistence

import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.EntityManager
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.PersistenceContext
import jakarta.persistence.Table
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import ru.ifmo.soa.starship.application.error.StarshipAlreadyExistsException
import ru.ifmo.soa.starship.application.port.StarshipRepository
import ru.ifmo.soa.starship.application.query.Page
import ru.ifmo.soa.starship.application.query.StarshipField
import ru.ifmo.soa.starship.application.query.StarshipFilter
import ru.ifmo.soa.starship.application.query.StarshipQuery
import ru.ifmo.soa.starship.domain.model.Starship

@Entity
@Table(name = "starship")
class StarshipEntity(

    /**
     * Без @GeneratedValue: идентификатор всегда известен до сохранения — либо пришёл
     * из URL (эндпоинт спецификации ЛР1), либо взят из последовательности заранее.
     * Так один и тот же маппинг обслуживает оба способа создания.
     */
    @Id
    @Column(name = "id", nullable = false)
    var id: Long? = null,

    @Column(name = "name", nullable = false)
    var name: String = "",

    /**
     * Идентификаторы десантников из первого сервиса.
     *
     * Внешнего ключа на таблицу десантников нет и быть не может: это другой сервис
     * со своей базой. Целостность поддерживается вызовом REST, а не СУБД.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "starship_marine",
        joinColumns = [JoinColumn(name = "starship_id")],
    )
    @Column(name = "space_marine_id", nullable = false)
    var marines: MutableSet<Int> = mutableSetOf(),
)

interface SpringDataStarshipRepository : JpaRepository<StarshipEntity, Long>, JpaSpecificationExecutor<StarshipEntity>

/**
 * Реализация порта поверх Spring Data.
 *
 * Транзакции объявлены здесь, а не в сценариях: прикладной слой не должен зависеть
 * от Spring. Побочный эффект полезен — сетевой вызов к первому сервису заведомо
 * оказывается вне транзакции БД и не держит соединение из общего на курс пула.
 */
@Repository
class JpaStarshipRepository(
    private val jpa: SpringDataStarshipRepository,
) : StarshipRepository {

    @PersistenceContext
    private lateinit var em: EntityManager

    @Transactional(readOnly = true)
    override fun findById(id: Long): Starship? = jpa.findById(id).orElse(null)?.toDomain()

    @Transactional(readOnly = true)
    override fun existsById(id: Long): Boolean = jpa.existsById(id)

    /** Колонка объявлена как identity, её последовательность и выдаёт номера. */
    @Transactional
    override fun nextId(): Long =
        (em.createNativeQuery("select nextval(pg_get_serial_sequence('starship', 'id'))").singleResult as Number).toLong()

    @Transactional
    override fun create(starship: Starship): Starship = try {
        jpa.save(starship.toEntity()).toDomain()
    } catch (_: DataIntegrityViolationException) {
        // Догоняем гонку: между проверкой existsById и записью корабль мог создать
        // параллельный запрос. Нарушение первичного ключа означает ровно конфликт.
        throw StarshipAlreadyExistsException(starship.id)
    }

    @Transactional
    override fun save(starship: Starship): Starship {
        val entity = jpa.findById(starship.id).orElseThrow {
            IllegalStateException("Корабль с id=${starship.id} исчез между чтением и записью")
        }
        entity.name = starship.name
        entity.marines = starship.marines.toMutableSet()
        return jpa.save(entity).toDomain()
    }

    @Transactional
    override fun deleteById(id: Long): Boolean {
        if (!jpa.existsById(id)) return false
        jpa.deleteById(id)
        return true
    }

    @Transactional(readOnly = true)
    override fun list(query: StarshipQuery): Page<Starship> {
        val orders = query.sort.map { s ->
            val property = when (s.field) {
                StarshipField.ID -> "id"
                StarshipField.NAME -> "name"
            }
            if (s.descending) Sort.Order.desc(property) else Sort.Order.asc(property)
        }.toMutableList()
        // Стабилизатор пагинации: без него порядок строк с равными ключами не гарантирован
        if (query.sort.none { it.field == StarshipField.ID }) orders += Sort.Order.asc("id")

        val result = jpa.findAll(specification(query.filter), PageRequest.of(query.page, query.size, Sort.by(orders)))
        return Page(
            items = result.content.map { it.toDomain() },
            page = query.page,
            size = query.size,
            totalElements = result.totalElements,
        )
    }

    /** Фильтры на точное равенство; без условий — выборка всей таблицы. */
    private fun specification(filter: StarshipFilter): Specification<StarshipEntity> =
        Specification { root, _, cb ->
            val predicates = listOfNotNull(
                filter.id?.let { cb.equal(root.get<Long>("id"), it) },
                filter.name?.let { cb.equal(root.get<String>("name"), it) },
            )
            if (predicates.isEmpty()) null else cb.and(*predicates.toTypedArray())
        }

    private fun StarshipEntity.toDomain(): Starship = Starship(
        id = requireNotNull(id) { "У сохранённого корабля обязан быть идентификатор" },
        name = name,
        marines = marines.toSet(),
    )

    private fun Starship.toEntity(): StarshipEntity = StarshipEntity(
        id = id,
        name = name,
        marines = marines.toMutableSet(),
    )
}
