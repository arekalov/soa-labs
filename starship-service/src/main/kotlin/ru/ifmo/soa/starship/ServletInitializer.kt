package ru.ifmo.soa.starship

import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer

/**
 * Точка входа при развёртывании WAR во внешний Tomcat.
 *
 * Задание требует именно контейнерного развёртывания, а не встроенного сервера,
 * поэтому `spring-boot-starter-tomcat` объявлен как `providedRuntime` и в WAR не попадает.
 */
class ServletInitializer : SpringBootServletInitializer() {
    override fun configure(application: SpringApplicationBuilder): SpringApplicationBuilder =
        application.sources(StarshipApplication::class.java)
}
