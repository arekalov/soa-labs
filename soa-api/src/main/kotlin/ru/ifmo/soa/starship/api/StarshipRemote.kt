package ru.ifmo.soa.starship.api

import jakarta.ejb.Remote
import ru.ifmo.soa.starship.dto.StarshipDto
import ru.ifmo.soa.starship.dto.StarshipPageDto
import ru.ifmo.soa.starship.query.StarshipQuery

@Remote
interface StarshipRemote {
    fun list(query: StarshipQuery): StarshipPageDto
    fun getById(id: Long): StarshipDto
    fun create(name: String?): StarshipDto
    fun createWithId(id: Long, name: String?): StarshipDto
    fun rename(id: Long, name: String?): StarshipDto
    fun delete(id: Long)
    fun board(starshipId: Long, spaceMarineId: Int): StarshipDto
    fun unload(starshipId: Long, spaceMarineId: Int): StarshipDto
}
