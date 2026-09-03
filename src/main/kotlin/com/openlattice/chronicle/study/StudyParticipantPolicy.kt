package com.openlattice.chronicle.study

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.net.URI
import java.time.OffsetDateTime

/**
 * Study-specific information a participant must review before Chronicle enrolls the device.
 *
 * This is distinct from the Chronicle publisher's platform privacy policy. The institution
 * operating a study owns this notice and must keep its public links available for participants.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public data class StudyParticipantPolicy(
    val responsibleInstitution: String,
    val serverOperator: String,
    val researchContact: String,
    val purpose: String,
    val expectedDuration: String,
    val procedures: String,
    val foreseeableRisks: String,
    val expectedBenefits: String,
    val dataUseAndSharing: String,
    val retentionAndDeletion: String,
    val privacyPolicyUrl: String,
    val withdrawalUrl: String,
    val consentDocumentUrl: String? = null,
    val version: String,
    val effectiveAt: OffsetDateTime,
) : StudySetting {

    init {
        requiredText("responsibleInstitution", responsibleInstitution)
        requiredText("serverOperator", serverOperator)
        requiredText("researchContact", researchContact)
        requiredText("purpose", purpose)
        requiredText("expectedDuration", expectedDuration)
        requiredText("procedures", procedures)
        requiredText("foreseeableRisks", foreseeableRisks)
        requiredText("expectedBenefits", expectedBenefits)
        requiredText("dataUseAndSharing", dataUseAndSharing)
        requiredText("retentionAndDeletion", retentionAndDeletion)
        requiredText("version", version, MAX_VERSION_LENGTH)
        publicHttpsUrl("privacyPolicyUrl", privacyPolicyUrl)
        publicHttpsUrl("withdrawalUrl", withdrawalUrl)
        consentDocumentUrl?.let { publicHttpsUrl("consentDocumentUrl", it) }
    }

    /** Re-runs constructor invariants when callers need an explicit validation boundary. */
    public fun validate(): StudyParticipantPolicy = copy()

    private companion object {
        const val MAX_TEXT_LENGTH: Int = 8_000
        const val MAX_VERSION_LENGTH: Int = 128
        const val MAX_URL_LENGTH: Int = 2_048

        fun requiredText(name: String, value: String, maxLength: Int = MAX_TEXT_LENGTH) {
            require(value.isNotBlank()) { "$name must not be blank" }
            require(value.length <= maxLength) { "$name must be at most $maxLength characters" }
        }

        fun publicHttpsUrl(name: String, value: String) {
            require(value.length <= MAX_URL_LENGTH) { "$name must be at most $MAX_URL_LENGTH characters" }
            val uri = runCatching { URI(value) }.getOrNull()
            require(
                uri != null &&
                    uri.isAbsolute &&
                    uri.scheme.equals("https", ignoreCase = true) &&
                    !uri.host.isNullOrBlank() &&
                    uri.userInfo == null
            ) {
                "$name must be an absolute HTTPS URL without embedded credentials"
            }
        }
    }
}
