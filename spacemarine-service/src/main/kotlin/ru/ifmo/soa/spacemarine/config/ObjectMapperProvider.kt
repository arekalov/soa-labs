package ru.ifmo.soa.spacemarine.config

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.ws.rs.ext.ContextResolver
import jakarta.ws.rs.ext.Provider

@Provider
class ObjectMapperProvider : ContextResolver<ObjectMapper> {
    override fun getContext(type: Class<*>?): ObjectMapper = jsonMapper
}
