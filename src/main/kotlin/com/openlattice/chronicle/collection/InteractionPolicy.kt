package com.openlattice.chronicle.collection

/**
 * Interaction-event policy for the `interaction_events` module
 * (see `docs/SENSING-EXPANSION-DESIGN.md` §3.3 / §6).
 *
 * Interaction events are captured via an Accessibility service. By default
 * ([captureElementPosition] = `true`) each event records the raw screen-coordinate bounds of the
 * interacted accessibility element. Android accessibility view events do not expose the user's
 * exact finger/pointer coordinate; Chronicle deliberately does not enable input interception to
 * obtain it. The element center, normalized position, and [gridRows] × [gridCols] cell are legacy
 * compatibility derivations rather than primary observations.
 *
 * In all modes the captured signal is the tapped element's *role* and the
 * foreground app — never the element's text or content description. That
 * content-freeness is a constructional invariant, not a configurable option, so it
 * is intentionally absent from this DTO.
 *
 * Validation (consistent with [CollectionCadence] / [BatteryPolicy]): the grid must
 * have at least one row and one column.
 *
 * Hard constraint: this DTO carries **no** `apiKey`, signing secret, or
 * `participantId` field — it is pure configuration.
 */
public data class InteractionPolicy(
    /** Legacy derived screen-region grid row count. Must be >= 1. */
    public val gridRows: Int = 4,
    /** Legacy derived screen-region grid column count. Must be >= 1. */
    public val gridCols: Int = 3,
    /** Whether tap / click events are recorded. */
    public val captureClicks: Boolean = true,
    /** Whether scroll events (direction + magnitude) are recorded. */
    public val captureScrolls: Boolean = true,
    /**
     * Legacy wire alias for [captureElementPosition]. It is retained so older clients continue to
     * honor the same study choice. This does not enable exact pointer capture.
     */
    public val captureExactPosition: Boolean = true,
    /**
     * Whether raw accessibility-node bounds and their display context are recorded. This is
     * element geometry, not a finger/touch coordinate. The default follows the legacy
     * [captureExactPosition] value so old settings payloads retain their behavior.
     */
    public val captureElementPosition: Boolean = captureExactPosition,
) {
    init {
        require(gridRows >= 1) {
            "InteractionPolicy.gridRows must be at least 1: $gridRows"
        }
        require(gridCols >= 1) {
            "InteractionPolicy.gridCols must be at least 1: $gridCols"
        }
        require(captureElementPosition == captureExactPosition) {
            "InteractionPolicy position flags must agree while the legacy wire alias is supported"
        }
    }

    public companion object {
        /**
         * Default policy: clicks and scrolls recorded, raw accessibility-element bounds captured,
         * and a legacy 4×3 derived grid emitted for backward compatibility.
         */
        public val DEFAULT: InteractionPolicy = InteractionPolicy()
    }
}
