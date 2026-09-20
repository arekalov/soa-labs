package ru.ifmo.soa.spacemarine.model

import jakarta.persistence.Column
import jakarta.persistence.Embeddable

@Embeddable
class Chapter(
    @Column(name = "chapter_name")
    var name: String? = null,

    @Column(name = "chapter_parent_legion")
    var parentLegion: String? = null,
)
