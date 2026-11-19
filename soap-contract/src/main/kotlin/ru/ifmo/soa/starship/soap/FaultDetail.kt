package ru.ifmo.soa.starship.soap

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlElement
import jakarta.xml.bind.annotation.XmlType

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "FaultDetail", namespace = StarshipPort.NS, propOrder = ["status", "code", "message", "details"])
// open сохраняет прежний XSD-контракт без ограничения final="extension restriction".
open class FaultDetail {
    @field:XmlElement(namespace = StarshipPort.NS)
    var status: Int = 0

    @field:XmlElement(namespace = StarshipPort.NS)
    var code: String? = null

    @field:XmlElement(namespace = StarshipPort.NS)
    var message: String? = null

    @field:XmlElement(namespace = StarshipPort.NS, nillable = true)
    var details: List<String> = mutableListOf()
}
