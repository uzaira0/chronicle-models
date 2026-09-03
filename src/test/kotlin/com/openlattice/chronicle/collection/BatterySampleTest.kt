package com.openlattice.chronicle.collection

import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * Model unit tests for the [BatterySample] wire contract
 * (see `docs/SENSING-EXPANSION-DESIGN.md` §5).
 */
class BatterySampleTest {

    private fun sample(
        levelPercent: Int = 80,
        voltageMillivolts: Int = 4100,
    ): BatterySample = BatterySample(
        id = "battery-sample-1",
        timestamp = OffsetDateTime.of(2026, 5, 21, 12, 0, 0, 0, ZoneOffset.UTC),
        timezone = "UTC",
        levelPercent = levelPercent,
        chargingState = BatteryChargingState.DISCHARGING,
        plugType = BatteryPlugType.UNPLUGGED,
        temperatureDeciC = 312,
        voltageMillivolts = voltageMillivolts,
        health = BatteryHealth.GOOD,
    )

    @Test fun testValidSampleConstructs() {
        assertEquals(80, sample().levelPercent)
    }

    @Test fun testNegativeTemperatureIsAllowed() {
        assertEquals(-15, sample().copy(temperatureDeciC = -15).temperatureDeciC)
    }

    @Test fun testLevelPercentAboveRangeRejected() {
        try {
            sample(levelPercent = 101)
            fail("Expected rejection of out-of-range levelPercent")
        } catch (e: IllegalArgumentException) { /* expected */ }
    }

    @Test fun testLevelPercentBelowRangeRejected() {
        try {
            sample(levelPercent = -1)
            fail("Expected rejection of negative levelPercent")
        } catch (e: IllegalArgumentException) { /* expected */ }
    }

    @Test fun testNegativeVoltageRejected() {
        try {
            sample(voltageMillivolts = -1)
            fail("Expected rejection of negative voltage")
        } catch (e: IllegalArgumentException) { /* expected */ }
    }

    @Test fun testEnumCardinalities() {
        assertEquals(5, BatteryChargingState.entries.size)
        assertEquals(5, BatteryPlugType.entries.size)
        assertEquals(7, BatteryHealth.entries.size)
    }
}
