package ru.ifmo.soa.starship.soap

import jakarta.xml.ws.WebFault

@WebFault(name = "StarshipFault", targetNamespace = StarshipPort.NS)
class StarshipFault : Exception {
    val faultInfo: FaultDetail

    constructor(message: String?, faultInfo: FaultDetail) : super(message) {
        this.faultInfo = faultInfo
    }

    constructor(message: String?, faultInfo: FaultDetail, cause: Throwable?) : super(message, cause) {
        this.faultInfo = faultInfo
    }
}
