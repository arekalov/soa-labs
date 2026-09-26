package ru.ifmo.soa.spacemarine.model

import jakarta.persistence.Column
import jakarta.persistence.Embeddable

@Embeddable
class Coordinates(
    @Column(name = "coordinates_x", nullable = false)
    var x: Int = 0,

    @Column(name = "coordinates_y", nullable = false)
    var y: Double = 0.0,
)
