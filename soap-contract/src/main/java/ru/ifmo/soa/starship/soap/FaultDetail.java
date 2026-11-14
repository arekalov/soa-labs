package ru.ifmo.soa.starship.soap;

import jakarta.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "FaultDetail")
public class FaultDetail {
    public int status;
    public String code;
    public String message;
    public java.util.List<String> details = new java.util.ArrayList<>();
}
