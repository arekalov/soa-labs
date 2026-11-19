package ru.ifmo.soa.starship.soap

import jakarta.jws.WebMethod
import jakarta.jws.WebParam
import jakarta.jws.WebResult
import jakarta.jws.WebService
import jakarta.jws.soap.SOAPBinding

@WebService(name = "StarshipPort", targetNamespace = StarshipPort.NS)
@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.WRAPPED)
interface StarshipPort {
    companion object {
        const val NS = "urn:soa:starship:v1"
    }

    @WebMethod(operationName = "list")
    @WebResult(name = "result", targetNamespace = NS)
    @Throws(StarshipFault::class)
    fun list(@WebParam(name = "query", targetNamespace = NS) query: ShipQuery?): ShipPage

    @WebMethod(operationName = "getById")
    @WebResult(name = "result", targetNamespace = NS)
    @Throws(StarshipFault::class)
    fun getById(@WebParam(name = "id", targetNamespace = NS) id: Long): Ship

    @WebMethod(operationName = "create")
    @WebResult(name = "result", targetNamespace = NS)
    @Throws(StarshipFault::class)
    fun create(@WebParam(name = "name", targetNamespace = NS) name: String?): Ship

    @WebMethod(operationName = "createWithId")
    @WebResult(name = "result", targetNamespace = NS)
    @Throws(StarshipFault::class)
    fun createWithId(
        @WebParam(name = "id", targetNamespace = NS) id: Long,
        @WebParam(name = "name", targetNamespace = NS) name: String?,
    ): Ship

    @WebMethod(operationName = "rename")
    @WebResult(name = "result", targetNamespace = NS)
    @Throws(StarshipFault::class)
    fun rename(
        @WebParam(name = "id", targetNamespace = NS) id: Long,
        @WebParam(name = "name", targetNamespace = NS) name: String?,
    ): Ship

    @WebMethod(operationName = "delete")
    @Throws(StarshipFault::class)
    fun delete(@WebParam(name = "id", targetNamespace = NS) id: Long): Unit

    @WebMethod(operationName = "board")
    @WebResult(name = "result", targetNamespace = NS)
    @Throws(StarshipFault::class)
    fun board(
        @WebParam(name = "starshipId", targetNamespace = NS) starshipId: Long,
        @WebParam(name = "spaceMarineId", targetNamespace = NS) spaceMarineId: Int,
    ): Ship

    @WebMethod(operationName = "unload")
    @WebResult(name = "result", targetNamespace = NS)
    @Throws(StarshipFault::class)
    fun unload(
        @WebParam(name = "starshipId", targetNamespace = NS) starshipId: Long,
        @WebParam(name = "spaceMarineId", targetNamespace = NS) spaceMarineId: Int,
    ): Ship
}
