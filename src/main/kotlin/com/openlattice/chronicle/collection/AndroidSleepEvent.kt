package com.openlattice.chronicle.collection

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.OffsetDateTime

/**
 * Which Play Services Sleep API delivery produced an [AndroidSleepEvent].
 *
 * The Sleep API emits two kinds of update: a post-hoc [SEGMENT] summarizing a detected sleep
 * period, and a periodic [CLASSIFY] (≈ every 10 min) carrying the current sleep confidence plus
 * the light/motion levels the on-device classifier used.
 */
public enum class SleepEventType { SEGMENT, CLASSIFY }

/** Status of a detected sleep [SleepEventType.SEGMENT], from `SleepSegmentEvent.getStatus()`. */
public enum class SleepSegmentStatus { SUCCESSFUL, MISSING_DATA, NOT_DETECTED, UNKNOWN }

/**
 * One sleep sample produced by the `sleep` collection module (Play Services Sleep API).
 *
 * **Content-free and mic-free.** Google's on-device classifier fuses the ambient-light sensor and
 * device motion; this app runs no model and no raw sensor stream leaves the device. The sample
 * carries only a sleep label/confidence and the coarse light/motion levels the classifier reports.
 * `HEALTH_METRICS`-class, opt-in. No `apiKey`, signing secret, or `participantId` field. Unknown
 * JSON fields are ignored for forward compatibility.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public data class AndroidSleepEvent(
    /** Stable per-event identifier, used for de-duplication on insert/upload. */
    public val id: String,
    /** Sample/detection time in UTC. */
    public val timestamp: OffsetDateTime,
    /** Device-default time-zone id at sample time. */
    public val timezone: String,
    /** Which Sleep API delivery this is. */
    public val eventType: SleepEventType,
    /** [SleepEventType.SEGMENT] start (epoch millis); `null` for a [SleepEventType.CLASSIFY]. */
    public val segmentStartMillis: Long? = null,
    /** [SleepEventType.SEGMENT] end (epoch millis); `null` for a [SleepEventType.CLASSIFY]. */
    public val segmentEndMillis: Long? = null,
    /** Detection status of a [SleepEventType.SEGMENT]; `null` for a [SleepEventType.CLASSIFY]. */
    public val segmentStatus: SleepSegmentStatus? = null,
    /** Sleep confidence 0–100 from a [SleepEventType.CLASSIFY]; `null` for a [SleepEventType.SEGMENT]. */
    public val confidence: Int? = null,
    /** Ambient-light level the classifier reported (`SleepClassifyEvent.getLight()`); when known. */
    public val light: Int? = null,
    /** Device-motion level the classifier reported (`SleepClassifyEvent.getMotion()`); when known. */
    public val motion: Int? = null,
) {
    init {
        require(id.isNotBlank()) { "AndroidSleepEvent.id must not be blank" }
        require(timezone.isNotBlank()) { "AndroidSleepEvent.timezone must not be blank" }
        confidence?.let { require(it in 0..100) { "AndroidSleepEvent.confidence must be 0..100: $it" } }
        if (segmentStartMillis != null && segmentEndMillis != null) {
            require(segmentEndMillis >= segmentStartMillis) {
                "AndroidSleepEvent segment end ($segmentEndMillis) must be >= start ($segmentStartMillis)"
            }
        }
    }
}
