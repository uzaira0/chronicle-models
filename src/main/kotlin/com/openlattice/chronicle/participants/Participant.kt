package com.openlattice.chronicle.participants

import com.fasterxml.jackson.annotation.JsonProperty
import com.openlattice.chronicle.candidates.Candidate
import com.openlattice.chronicle.collection.ParticipantConsentState
import com.openlattice.chronicle.data.ParticipationStatus
import com.openlattice.chronicle.util.JsonFields
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

/**
 *
 * @author Matthew Tamayo-Rios &lt;matthew@openlattice.com&gt;
 */
public data class Participant(
    @field:NotBlank(message = "Participant ID is required")
    @field:Size(max = 255, message = "Participant ID exceeds maximum length")
    @param:JsonProperty(JsonFields.PARTICIPANT_ID) val participantId: String,

    @field:Valid
    @field:NotNull(message = "Candidate information is required")
    @param:JsonProperty(JsonFields.CANDIDATE) val candidate: Candidate,

    @field:NotNull(message = "Participation status is required")
    @param:JsonProperty(JsonFields.PARTICIPATION_STATUS) val participationStatus: ParticipationStatus,

    @field:Size(max = 50_000, message = "Participant notes exceed maximum length")
    @param:JsonProperty("participantNotes") val participantNotes: String? = null,
    @field:Size(max = 1000, message = "Participant has too many tags")
    @param:JsonProperty("participantTags")
    val participantTags: Set<@Size(max = 255, message = "Participant tag exceeds maximum length") String> = emptySet(),
    val withdrawnAt: java.time.OffsetDateTime? = null,
    val processingBlockedAt: java.time.OffsetDateTime? = null,
    val processingBlockReason: String? = null,
    val processingBlockedBy: String? = null,
    /** Server-assigned; a status change sends it back as If-Match. Ignored on registration. */
    val statusRevision: Long = 0,
    /** Server-derived from the participant's decisions; null before any device enrolled. Ignored on registration. */
    val consentState: ParticipantConsentState? = null,
)
