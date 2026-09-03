package com.openlattice.chronicle.collection

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * Health Connect record families a study may request from the Android client.
 *
 * The stable wire identifiers are deliberately independent of Android class names. A study must
 * select an explicit subset; an omitted/empty set means Chronicle requests and reads no Health
 * Connect records.
 */
public enum class HealthConnectRecordType(
    @get:JsonValue public val id: String,
) {
    STEPS("steps"),
    DISTANCE("distance"),
    HEART_RATE("heart_rate"),
    TOTAL_CALORIES_BURNED("total_calories_burned"),
    ACTIVE_CALORIES_BURNED("active_calories_burned"),
    FLOORS_CLIMBED("floors_climbed"),
    RESTING_HEART_RATE("resting_heart_rate"),
    OXYGEN_SATURATION("oxygen_saturation"),
    RESPIRATORY_RATE("respiratory_rate"),
    SLEEP("sleep"),
    EXERCISE("exercise"),
    HEART_RATE_VARIABILITY("heart_rate_variability"),
    BODY_TEMPERATURE("body_temperature"),
    SKIN_TEMPERATURE("skin_temperature"),
    ;

    public companion object {
        private val BY_ID: Map<String, HealthConnectRecordType> = entries.associateBy { it.id }

        @JvmStatic
        @JsonCreator
        public fun fromId(id: String): HealthConnectRecordType =
            requireNotNull(BY_ID[id]) { "Unknown Health Connect record type: $id" }
    }
}
