package com.openlattice.chronicle.participantaccess

import com.openlattice.chronicle.data.ParticipationStatus
import java.time.OffsetDateTime
import java.util.UUID

/** Language-neutral reminder contract for Android and a future Serde client. */
public data class MobileReminderConfiguration(
    val participationStatus: ParticipationStatus,
    val forms: List<MobileReminderForm>,
)

public data class MobileReminderForm(
    val formKind: ParticipantFormKind,
    val resourceId: UUID? = null,
    val title: String,
    val recurrenceRule: String,
    val accessCode: String,
    val accessCodeExpiresAt: OffsetDateTime,
)
