package ru.ifmo.soa.starship.model

import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.Table

@Entity
@Table(name = "starship")
class Starship(

    /**
     * Без `@GeneratedValue`: идентификатор известен до сохранения — либо пришёл из URL
     * (эндпоинт спецификации ЛР1), либо взят из последовательности заранее.
     */
    @Id
    @Column(name = "id", nullable = false)
    var id: Long? = null,

    @Column(name = "name", nullable = false)
    var name: String = "",

    /**
     * Идентификаторы десантников из первого сервиса.
     *
     * Внешнего ключа на их таблицу нет и быть не может: это другой сервис со своей базой.
     * Целостность поддерживается вызовом REST, а не СУБД.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "starship_marine", joinColumns = [JoinColumn(name = "starship_id")])
    @Column(name = "space_marine_id", nullable = false)
    var marines: MutableSet<Int> = mutableSetOf(),
) {
    fun hasOnBoard(spaceMarineId: Int): Boolean = spaceMarineId in marines
}
