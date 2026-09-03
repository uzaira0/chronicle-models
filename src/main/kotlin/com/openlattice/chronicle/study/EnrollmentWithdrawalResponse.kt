package com.openlattice.chronicle.study

import java.util.UUID

/** Accepted participant withdrawal and the deletion jobs created for it. */
public data class EnrollmentWithdrawalResponse(
    val requestId: UUID,
    val status: String = "accepted",
    val deletionJobIds: List<UUID> = emptyList(),
    val alreadyWithdrawn: Boolean = false,
)
