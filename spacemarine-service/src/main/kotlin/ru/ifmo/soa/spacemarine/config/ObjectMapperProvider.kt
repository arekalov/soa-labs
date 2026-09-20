package ru.ifmo.soa.spacemarine.config

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import jakarta.ws.rs.ext.ContextResolver
import jakarta.ws.rs.ext.Provider

@Provider
class ObjectMapperProvider : ContextResolver<ObjectMapper> {

    private val mapper: ObjectMapper = JsonMapper.builder()
        .addModule(KotlinModule.Builder().build())
        .addModule(JavaTimeModule())
        // Instant должен уходить как 2026-09-10T12:00:00Z, а не числом секунд.
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        // Неизвестные поля игнорируем сознательно: id и creationDate помечены readOnly,
        // поэтому клиент вправе вернуть их обратно в теле PUT после GET.
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .build()

    override fun getContext(type: Class<*>?): ObjectMapper = mapper
}
