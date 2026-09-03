package com.openlattice.chronicle.collection

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.dataformat.smile.SmileFactory
import com.fasterxml.jackson.datatype.guava.GuavaModule
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule

/**
 * Test ObjectMapper factory for chronicle-models.
 *
 * Mirrors the module set used by chronicle-api's `AbstractJacksonSerializationTest`
 * (Kotlin + JavaTime + Guava) but is defined locally so chronicle-models tests do
 * not depend on chronicle-api — that would create a build cycle.
 */
internal object TestMappers {

    fun json(): ObjectMapper = configure(ObjectMapper())

    fun smile(): ObjectMapper = configure(ObjectMapper(SmileFactory()))

    private fun configure(mapper: ObjectMapper): ObjectMapper {
        mapper.registerModule(JavaTimeModule())
        mapper.registerModule(GuavaModule())
        mapper.registerModule(KotlinModule.Builder().build())
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        mapper.disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
        return mapper
    }
}
