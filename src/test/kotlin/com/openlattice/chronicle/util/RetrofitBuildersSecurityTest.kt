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
}
