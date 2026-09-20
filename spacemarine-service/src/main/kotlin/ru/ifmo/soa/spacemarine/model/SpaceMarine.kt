package ru.ifmo.soa.spacemarine.model

import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.time.temporal.ChronoUnit

/** Верхняя граница `coordinates.y`, включительно. */
const val MAX_COORDINATE_Y = 12.0

/**
 * Postgres хранит `timestamp` с микросекундами, а клиент присылает миллисекунды из ISO-8601.
 * Без среза фильтр по точному равенству `creationDate` не совпал бы никогда.
 */
val TIME_PRECISION: ChronoUnit = ChronoUnit.MILLIS

fun nowTruncated(): Instant = Instant.now().truncatedTo(TIME_PRECISION)

@Entity
@Table(name = "space_marine")
class SpaceMarine(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    var id: Int? = null,

    @Column(name = "name", nullable = false)
    var name: String = "",

    @Embedded
    var coordinates: Coordinates = Coordinates(),

    /** Поле `readOnly` по спецификации, поэтому колонка не обновляется. */
    @Column(name = "creation_date", nullable = false, updatable = false)
    var creationDate: Instant = Instant.EPOCH,

    @Column(name = "health", nullable = false)
    var health: Float = 0f,

    @Column(name = "loyal", nullable = false)
    var loyal: Boolean = false,

    @Column(name = "achievements")
    var achievements: String? = null,

    /**
     * Строкой, а не порядковым номером: иначе таблица нечитаема, а вставка значения
     * в середину перечисления тихо переразметит существующие строки.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 16)
    var category: AstartesCategory = AstartesCategory.SCOUT,

    @Embedded
    var chapter: Chapter? = null,
)
