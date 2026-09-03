package com.openlattice.chronicle.participantaccess

import org.junit.Assert.assertEquals
import org.junit.Test

class ParticipantFormAccessModelsTest {
    @Test
    fun enrollmentIsAStableParticipantAccessPurpose() {
        assertEquals("ENROLLMENT", ParticipantFormKind.ENROLLMENT.name)
    }
}
