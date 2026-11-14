package ru.ifmo.soa.starship.soap;

import jakarta.jws.*;
import jakarta.jws.soap.SOAPBinding;

@WebService(name = "StarshipPort", targetNamespace = StarshipPort.NS)
@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.WRAPPED)
public interface StarshipPort {
    String NS = "urn:soa:starship:v1";
    @WebMethod(operationName = "list")
    @WebResult(name = "result")
    ShipPage list(@WebParam(name = "query") ShipQuery query) throws StarshipFault;

    @WebMethod(operationName = "getById")
    @WebResult(name = "result")
    Ship getById(@WebParam(name = "id") long id) throws StarshipFault;

    @WebMethod(operationName = "create")
    @WebResult(name = "result")
    Ship create(@WebParam(name = "name") String name) throws StarshipFault;

    @WebMethod(operationName = "createWithId")
    @WebResult(name = "result")
    Ship createWithId(@WebParam(name = "id") long id, @WebParam(name = "name") String name) throws StarshipFault;

    @WebMethod(operationName = "rename")
    @WebResult(name = "result")
    Ship rename(@WebParam(name = "id") long id, @WebParam(name = "name") String name) throws StarshipFault;

    @WebMethod(operationName = "delete")
    void delete(@WebParam(name = "id") long id) throws StarshipFault;

    @WebMethod(operationName = "board")
    @WebResult(name = "result")
    Ship board(@WebParam(name = "starshipId") long starshipId, @WebParam(name = "spaceMarineId") int spaceMarineId) throws StarshipFault;

    @WebMethod(operationName = "unload")
    @WebResult(name = "result")
    Ship unload(@WebParam(name = "starshipId") long starshipId, @WebParam(name = "spaceMarineId") int spaceMarineId) throws StarshipFault;

}
