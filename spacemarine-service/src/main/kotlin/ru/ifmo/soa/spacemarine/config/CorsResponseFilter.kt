package ru.ifmo.soa.spacemarine.config

import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerResponseContext
import jakarta.ws.rs.container.ContainerResponseFilter
import jakarta.ws.rs.ext.Provider

private const val ALLOW_ORIGIN = "Access-Control-Allow-Origin"
private const val ALLOW_METHODS = "Access-Control-Allow-Methods"
private const val ALLOW_HEADERS = "Access-Control-Allow-Headers"
private const val MAX_AGE = "Access-Control-Max-Age"
private const val ALLOW_PRIVATE_NETWORK = "Access-Control-Allow-Private-Network"

/**
 * Клиент — статика с другого адреса, поэтому без этих заголовков браузер отсечёт
 * любой запрос к сервису. На контракт API они не влияют.
 */
@Provider
class CorsResponseFilter : ContainerResponseFilter {
    override fun filter(request: ContainerRequestContext, response: ContainerResponseContext) {
        with(response.headers) {
            putSingle(ALLOW_ORIGIN, "*")
            putSingle(ALLOW_METHODS, "GET, POST, PUT, PATCH, DELETE, OPTIONS")
            putSingle(ALLOW_HEADERS, "Content-Type, Accept")
            putSingle(MAX_AGE, "3600")
            putSingle(ALLOW_PRIVATE_NETWORK, "true")
        }
    }
}
