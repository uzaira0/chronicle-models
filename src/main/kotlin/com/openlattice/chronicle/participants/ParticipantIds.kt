package com.openlattice.chronicle.participants

import com.openlattice.chronicle.generated.ParticipantProcessingContracts

/** The participant-id rule (owned by the ontology): server writes and Android enrollment links validate with it. */
public object ParticipantIds {
    private val PATTERN = Regex(ParticipantProcessingContracts.PARTICIPANT_ID_PATTERN)

    public fun isValid(id: String): Boolean = PATTERN.matches(id)
}
