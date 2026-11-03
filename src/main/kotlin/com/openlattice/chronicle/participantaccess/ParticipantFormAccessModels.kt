package com.openlattice.chronicle.participantaccess

import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

public data class CreateParticipantFormAccessCodeRequest(
    val formKind: ParticipantFormKind,
    val resourceId: UUID? = null,
    val logicalDate: LocalDate? = null,
    val expiresAt: OffsetDateTime? = null,
)

/** The raw access code is returned once and must only be placed in a URL fragment. */
public data class ParticipantFormAccessCodeResponse(
    val accessCode: String,
    val expiresAt: OffsetDateTime,
    val formKind: ParticipantFormKind,
    val resourceId: UUID? = null,
    val logicalDate: LocalDate? = null,
)

public data class ExchangeParticipantFormAccessCodeRequest(
    val accessCode: String,
    val attemptId: UUID,
)

/**
 * Public session context. The session credential itself is an HttpOnly cookie; only the
 * double-submit mutation token is exposed to the participant UI.
 */
public data class ParticipantFormSessionResponse(
    val csrfToken: String,
    val studyId: UUID,
    val participantId: String,
    val formKind: ParticipantFormKind,
    val resourceId: UUID? = null,
    val logicalDate: LocalDate? = null,
    val expiresAt: OffsetDateTime,
    /** Study participant-policy links, shown in the participant form footer. */
    val privacyPolicyUrl: String? = null,
    val withdrawalUrl: String? = null,
)
