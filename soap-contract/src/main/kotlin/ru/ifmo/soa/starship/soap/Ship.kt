package ru.ifmo.soa.starship.soap

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlElement
import jakarta.xml.bind.annotation.XmlType

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Ship", namespace = StarshipPort.NS, propOrder = ["id", "name", "marines"])
// open сохраняет прежний XSD-контракт без ограничения final="extension restriction".
open class Ship {
    @field:XmlElement(namespace = StarshipPort.NS)
    var id: Long = 0

    @field:XmlElement(namespace = StarshipPort.NS)
    var name: String? = null

    @field:XmlElement(namespace = StarshipPort.NS, nillable = true)
    // JAXB заполняет коллекцию при чтении XML: начальный список должен быть изменяемым.
    var marines: List<Int> = mutableListOf()
}
