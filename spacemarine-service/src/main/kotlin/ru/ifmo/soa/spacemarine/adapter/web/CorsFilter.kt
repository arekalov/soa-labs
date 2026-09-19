package ru.ifmo.soa.spacemarine.adapter.web

import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerRequestFilter
import jakarta.ws.rs.container.ContainerResponseContext
import jakarta.ws.rs.container.ContainerResponseFilter
import jakarta.ws.rs.container.PreMatching
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.Provider

/**
 * Разрешение кросс-доменных запросов.
 *
 * Клиентское приложение — статика, отдаваемая nginx с другого адреса, а код выполняется
 * в браузере. Без этих заголовков браузер отсечёт любой запрос к сервису ещё до отправки.
 *
 * Спецификация CORS не описывает, и на контракт API он не влияет: заголовки не меняют
 * ни путей, ни кодов ответов, ни схем.
 */
@Provider
class CorsResponseFilter : ContainerResponseFilter {

    override fun filter(request: ContainerRequestContext, response: ContainerResponseContext) {
        with(response.headers) {
            putSingle(ALLOW_ORIGIN, "*")
            putSingle(ALLOW_METHODS, "GET, POST, PUT, PATCH, DELETE, OPTIONS")
            putSingle(ALLOW_HEADERS, "Content-Type, Accept")
            putSingle(MAX_AGE, "3600")
        }
    }

    companion object {
        const val ALLOW_ORIGIN = "Access-Control-Allow-Origin"
        const val ALLOW_METHODS = "Access-Control-Allow-Methods"
        const val ALLOW_HEADERS = "Access-Control-Allow-Headers"
        const val MAX_AGE = "Access-Control-Max-Age"
    }
}

/**
 * Ответ на предварительный запрос OPTIONS.
 *
 * Фильтр помечен `@PreMatching`, потому что ни один ресурс метод OPTIONS не объявляет:
 * без перехвата до сопоставления маршрута запрос завершился бы ошибкой 405,
 * и браузер счёл бы основной запрос запрещённым.
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
