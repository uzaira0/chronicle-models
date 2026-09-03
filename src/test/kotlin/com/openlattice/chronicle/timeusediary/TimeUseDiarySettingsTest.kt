package com.openlattice.chronicle.timeusediary

import com.openlattice.chronicle.collection.TestMappers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * Pins the Time Use Diary settings contract, including the upstream-parity fields
 * `clockFormat` / `clockFormatLocked` (methodic-labs). The backward-compatibility
 * test is the important one: settings persisted before these fields existed must
 * still deserialize, defaulting to a 12-hour, unlocked clock — otherwise enabling
 * parity would break every study's stored TUD settings.
 */
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

    @Test fun legacyJsonWithoutClockFormatDefaultsTo12Unlocked() {
        // A settings blob written before the parity fields existed.
        val legacyJson = """
            {
              "@class": "com.openlattice.chronicle.timeusediary.TimeUseDiarySettings",
              "enableChangesForSherbrookeUniversity": true,
              "enableChangesForOhioStateUniversity": false,
              "language": "fr"
            }
        """.trimIndent()
        val restored = mapper.readValue(legacyJson, TimeUseDiarySettings::class.java)
        assertEquals(12, restored.clockFormat)
        assertFalse(restored.clockFormatLocked)
        assertEquals("fr", restored.language)
        assertEquals(true, restored.enableChangesForSherbrookeUniversity)
    }
}
