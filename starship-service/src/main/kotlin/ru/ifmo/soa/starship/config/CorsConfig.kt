package ru.ifmo.soa.starship.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

private const val PREFLIGHT_MAX_AGE = 3600L

/**
 * Клиент отдаётся nginx с другого адреса, поэтому без этих заголовков браузер отсечёт
 * запросы ещё до отправки. На контракт API это не влияет.
 */
@Configuration
class CorsConfig : WebMvcConfigurer {

    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/**")
            .allowedOriginPatterns("*")
            .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            .allowedHeaders("Content-Type", "Accept")
            // Chromium: страница с публичного сайта обращается к localhost через SSH-туннель
            // и требует Access-Control-Allow-Private-Network на preflight.
            .allowPrivateNetwork(true)
            .maxAge(PREFLIGHT_MAX_AGE)
    }
}
