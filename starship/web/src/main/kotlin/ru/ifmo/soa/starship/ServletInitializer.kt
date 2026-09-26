package ru.ifmo.soa.starship

import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer

/**
 * Точка входа при развёртывании WAR во внешний Tomcat.
 */
class ServletInitializer : SpringBootServletInitializer() {
    override fun configure(application: SpringApplicationBuilder): SpringApplicationBuilder =
        application.sources(StarshipApplication::class.java)
}
