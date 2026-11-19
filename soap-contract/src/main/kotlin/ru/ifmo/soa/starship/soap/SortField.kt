package ru.ifmo.soa.starship.soap

import jakarta.xml.bind.annotation.XmlEnum
import jakarta.xml.bind.annotation.XmlType

@XmlEnum
@XmlType(name = "sortField", namespace = StarshipPort.NS)
enum class SortField { ID, NAME }
