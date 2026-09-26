package ru.ifmo.soa.starship

import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer

/**
 * Точка входа при развёртывании WAR в контейнер сервера приложений.
 */
class ServletInitializer : SpringBootServletInitializer() {
    override fun configure(application: SpringApplicationBuilder): SpringApplicationBuilder =
        application.sources(StarshipApplication::class.java)
}
