package ru.ifmo.soa.starship.adapter.web

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/**
 * Разрешение кросс-доменных запросов от браузерного клиента.
 *
 * Клиент отдаётся nginx с другого адреса, поэтому без этих заголовков браузер
 * отсечёт запросы ещё до отправки. На контракт API это не влияет: ни пути,
 * ни коды ответов, ни схемы не меняются.
 */
@Configuration
class CorsConfiguration : WebMvcConfigurer {

    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/**")
            .allowedOriginPatterns("*")
            .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            .allowedHeaders("Content-Type", "Accept")
            .maxAge(3600)
    }
}
