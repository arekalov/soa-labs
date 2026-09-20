package ru.ifmo.soa.starship.repository

import org.springframework.data.jpa.domain.Specification
import ru.ifmo.soa.starship.model.Starship
import ru.ifmo.soa.starship.query.StarshipFilter

/** Сборка условий выборки из фильтров запроса. */
object StarshipSpecifications {
    fun of(filter: StarshipFilter): Specification<Starship> = Specification { root, _, cb ->
        val predicates = listOfNotNull(
            filter.id?.let { cb.equal(root.get<Long>("id"), it) },
            filter.name?.let { cb.equal(root.get<String>("name"), it) },
        )
        if (predicates.isEmpty()) null else cb.and(*predicates.toTypedArray())
    }
}
