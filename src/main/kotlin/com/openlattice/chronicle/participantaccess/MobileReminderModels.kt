package com.openlattice.chronicle.participantaccess

import com.openlattice.chronicle.data.ParticipationStatus
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
    /** Zone the rule recurs in; null recurs in the phone's own zone. */
    val timeZone: String? = null,
    /**
     * App-usage only: an ended survey day still has apps to answer. The phone reminds only while it is true, and an
     * offline phone reminds on the last state it fetched.
     */
    val unansweredDays: Boolean = false,
)
