package com.openlattice.chronicle.collection

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * What the device should do with data a module has already collected on-device but
 * not yet uploaded, when a researcher **disables** that module mid-study.
 *
 * Carried on [CollectionModuleSetting.disableDisposition] and chosen by the
 * researcher at the time of the disable action (design
 * `docs/COLLECTION-LOOP-CLOSURE-DESIGN.md` §5.1). The device acts on the disposition
 * only on the `ACTIVE → INACTIVE` transition (a module that was never collecting has
 * nothing to dispose of).
 *
 * Each constant is a stable lowercase snake_case wire id, serialized via
 * [CollectionDataDisposition.id] — never a raw string literal — mirroring
 * [CollectionModuleId]. The default disable disposition is [FLUSH_THEN_STOP] (no data
 * loss).
 *
 * @author uzaira0
 */
public enum class CollectionDataDisposition(
    /** Stable lowercase snake_case wire identifier. */
    @get:JsonValue
    public val id: String,
) {
    /** Drain and upload the module's pending queue, then stop collecting. No data loss. */
    FLUSH_THEN_STOP("flush_then_stop"),

    /** Drop the module's unsent queue, then stop collecting. Intentional data loss. */
    DISCARD_AND_STOP("discard_and_stop"),

    /**
     * Stop collecting but retain the module's pending queue locally (subject to a
     * device-side cap) until the module is re-enabled (and re-acknowledged) or the
     * disposition is later changed to flush/discard.
     */
    HOLD_PENDING("hold_pending"),
    ;

    public companion object {
        private val BY_ID: Map<String, CollectionDataDisposition> = entries.associateBy { it.id }

        /** Resolves a wire string to a [CollectionDataDisposition], or `null` if unknown. */
        @JvmStatic
        public fun fromIdOrNull(id: String): CollectionDataDisposition? = BY_ID[id]

        /** Jackson value-deserialization entry point; throws on an unknown wire id. */
        @JvmStatic
        @JsonCreator
        public fun fromId(id: String): CollectionDataDisposition =
            BY_ID[id] ?: throw IllegalArgumentException("Unknown CollectionDataDisposition: $id")
    }
}
