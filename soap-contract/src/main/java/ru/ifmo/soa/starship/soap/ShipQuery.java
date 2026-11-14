package ru.ifmo.soa.starship.soap;

import jakarta.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ShipQuery")
public class ShipQuery {
    public Long id;
    public String name;
    public java.util.List<ShipSort> sort = new java.util.ArrayList<>();
    public int page;
    public int size = 20;
}
