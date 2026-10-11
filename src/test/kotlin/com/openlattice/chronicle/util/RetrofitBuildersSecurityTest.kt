package com.openlattice.chronicle.util

import com.fasterxml.jackson.databind.MapperFeature
import org.junit.Assert.assertFalse
import org.junit.Test

class RetrofitBuildersSecurityTest {

    @Test
    fun mapperRejectsCaseInsensitiveProperties() {
        @Suppress("DEPRECATION")
        assertFalse(RetrofitBuilders.mapper.isEnabled(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES))
    }
    @Test
    fun timestampKeepsItsOffsetAndLocalCalendarDate() {
        val timestamp = RetrofitBuilders.mapper.readValue("\"2026-10-07T23:45:00-05:00\"", java.time.OffsetDateTime::class.java)
        org.junit.Assert.assertEquals(java.time.ZoneOffset.ofHours(-5), timestamp.offset)
        org.junit.Assert.assertEquals(java.time.LocalDate.of(2026, 10, 7), timestamp.toLocalDate())
    }
}
