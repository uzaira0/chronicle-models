package com.openlattice.chronicle.android

import java.util.UUID

/** Exact durable outcomes of one upload: every submitted row is either accepted or discarded. */
public data class UploadReceipt(
    val acceptedIds: Set<UUID>,
    val discardedIds: Set<UUID>,
    val settingsVersion: Int,
) {
    public fun validateSubmitted(ids: Set<UUID>) {
        require(acceptedIds.intersect(discardedIds).isEmpty() && acceptedIds + discardedIds == ids) {
            "Upload receipt does not partition the submitted row IDs"
        }
    }
}
