package ru.ifmo.soa.spacemarine.config

import jakarta.ws.rs.ApplicationPath
import jakarta.ws.rs.core.Application

/**
 * Корень JAX-RS.
 *
 * Путь «/» вместе с контекст-рутом «/» из `jboss-web.xml` даёт адреса вида
 * `/space-marines`: в спецификации нет блока `servers`, префикс зашит прямо в `paths`.
 */
@ApplicationPath("/")
class JaxRsApplication : Application()
