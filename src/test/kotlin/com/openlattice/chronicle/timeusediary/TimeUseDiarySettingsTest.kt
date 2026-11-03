package com.openlattice.chronicle.timeusediary

import com.openlattice.chronicle.collection.TestMappers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/** Current diary settings preserve explicit choices and default omitted optional clock fields. */
class TimeUseDiarySettingsTest {

    private val mapper = TestMappers.json()

    @Test fun defaultsMatchUpstreamParity() {
        val settings = TimeUseDiarySettings()
        assertEquals(12, settings.clockFormat)
        assertFalse(settings.clockFormatLocked)
        assertEquals("en", settings.language)
        assertFalse(settings.enableChangesForSherbrookeUniversity)
        assertFalse(settings.enableChangesForOhioStateUniversity)
    }

    @Test fun roundTripPreservesClockFormatFields() {
        val settings = TimeUseDiarySettings(
            language = "he",
            clockFormat = 24,
            clockFormatLocked = true,
        )
        val json = mapper.writeValueAsString(settings)
        val restored = mapper.readValue(json, TimeUseDiarySettings::class.java)
        assertEquals(settings, restored)
        assertEquals(24, restored.clockFormat)
        assertEquals(true, restored.clockFormatLocked)
    }

    @Test fun jsonWithoutClockFormatDefaultsTo12Unlocked() {
        val settingsJson = """
            {
              "@class": "com.openlattice.chronicle.timeusediary.TimeUseDiarySettings",
              "enableChangesForSherbrookeUniversity": true,
              "enableChangesForOhioStateUniversity": false,
              "language": "fr"
            }
        """.trimIndent()
        val restored = mapper.readValue(settingsJson, TimeUseDiarySettings::class.java)
        assertEquals(12, restored.clockFormat)
        assertFalse(restored.clockFormatLocked)
        assertEquals("fr", restored.language)
        assertEquals(true, restored.enableChangesForSherbrookeUniversity)
    }
}
