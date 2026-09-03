package com.openlattice.chronicle.collection

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.OffsetDateTime

/**
 * The kind of interaction the participant performed. All are content-free attention/salience
 * signals derived from `AccessibilityEvent` types — never element text or contentDescription.
 *
 * - [CLICK] / [LONG_CLICK] / [SCROLL] — the original pointer interactions.
 * - [FOCUS] — input focus moved to an element (`TYPE_VIEW_FOCUSED`); under keyboard/D-pad
 *   navigation this is a direct "what has attention" signal.
 * - [ACCESSIBILITY_FOCUS] — accessibility focus moved to an element
 *   (`TYPE_VIEW_ACCESSIBILITY_FOCUSED`); the explore-by-touch / screen-reader attention cursor.
 * - [SELECT] — an item was selected (`TYPE_VIEW_SELECTED`, e.g. a spinner/list selection); the
 *   *position* of the selection, never its text.
 */
public enum class InteractionEventType {
    CLICK,
    LONG_CLICK,
    SCROLL,
    FOCUS,
    ACCESSIBILITY_FOCUS,
    SELECT,
}

/**
 * Provenance for an interaction position observation.
 *
 * Android does not include pointer coordinates in a view accessibility event. Chronicle records
 * the raw screen-coordinate bounds supplied by [android.view.accessibility.AccessibilityNodeInfo]
 * without enabling an input-observing mode that would consume or alter the participant's touch
 * stream. The legacy center, normalized position, and grid cell are derivations of these bounds.
 */
public enum class InteractionPositionSource {
    ACCESSIBILITY_NODE_BOUNDS,
}

/**
 * One interaction-salience event produced by the `interaction_events` collection module
 * (see `docs/SENSING-EXPANSION-DESIGN.md` §6).
 *
 * This is the shared **wire contract** between the Android `:collection-interaction`
 * module and the server. Events are captured via an `AccessibilityService`. When the study
 * captures element position, each new event carries the authoritative raw
 * [AccessibilityNodeInfo][android.view.accessibility.AccessibilityNodeInfo] screen-coordinate
 * bounds ([nodeBoundsLeft], [nodeBoundsTop], [nodeBoundsRight], [nodeBoundsBottom]), their
 * [positionSource], and display context. These are the bounds of the interacted accessibility
 * element, **not the user's finger/pointer coordinate**. [rawX]/[rawY], normalized position, and
 * the coarse grid fields remain only as backward-compatible derivations for older consumers; new
 * analysis should derive any desired center/grid representation from the raw bounds. Each event
 * also carries
 * the tapped element's *role* ([elementRole], e.g. the view class name) and the
 * [foregroundPackage]. When the study's [InteractionPolicy] disables element-position capture,
 * the node bounds, provenance, display context, and legacy center/normalized fields are left
 * `null`; only the legacy derived grid cell is reported. It is `INTERACTION_METADATA`-class
 * data: **content-free by construction** —
 * the element's text and `contentDescription` are never captured, so this DTO has no field for
 * them. It also carries **no** `apiKey`, signing secret, or `participantId` field.
 *
 * The legacy grid dimensions remain on every event so older servers can interpret the legacy
 * derived cell even if the study's [InteractionPolicy] later changes.
 *
 * Validation (consistent with the other collection DTOs): the grid must have at least one
 * row and column, the cell index must fall inside the grid, a present [normalizedX]/
 * [normalizedY] must be a fraction in `[0, 1]`, a present [rawX]/[rawY] must be non-negative,
 * and a present [screenWidth]/[screenHeight] must be at least 1. [scrollDeltaX]/[scrollDeltaY]
 * are populated only for [InteractionEventType.SCROLL] (signed; magnitude may be unknown, in
 * which case they are `null`) and are left `null` for click events.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public data class AndroidInteractionEvent(
    /** Stable per-event identifier, used for de-duplication on insert/upload. */
    public val id: String,
    /** Event time in UTC. */
    public val timestamp: OffsetDateTime,
    /** Device-default time-zone id at event time. */
    public val timezone: String,
    /** The kind of interaction. */
    public val eventType: InteractionEventType,
    /** Number of rows in the screen-region grid this event was bucketed under; `>= 1`. */
    public val gridRows: Int,
    /** Number of columns in the screen-region grid this event was bucketed under; `>= 1`. */
    public val gridCols: Int,
    /** 0-based grid row of the interaction; `0 until gridRows`. */
    public val gridRow: Int,
    /** 0-based grid column of the interaction; `0 until gridCols`. */
    public val gridCol: Int,
    /** The interacted element's role (view class name); never its text or contentDescription. */
    public val elementRole: String,
    /** Package name of the foreground app at event time. */
    public val foregroundPackage: String,
    /** Provenance of the raw position observation; `null` on legacy clients. */
    public val positionSource: InteractionPositionSource? = null,
    /** Raw left edge of the accessibility node in screen pixels. May be negative under magnification. */
    public val nodeBoundsLeft: Int? = null,
    /** Raw top edge of the accessibility node in screen pixels. May be negative under magnification. */
    public val nodeBoundsTop: Int? = null,
    /** Raw right edge of the accessibility node in screen pixels. */
    public val nodeBoundsRight: Int? = null,
    /** Raw bottom edge of the accessibility node in screen pixels. */
    public val nodeBoundsBottom: Int? = null,
    /** Logical display id whose screen coordinate space contains the node; `null` on legacy clients. */
    public val displayId: Int? = null,
    /**
     * Legacy derived horizontal center of the accessibility node. This is not a pointer coordinate.
     * New consumers should derive a representation from the raw node bounds instead.
     */
    public val rawX: Int? = null,
    /**
     * Legacy derived vertical center of the accessibility node. This is not a pointer coordinate.
     */
    public val rawY: Int? = null,
    /**
     * Width of the node's logical display coordinate space at event time. Populated when the
     * study captures element position; `null` in the grid-only mode.
     */
    public val screenWidth: Int? = null,
    /**
     * Height of the node's logical display coordinate space at event time. Populated when the
     * study captures element position; `null` in the grid-only mode.
     */
    public val screenHeight: Int? = null,
    /**
     * Legacy normalized node-center position. New analysis should derive positions from the raw
     * node bounds and display context.
     */
    public val normalizedX: Double? = null,
    /**
     * Legacy normalized node-center position. New analysis should derive positions from the raw
     * node bounds and display context.
     */
    public val normalizedY: Double? = null,
    /** Signed horizontal scroll delta for a [InteractionEventType.SCROLL]; `null` otherwise / if unknown. */
    public val scrollDeltaX: Int? = null,
    /** Signed vertical scroll delta for a [InteractionEventType.SCROLL]; `null` otherwise / if unknown. */
    public val scrollDeltaY: Int? = null,
    /**
     * Monotonic event time in milliseconds since boot (`AccessibilityEvent.getEventTime()` /
     * `SystemClock.uptimeMillis()` domain). Unlike [timestamp] (wall-clock, which can jump on
     * clock corrections), this is strictly monotonic, so it is the authoritative basis for
     * ordering and for the kinematics below. `null` only on legacy clients that didn't capture it.
     */
    public val eventTimeMillis: Long? = null,
    /**
     * Opaque id grouping a burst of interactions into one *episode* (a continuous session of
     * activity, reset after an idle gap). Lets the server reconstruct scanpaths / per-session
     * salience without inferring session boundaries from gaps. `null` if not grouped.
     */
    public val episodeId: String? = null,
    /**
     * Milliseconds since the previous interaction in the same [episodeId] (the inter-event
     * dwell). A hesitation / reading-time proxy. `null` for the first event of an episode or on
     * legacy clients.
     */
    public val dwellMillisSincePrev: Long? = null,
    /**
     * Display rotation at event time as a `Surface.ROTATION_*` ordinal (`0` = portrait, `1` =
     * landscape, `2` = reverse-portrait, `3` = reverse-landscape), so spatial signals are
     * interpretable across orientation. Populated with element-position context; `null` otherwise.
     */
    public val orientation: Int? = null,
    /**
     * Display density in DPI at event time, so raw pixels are physically interpretable.
     * Populated with element-position context; `null` otherwise.
     */
    public val screenDensityDpi: Int? = null,
    /**
     * Horizontal scroll velocity in pixels/second for a [InteractionEventType.SCROLL] (delta ÷
     * inter-event time). Skim-vs-read signal. `null` for non-scroll events or when undeterminable.
     */
    public val scrollVelocityX: Double? = null,
    /** Vertical scroll velocity in pixels/second for a [InteractionEventType.SCROLL]; `null` otherwise. */
    public val scrollVelocityY: Double? = null,
    /**
     * Whether this [InteractionEventType.SCROLL] reversed the previous scroll's dominant
     * direction (a re-reading / back-tracking signal). `null` for non-scroll events or when there
     * is no prior scroll in the episode to compare against.
     */
    public val scrollReversed: Boolean? = null,
) {
    init {
        require(gridRows >= 1) {
            "AndroidInteractionEvent.gridRows must be at least 1: $gridRows"
        }
        require(gridCols >= 1) {
            "AndroidInteractionEvent.gridCols must be at least 1: $gridCols"
        }
        require(gridRow in 0 until gridRows) {
            "AndroidInteractionEvent.gridRow ($gridRow) must be in 0 until gridRows ($gridRows)"
        }
        require(gridCol in 0 until gridCols) {
            "AndroidInteractionEvent.gridCol ($gridCol) must be in 0 until gridCols ($gridCols)"
        }
        normalizedX?.let {
            require(it in 0.0..1.0) {
                "AndroidInteractionEvent.normalizedX must be a fraction in [0, 1]: $it"
            }
        }
        normalizedY?.let {
            require(it in 0.0..1.0) {
                "AndroidInteractionEvent.normalizedY must be a fraction in [0, 1]: $it"
            }
        }
        rawX?.let { require(it >= 0) { "AndroidInteractionEvent.rawX must be non-negative: $it" } }
        rawY?.let { require(it >= 0) { "AndroidInteractionEvent.rawY must be non-negative: $it" } }
        val bounds = listOf(nodeBoundsLeft, nodeBoundsTop, nodeBoundsRight, nodeBoundsBottom)
        require(bounds.all { it == null } || bounds.all { it != null }) {
            "AndroidInteractionEvent node bounds must be entirely present or entirely absent"
        }
        if (bounds.all { it != null }) {
            require(positionSource != null) { "AndroidInteractionEvent node bounds require positionSource" }
            require(nodeBoundsLeft!! <= nodeBoundsRight!!) {
                "AndroidInteractionEvent node bounds left must not exceed right"
            }
            require(nodeBoundsTop!! <= nodeBoundsBottom!!) {
                "AndroidInteractionEvent node bounds top must not exceed bottom"
            }
        }
        require(positionSource == null || bounds.all { it != null }) {
            "AndroidInteractionEvent positionSource requires node bounds"
        }
        displayId?.let { require(it >= 0) { "AndroidInteractionEvent.displayId must be non-negative: $it" } }
        if (positionSource != null) {
            require(displayId != null) { "AndroidInteractionEvent positionSource requires displayId" }
            require(screenWidth != null && screenHeight != null) {
                "AndroidInteractionEvent positionSource requires display dimensions"
            }
            require(screenDensityDpi != null) {
                "AndroidInteractionEvent positionSource requires display density"
            }
        }
        screenWidth?.let { require(it >= 1) { "AndroidInteractionEvent.screenWidth must be at least 1: $it" } }
        screenHeight?.let { require(it >= 1) { "AndroidInteractionEvent.screenHeight must be at least 1: $it" } }
        eventTimeMillis?.let { require(it >= 0) { "AndroidInteractionEvent.eventTimeMillis must be non-negative: $it" } }
        dwellMillisSincePrev?.let {
            require(it >= 0) { "AndroidInteractionEvent.dwellMillisSincePrev must be non-negative: $it" }
        }
        orientation?.let { require(it in 0..3) { "AndroidInteractionEvent.orientation must be a Surface.ROTATION_* ordinal (0..3): $it" } }
        screenDensityDpi?.let {
            require(it >= 1) { "AndroidInteractionEvent.screenDensityDpi must be at least 1: $it" }
        }
    }
}
