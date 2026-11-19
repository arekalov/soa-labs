package ru.ifmo.soa.starship.soap

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlElement
import jakarta.xml.bind.annotation.XmlType

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ShipSort", namespace = StarshipPort.NS, propOrder = ["field", "descending"])
// open сохраняет прежний XSD-контракт без ограничения final="extension restriction".
open class ShipSort {
    @field:XmlElement(namespace = StarshipPort.NS)
    var field: SortField? = null

    @field:XmlElement(namespace = StarshipPort.NS)
    var descending: Boolean = false
}
