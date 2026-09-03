package com.openlattice.chronicle.collection

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.OffsetDateTime

/** Detected activity class, from Play Services `DetectedActivity` types. */
public enum class DetectedActivityType {
    STILL, WALKING, RUNNING, ON_FOOT, IN_VEHICLE, ON_BICYCLE, TILTING, UNKNOWN
}

/**
 * Whether this sample marks entering or exiting an activity, from the Activity Transition API.
 * `null` on a periodic activity-result sample (no transition semantics).
 */
public enum class ActivityTransitionType { ENTER, EXIT }

/**
 * One activity-recognition sample produced by the `activity_recognition` collection module
 * (Play Services ActivityRecognition / Activity Transition API).
 *
 * **Content-free.** Google's on-device classifier fuses low-power motion sensors; this app runs no
 * model and no raw sensor stream or location leaves the device — only an activity label plus a
 * confidence. `BEHAVIORAL_METADATA`-class, opt-in (default OFF). No `apiKey`, signing secret, or
 * `participantId` field. Unknown JSON fields are ignored for forward compatibility.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public data class AndroidActivityRecognitionEvent(
    /** Stable per-event identifier, used for de-duplication on insert/upload. */
    public val id: String,
    /** Sample/detection time in UTC. */
    public val timestamp: OffsetDateTime,
    /** Device-default time-zone id at sample time. */
    public val timezone: String,
    /** The detected activity class. */
    public val activityType: DetectedActivityType,
    /** Classifier confidence 0–100 for [activityType]. */
    public val confidence: Int,
    /**
     * For an Activity Transition sample: whether [activityType] was entered or exited. `null` for a
     * periodic activity-result sample.
     */
    public val transitionType: ActivityTransitionType? = null,
) {
    init {
        require(id.isNotBlank()) { "AndroidActivityRecognitionEvent.id must not be blank" }
        require(timezone.isNotBlank()) { "AndroidActivityRecognitionEvent.timezone must not be blank" }
        require(confidence in 0..100) { "AndroidActivityRecognitionEvent.confidence must be 0..100: $confidence" }
    }
}
