package ru.ifmo.soa.starship.soap

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlElement
import jakarta.xml.bind.annotation.XmlType

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ShipQuery", namespace = StarshipPort.NS, propOrder = ["id", "name", "sort", "page", "size"])
// open сохраняет прежний XSD-контракт без ограничения final="extension restriction".
open class ShipQuery {
    @field:XmlElement(namespace = StarshipPort.NS)
    var id: Long? = null

    @field:XmlElement(namespace = StarshipPort.NS)
    var name: String? = null

    @field:XmlElement(namespace = StarshipPort.NS, nillable = true)
    var sort: List<ShipSort?> = mutableListOf()

    @field:XmlElement(namespace = StarshipPort.NS)
    var page: Int = 0

    @field:XmlElement(namespace = StarshipPort.NS)
    var size: Int = 20
}
