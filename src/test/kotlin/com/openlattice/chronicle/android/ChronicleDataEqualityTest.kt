package com.openlattice.chronicle.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.time.OffsetDateTime
import java.util.UUID

/**
 * Equality previously walked only the right-hand operand, so a non-empty instance reported
 * itself equal to an empty one and the reverse comparison indexed past the shorter list.
 */
class ChronicleDataEqualityTest {

    private val sample = ChronicleUsageEvent(
        studyId = UUID.fromString("00000000-0000-4000-8000-000000000001"),
        participantId = "p1",
        appPackageName = "com.example.app",
        interactionType = "Activity Resumed",
        timestamp = OffsetDateTime.parse("2026-01-01T00:00:00Z"),
        timezone = "UTC",
        user = "u",
        applicationLabel = "Example",
    )

    @Test
    fun aNonEmptyInstanceIsNotEqualToAnEmptyOne() {
        assertNotEquals(ChronicleData(listOf(sample)), ChronicleData(listOf()))
    }

    @Test
    fun anEmptyInstanceIsNotEqualToANonEmptyOne() {
        assertNotEquals(ChronicleData(listOf()), ChronicleData(listOf(sample)))
    }

    @Test
    fun equalContentsRemainEqual() {
        assertEquals(ChronicleData(listOf(sample)), ChronicleData(listOf(sample)))
    }
}
