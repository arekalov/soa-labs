package ru.ifmo.soa.starship.soap;

import jakarta.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Ship")
public class Ship {
    public long id;
    public String name;
    public java.util.List<Integer> marines = new java.util.ArrayList<>();
}
