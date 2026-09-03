package com.openlattice.chronicle.participants

import com.fasterxml.jackson.annotation.JsonProperty
import com.openlattice.chronicle.candidates.Candidate
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

    @param:JsonProperty("participantNotes") val participantNotes: String? = null,
    @param:JsonProperty("participantTags") val participantTags: Set<String> = emptySet(),
)