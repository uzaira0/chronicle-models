package com.openlattice.chronicle.android

import java.util.UUID

/** Exact durable outcomes; held rows remain on the phone until a later settings revision. */
public data class SensorUploadReceipt(
    val acceptedIds: Set<UUID>,
    val discardedIds: Set<UUID>,
    val heldIds: Set<UUID>,
    val settingsVersion: Int,
) {
    public fun validateSubmitted(ids: Set<UUID>) {
        require(heldIds.isEmpty() || settingsVersion > 0) { "Held sensor rows require an authoritative revision" }
        require(acceptedIds.intersect(discardedIds).isEmpty() && acceptedIds.intersect(heldIds).isEmpty() &&
            discardedIds.intersect(heldIds).isEmpty() && acceptedIds + discardedIds + heldIds == ids) {
            "Sensor receipt does not partition the submitted sample IDs"
        }
    }
}
