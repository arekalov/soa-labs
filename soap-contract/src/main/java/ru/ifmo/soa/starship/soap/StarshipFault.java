package ru.ifmo.soa.starship.soap;

@jakarta.xml.ws.WebFault(name = "StarshipFault", targetNamespace = StarshipPort.NS)
public class StarshipFault extends Exception {
    private final FaultDetail faultInfo;
    public StarshipFault(String message, FaultDetail faultInfo) { super(message); this.faultInfo = faultInfo; }
    public StarshipFault(String message, FaultDetail faultInfo, Throwable cause) { super(message, cause); this.faultInfo = faultInfo; }
    public FaultDetail getFaultInfo() { return faultInfo; }
}
