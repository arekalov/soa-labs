package ru.ifmo.soa.starship.adapter.persistence

import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.Table
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import ru.ifmo.soa.starship.application.error.StarshipAlreadyExistsException
import ru.ifmo.soa.starship.application.port.StarshipRepository
import ru.ifmo.soa.starship.domain.model.Starship

@Entity
@Table(name = "starship")
class StarshipEntity(

    /** Без @GeneratedValue: идентификатор приходит из URL, так требует спецификация. */
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

interface SpringDataStarshipRepository : JpaRepository<StarshipEntity, Long>

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

    @Transactional(readOnly = true)
    override fun findById(id: Long): Starship? = jpa.findById(id).orElse(null)?.toDomain()

    @Transactional(readOnly = true)
    override fun existsById(id: Long): Boolean = jpa.existsById(id)

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
