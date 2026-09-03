package com.openlattice.chronicle.crypto

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * Logical stream an [EncryptedEnvelope] carries.
 *
 * The device seals one batch of one stream into a single envelope; the backend stores
 * it blind and routes by this tag without decrypting. Serialized as a stable lowercase
 * wire id (never the ordinal), mirroring [com.openlattice.chronicle.collection.CollectionModuleId].
 */
public enum class EncryptedPayloadType(@get:JsonValue public val id: String) {
    /** A batch of [com.openlattice.chronicle.android.AndroidSensorSample]. */
    SENSOR("sensor"),

    /** A [com.openlattice.chronicle.android.ChronicleData] blob (usage + device-state events). */
    USAGE("usage"),

    /** A batch of [com.openlattice.chronicle.collection.BatterySample]. */
    BATTERY("battery"),

    /** A batch of [com.openlattice.chronicle.collection.AndroidInteractionEvent]. */
    INTERACTION("interaction"),

    /** A batch of [com.openlattice.chronicle.collection.AndroidAudioActivityEvent]. */
    AUDIO_ACTIVITY("audio_activity"),

    /** A batch of [com.openlattice.chronicle.collection.AndroidAudioContentEvent]. */
    AUDIO_CONTENT("audio_content"),

    /** A batch of [com.openlattice.chronicle.collection.AndroidNotificationActivityEvent]. */
    NOTIFICATION_ACTIVITY("notification_activity"),

    /** A batch of [com.openlattice.chronicle.collection.AndroidSleepEvent]. */
    SLEEP("sleep"),

    /** A batch of [com.openlattice.chronicle.collection.AndroidActivityRecognitionEvent]. */
    ACTIVITY_RECOGNITION("activity_recognition"),

    /** A batch of [com.openlattice.chronicle.collection.AndroidHealthMetricEvent]. */
    HEALTH_CONNECT("health_connect"),

    /** A batch of [com.openlattice.chronicle.collection.AndroidConnectivityStateEvent]. */
    CONNECTIVITY_STATE("connectivity_state"),

    /** A batch of [com.openlattice.chronicle.collection.AndroidAppNetworkUsageEvent]. */
    APP_NETWORK_USAGE("app_network_usage"),

    /** A batch of [com.openlattice.chronicle.collection.AndroidDeviceSettingsEvent]. */
    DEVICE_SETTINGS("device_settings"),
    ;

    public companion object {
        private val byId: Map<String, EncryptedPayloadType> = entries.associateBy { it.id }

        @JvmStatic
        @JsonCreator
        public fun fromId(id: String): EncryptedPayloadType =
            byId[id] ?: throw IllegalArgumentException("Unknown encrypted payload type: $id")

        @JvmStatic
        public fun fromIdOrNull(id: String): EncryptedPayloadType? = byId[id]
    }
}
