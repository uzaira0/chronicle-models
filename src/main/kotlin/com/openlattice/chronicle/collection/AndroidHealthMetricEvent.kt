package com.openlattice.chronicle.collection

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.OffsetDateTime

/**
 * The Health Connect record category a [AndroidHealthMetricEvent] carries. A generic, evolvable
 * subset of Health Connect's record types; anything not modeled maps to [OTHER]. The [value] and
 * [unit] are interpreted per type (e.g. [STEPS] → count, [HEART_RATE] → bpm, [DISTANCE] → meters).
 */
public enum class HealthMetricType {
    STEPS, DISTANCE, ACTIVE_CALORIES, TOTAL_CALORIES, HEART_RATE, RESTING_HEART_RATE,
    OXYGEN_SATURATION, RESPIRATORY_RATE, SLEEP_SESSION, EXERCISE_SESSION, FLOORS_CLIMBED,
    // Expanded Health Connect record coverage (read-only; written by the participant's own
    // apps/wearables). HRV → ms; BODY/SKIN_TEMPERATURE → celsius. Sleep stages are modeled
    // one-type-per-stage (value = stage duration in minutes) for clean queryability, derived
    // from SleepSessionRecord.stages under the existing READ_SLEEP permission.
    HEART_RATE_VARIABILITY, BODY_TEMPERATURE, SKIN_TEMPERATURE,
    SLEEP_STAGE_AWAKE, SLEEP_STAGE_LIGHT, SLEEP_STAGE_DEEP, SLEEP_STAGE_REM,
    SLEEP_STAGE_OUT_OF_BED, SLEEP_STAGE_AWAKE_IN_BED, SLEEP_STAGE_UNKNOWN,
    OTHER
}

/**
 * One health metric read from the system Health Connect store by the `health_connect` collection
 * module.
 *
 * Each sample is a single aggregated/instantaneous record over the window [[startMillis], [endMillis]]
 * (equal for an instantaneous reading). Health Connect is the source of truth — Chronicle only reads
 * records the participant's own apps/wearables have written. `HEALTH_METRICS`-class, opt-in. No
 * `apiKey`, signing secret, or `participantId` field. Unknown JSON fields are ignored for forward
 * compatibility.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public data class AndroidHealthMetricEvent(
    /** Stable per-event identifier, used for de-duplication on insert/upload. */
    public val id: String,
    /** Read/capture time in UTC. */
    public val timestamp: OffsetDateTime,
    /** Device-default time-zone id at sample time. */
    public val timezone: String,
    /** The health record category. */
    public val metricType: HealthMetricType,
    /** The metric value, interpreted per [metricType] and [unit]. Must be finite. */
    public val value: Double,
    /** Unit for [value] (e.g. `count`, `bpm`, `m`, `kcal`, `%`). */
    public val unit: String,
    /** Record window start (epoch millis). */
    public val startMillis: Long,
    /** Record window end (epoch millis); equals [startMillis] for an instantaneous reading. */
    public val endMillis: Long,
    /** Package of the app/wearable integration that wrote the record, when known. */
    public val sourcePackage: String? = null,
) {
    init {
        require(id.isNotBlank()) { "AndroidHealthMetricEvent.id must not be blank" }
        require(timezone.isNotBlank()) { "AndroidHealthMetricEvent.timezone must not be blank" }
        require(unit.isNotBlank()) { "AndroidHealthMetricEvent.unit must not be blank" }
        require(value.isFinite()) { "AndroidHealthMetricEvent.value must be finite: $value" }
        require(endMillis >= startMillis) {
            "AndroidHealthMetricEvent end ($endMillis) must be >= start ($startMillis)"
        }
    }
}
