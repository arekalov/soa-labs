package ru.ifmo.soa.starship.soap;

import jakarta.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ShipSort")
public class ShipSort {
    public SortField field;
    public boolean descending;
}
