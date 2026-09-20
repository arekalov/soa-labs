package ru.ifmo.soa.starship

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class StarshipApplication

fun main(args: Array<String>) {
    runApplication<StarshipApplication>(*args)
}
