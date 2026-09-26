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
import ru.ifmo.soa.spacemarine.api.AstartesCategory
import java.time.Instant

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

    @Column(name = "creation_date", nullable = false, updatable = false)
    var creationDate: Instant = Instant.EPOCH,

    @Column(name = "health", nullable = false)
    var health: Float = 0f,

    @Column(name = "loyal", nullable = false)
    var loyal: Boolean = false,

    @Column(name = "achievements")
    var achievements: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 16)
    var category: AstartesCategory = AstartesCategory.SCOUT,

    @Embedded
    var chapter: Chapter? = null,
)
