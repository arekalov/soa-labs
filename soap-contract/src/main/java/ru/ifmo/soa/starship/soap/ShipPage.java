package ru.ifmo.soa.starship.soap;

import jakarta.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ShipPage")
public class ShipPage {
    public java.util.List<Ship> items = new java.util.ArrayList<>();
    public int page;
    public int size;
    public long totalElements;
    public int totalPages;
}
