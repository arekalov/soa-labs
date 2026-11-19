package ru.ifmo.soa.starship.soap

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlElement
import jakarta.xml.bind.annotation.XmlType

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ShipPage", namespace = StarshipPort.NS, propOrder = ["items", "page", "size", "totalElements", "totalPages"])
// open сохраняет прежний XSD-контракт без ограничения final="extension restriction".
open class ShipPage {
    @field:XmlElement(namespace = StarshipPort.NS, nillable = true)
    var items: List<Ship> = mutableListOf()

    @field:XmlElement(namespace = StarshipPort.NS)
    var page: Int = 0

    @field:XmlElement(namespace = StarshipPort.NS)
    var size: Int = 0

    @field:XmlElement(namespace = StarshipPort.NS)
    var totalElements: Long = 0

    @field:XmlElement(namespace = StarshipPort.NS)
    var totalPages: Int = 0
}
