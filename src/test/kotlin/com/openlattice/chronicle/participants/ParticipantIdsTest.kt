package com.openlattice.chronicle.participants

import org.junit.Assert.assertEquals
import org.junit.Test

class ParticipantIdsTest {
    @Test fun rejectsDotSegmentsAndPreservesOtherParticipantIds() {
        val cases = mapOf(
            "." to false, ".." to false, "..." to true, "a.b" to true, ".a" to true,
            "p" to true, "P-007" to true, "site_1.subject-2" to true, "x".repeat(255) to true,
            "" to false, "x".repeat(256) to false, "has space" to false, "a/b" to false,
            "ñ" to false, "p\n" to false, "p;drop" to false,
        )
        for ((id, valid) in cases) assertEquals(id, valid, ParticipantIds.isValid(id))
    }
}
