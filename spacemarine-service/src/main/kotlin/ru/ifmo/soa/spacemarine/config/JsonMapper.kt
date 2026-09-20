package ru.ifmo.soa.spacemarine.config

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.cfg.CoercionAction
import com.fasterxml.jackson.databind.cfg.CoercionInputShape
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.databind.type.LogicalType
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule

/** Единственный настроенный маппер: его отдаёт JAX-RS, им же накладывается тело PATCH. */
val jsonMapper: ObjectMapper = JsonMapper.builder()
    .addModule(KotlinModule.Builder().build())
    .addModule(JavaTimeModule())
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    // id и creationDate помечены readOnly, поэтому клиент вправе вернуть их в теле после GET.
    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    // Иначе Jackson молча превратил бы {"name": 42} в имя "42" вместо ответа 400.
    .withCoercionConfig(LogicalType.Textual) { config ->
        config.setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
        config.setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
        config.setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail)
    }
    .build()
