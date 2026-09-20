package ru.ifmo.soa.spacemarine.config

import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerRequestFilter
import jakarta.ws.rs.container.PreMatching
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.Provider

/**
 * Ответ на предварительный запрос OPTIONS.
 *
 * `@PreMatching` обязателен: метод OPTIONS не объявляет ни один контроллер, и без перехвата
 * до сопоставления маршрута запрос завершился бы кодом 405, а браузер счёл бы основной
 * запрос запрещённым.
 */
@Provider
@PreMatching
class CorsPreflightFilter : ContainerRequestFilter {

    override fun filter(requestContext: ContainerRequestContext) {
        if (requestContext.method == "OPTIONS") {
            requestContext.abortWith(Response.ok().build())
        }
    }
}
