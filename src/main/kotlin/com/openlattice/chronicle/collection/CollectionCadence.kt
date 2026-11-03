package com.openlattice.chronicle.collection

import com.openlattice.chronicle.generated.CollectionDefaultValues

/**
 * How often a collection action runs.
 *
 * Validation: a non-positive [intervalSeconds] is rejected — cadence
 * must always be a positive interval. [jitterSeconds] must be non-negative and is
 * clamped against the interval so jitter can never exceed the interval itself.
 *
 * @author uzaira0
 */
public data class CollectionCadence(
    /** Base interval between actions, in seconds. Must be positive. */
    val intervalSeconds: Long = CollectionDefaultValues.INTERVAL_SECONDS,
    /** Random spread added to the interval, in seconds. Non-negative, <= [intervalSeconds]. */
    val jitterSeconds: Long = 0L,
) {
    init {
        require(intervalSeconds > 0L) {
            "CollectionCadence.intervalSeconds must be positive: $intervalSeconds"
        }
        require(jitterSeconds >= 0L) {
            "CollectionCadence.jitterSeconds must be non-negative: $jitterSeconds"
        }
        require(jitterSeconds <= intervalSeconds) {
            "CollectionCadence.jitterSeconds ($jitterSeconds) must not exceed intervalSeconds ($intervalSeconds)"
        }
    }

    public companion object {
        /** Default usage/lifecycle polling cadence. */
        public val DEFAULT_COLLECTION: CollectionCadence = CollectionCadence(intervalSeconds = 900L)
    }
}
