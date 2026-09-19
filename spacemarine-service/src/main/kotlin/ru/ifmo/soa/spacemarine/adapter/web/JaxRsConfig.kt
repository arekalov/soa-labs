package ru.ifmo.soa.spacemarine.adapter.web

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import jakarta.ws.rs.ApplicationPath
import jakarta.ws.rs.core.Application
import jakarta.ws.rs.ext.ContextResolver
import jakarta.ws.rs.ext.Provider

/**
 * Корень JAX-RS.
 *
 * Путь «/» вместе с контекст-рутом «/» из `jboss-web.xml` даёт URL вида
 * `/space-marines`, ровно как в спецификации, где блок `servers` отсутствует,
 * а префикс зашит в `paths`.
 */
@ApplicationPath("/")
class SpaceMarineApplication : Application()

/** Единственный настроенный [ObjectMapper]: используется и JAX-RS, и разбором PATCH. */
object JsonMappers {

    val mapper: ObjectMapper = JsonMapper.builder()
        .addModule(KotlinModule.Builder().build())
        .addModule(JavaTimeModule())
        // Instant должен уходить как 2026-09-10T12:00:00Z, а не числом секунд.
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        // Неизвестные поля игнорируем сознательно: id и creationDate помечены readOnly,
        // поэтому клиент вправе вернуть их обратно в PUT-теле после GET. Падать 400-кой
        // на таком цикле «прочитал — поправил — отправил» было бы нарушением семантики readOnly.
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .build()
}

@Provider
class ObjectMapperContextResolver : ContextResolver<ObjectMapper> {
    override fun getContext(type: Class<*>?): ObjectMapper = JsonMappers.mapper
}
