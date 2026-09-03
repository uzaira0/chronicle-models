package com.openlattice.chronicle.study

import com.openlattice.chronicle.collection.TestMappers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.OffsetDateTime

class StudyParticipantPolicyTest {

    private fun validPolicy(
        privacyPolicyUrl: String = "https://research.example.org/privacy/study-a",
        withdrawalUrl: String = "https://research.example.org/withdraw/study-a",
        consentDocumentUrl: String? = "https://research.example.org/consent/study-a.pdf",
    ): StudyParticipantPolicy = StudyParticipantPolicy(
        responsibleInstitution = "Example Research Institute",
        serverOperator = "Example Research Institute",
        researchContact = "study-team@example.org",
        purpose = "Understand how daily routines relate to health.",
        expectedDuration = "Twelve weeks",
        procedures = "The Chronicle app collects only the modules you approve.",
        foreseeableRisks = "Collection may reveal sensitive patterns about daily activity.",
        expectedBenefits = "There may be no direct benefit to participants.",
        dataUseAndSharing = "Approved researchers receive coded study data.",
        retentionAndDeletion = "Data is retained for seven years, then deleted.",
        privacyPolicyUrl = privacyPolicyUrl,
        withdrawalUrl = withdrawalUrl,
        consentDocumentUrl = consentDocumentUrl,
        version = "consent-2026-08-17",
        effectiveAt = OffsetDateTime.parse("2026-08-17T00:00:00Z"),
    )

    @Test
    fun `round-trips as a polymorphic study setting`() {
        val mapper = TestMappers.json()
        val policy: StudySetting = validPolicy()

        assertEquals(policy, mapper.readValue(mapper.writeValueAsString(policy), StudySetting::class.java))
    }

    @Test
    fun `round-trips in study settings under participant policy`() {
        val mapper = TestMappers.json()
        val settings = StudySettings(mapOf(StudySettingType.ParticipantPolicy to validPolicy()))

        val restored = mapper.readValue(mapper.writeValueAsString(settings), StudySettings::class.java)

        assertEquals(settings[StudySettingType.ParticipantPolicy], restored[StudySettingType.ParticipantPolicy])
    }

    @Test
    fun `rejects policy and withdrawal links that are not public https URLs`() {
        listOf(
            { validPolicy(privacyPolicyUrl = "http://research.example.org/privacy") },
            { validPolicy(withdrawalUrl = "file:///tmp/withdrawal") },
            { validPolicy(consentDocumentUrl = "https://user:password@research.example.org/consent.pdf") },
        ).forEach { constructInvalid ->
            assertThrows(IllegalArgumentException::class.java) { constructInvalid() }
        }
    }

    @Test
    fun `rejects missing informed-consent content`() {
        assertThrows(IllegalArgumentException::class.java) {
            validPolicy().copy(purpose = "")
        }
    }
}
