package com.openlattice.chronicle.collection

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * Stable identifiers for data collection modules.
 *
 * Per design §1A.1 each ID is a lowercase snake_case string, declared exactly once,
 * stable, and never reused. Wire and diagnostics use [CollectionModuleId.id] — never
 * a raw string literal. IDs are decoupled from class names and package paths so a
 * module can move during the Gradle split (Phase 10) without an ID change.
 *
 * Reserved (inactive) IDs (design §1A.3) freeze the namespace for known future
 * modules; they carry [active] = `false`, have no implementation, and must fail the
 * "module must be registered" guardrail if instantiated.
 *
 * @author uzaira0
 */
public enum class CollectionModuleId(
    /** Stable lowercase snake_case wire identifier. */
    @get:JsonValue
    public val id: String,
    /** The privacy classification this module's output falls under (design §1A.4). */
    public val privacyClass: CollectionPrivacyClass,
    /** `false` for reserved IDs that freeze the namespace but have no implementation. */
    public val active: Boolean,
    /**
     * Per-module override of the default-enabled policy. When non-null it wins over
     * [CollectionPrivacyClass.defaultEnabled] (see [CollectionDefaults.defaultEnabled]).
     * Used for a module whose privacy class would otherwise imply a different default —
     * e.g. `in_app_activity_class` is BEHAVIORAL_METADATA (class default-on) but is a finer,
     * more-revealing within-app signal we want **opt-in**, so it overrides to `false`.
     */
    public val defaultEnabledOverride: Boolean? = null,
) {
    // ----- Active modules (design §1A.2) -----
    USAGE_EVENTS("usage_events", CollectionPrivacyClass.BEHAVIORAL_METADATA, true),

    // The within-app Activity/screen class refinement of `usage_events`. Same source
    // (UsageStatsManager) and same per-transition resolution as the app-usage log — it is
    // the foreground Activity class (`UsageEvents.Event.getClassName`) that already rides each
    // usage event, split out into its own independently-toggleable module. When this module is
    // off, usage events upload package-level only (the activity class is stripped on-device).
    // BEHAVIORAL_METADATA, but default OFF (opt-in): the sub-screen activity class is finer and
    // more revealing than package-level usage, so a study must explicitly enable it.
    IN_APP_ACTIVITY_CLASS("in_app_activity_class", CollectionPrivacyClass.BEHAVIORAL_METADATA, true, defaultEnabledOverride = false),
    DEVICE_LIFECYCLE("device_lifecycle", CollectionPrivacyClass.DEVICE_STATE_METADATA, true),
    USER_IDENTIFICATION("user_identification", CollectionPrivacyClass.LOCAL_PARTICIPANT_LABEL, true),
    UPLOAD_TELEMETRY("upload_telemetry", CollectionPrivacyClass.OPERATIONAL_DIAGNOSTICS, true),
    SENSOR_AVAILABILITY("sensor_availability", CollectionPrivacyClass.DEVICE_CAPABILITY, true),
    QUESTIONNAIRE("questionnaire", CollectionPrivacyClass.BEHAVIORAL_METADATA, true),
    BATTERY_TELEMETRY("battery_telemetry", CollectionPrivacyClass.DEVICE_STATE_METADATA, true),

    // ----- Per-sensor hardware modules (per-sensor consent redesign, 2026-06-11) -----
    // Each AndroidSensorType is its own first-class module: the study marks it
    // required / optional / unavailable and the participant consents to each one
    // individually, exactly like the App & Device Usage modules. There is no grouped
    // "hardware sensors" unit. Each sensor's own sampling rate + duty cycle ride
    // CollectionModuleSetting.sensorPolicy (web-editable, mobile read-only). The
    // AndroidSensorType <-> CollectionModuleId mapping lives in SensorCollectionModules.
    SENSOR_ACCELEROMETER("sensor_accelerometer", CollectionPrivacyClass.PHYSICAL_TELEMETRY, true),
    SENSOR_GYROSCOPE("sensor_gyroscope", CollectionPrivacyClass.PHYSICAL_TELEMETRY, true),
    SENSOR_MAGNETOMETER("sensor_magnetometer", CollectionPrivacyClass.PHYSICAL_TELEMETRY, true),
    SENSOR_GRAVITY("sensor_gravity", CollectionPrivacyClass.PHYSICAL_TELEMETRY, true),
    SENSOR_LINEAR_ACCELERATION("sensor_linear_acceleration", CollectionPrivacyClass.PHYSICAL_TELEMETRY, true),
    SENSOR_ROTATION_VECTOR("sensor_rotation_vector", CollectionPrivacyClass.PHYSICAL_TELEMETRY, true),
    SENSOR_STEP_COUNTER("sensor_step_counter", CollectionPrivacyClass.PHYSICAL_TELEMETRY, true),
    SENSOR_LIGHT("sensor_light", CollectionPrivacyClass.PHYSICAL_TELEMETRY, true),
    SENSOR_PROXIMITY("sensor_proximity", CollectionPrivacyClass.PHYSICAL_TELEMETRY, true),
    SENSOR_SIGNIFICANT_MOTION("sensor_significant_motion", CollectionPrivacyClass.PHYSICAL_TELEMETRY, true),
    SENSOR_TILT_DETECTOR("sensor_tilt_detector", CollectionPrivacyClass.PHYSICAL_TELEMETRY, true),
    SENSOR_SCREEN_ORIENTATION("sensor_screen_orientation", CollectionPrivacyClass.PHYSICAL_TELEMETRY, true),
    // Samsung vendor sensors retired to decode-only aliases (2026-06-12). `grip_wifi` is the
    // SX9375 SAR capacitive sensor Samsung uses for antenna power back-off (not a behavioral
    // signal); `samsung_motion` (vendor type 65559) is a proprietary composite with undocumented
    // payload. Both are Samsung-only (non-portable across the panel) and uninterpretable for
    // research, and duplicate signals already captured portably (accelerometer / proximity /
    // significant_motion). active = false: never offered, gated, collected, or persisted afresh;
    // their AndroidSensorType <-> module mapping survives in SensorCollectionModules.byType only so
    // any legacy persisted "sensor_samsung_*" setting/ack and any raw vendor sample still decode.
    SENSOR_SAMSUNG_GRIP_WIFI("sensor_samsung_grip_wifi", CollectionPrivacyClass.PHYSICAL_TELEMETRY, false),
    SENSOR_SAMSUNG_MOTION("sensor_samsung_motion", CollectionPrivacyClass.PHYSICAL_TELEMETRY, false),

    // ----- Sensing expansion — interaction salience (docs/SENSING-EXPANSION-DESIGN.md §6) -----
    // Captured via an AccessibilityService and reduced to a coarse screen-region grid cell +
    // element role + foreground package; content-free by construction (element text /
    // contentDescription are never logged). Default-disabled (INTERACTION_METADATA),
    // consent-gated, and requires the participant to enable the accessibility service.
    INTERACTION_EVENTS("interaction_events", CollectionPrivacyClass.INTERACTION_METADATA, true),

    // App-audio sensing (docs/SENSING-EXPANSION-DESIGN.md §4). Mic-free: derived from AudioManager
    // playback/output state + the active MediaSession (which app + playback), never the microphone.
    // audio_activity — content-free behavioral metadata (route/playback/volume/call/which-app);
    // opt-in (default OFF). audio_content — media-session metadata (title/artist the participant is
    // playing), MEDIA_CONTENT (default OFF). NOT raw audio capture (no RECORD_AUDIO / MediaProjection).
    AUDIO_ACTIVITY("audio_activity", CollectionPrivacyClass.BEHAVIORAL_METADATA, true, defaultEnabledOverride = false),
    AUDIO_CONTENT("audio_content", CollectionPrivacyClass.MEDIA_CONTENT, true),
    // Content-free notification-activity ("digital interruption load"): per-app notification counts /
    // categories / timing via the NotificationListenerService — never notification text. Opt-in (default OFF).
    NOTIFICATION_ACTIVITY("notification_activity", CollectionPrivacyClass.BEHAVIORAL_METADATA, true, defaultEnabledOverride = false),
    // Ambient-sound classification (currently iOS-only: Apple SoundAnalysis). The mic-bearing
    // counterpart of the mic-free audio modules above: short duty-cycled listen windows are
    // classified ON DEVICE and reduced to labels + confidences (music / speech / television / …);
    // the audio is discarded at the classifier boundary — no recording, transcript, or waveform
    // ever exists past it. AMBIENT_AUDIO_CONTEXT-class (its own mic tier), opt-in (default OFF).
    // No Android implementation yet — the id is active for study configuration + iOS collection.
    AMBIENT_AUDIO("ambient_audio", CollectionPrivacyClass.AMBIENT_AUDIO_CONTEXT, true),

    // ----- Sensing expansion — passive context & health (docs/SENSING-EXPANSION-DESIGN.md) -----
    // All opt-in (default OFF): a study must explicitly enable each, and the participant consents.
    // sleep + activity_recognition ride Google Play Services' on-device classifiers (Sleep API /
    // ActivityRecognition) — content-free labels, no model runs in-app and no raw sensor leaves the
    // device. They no-op on devices without Play Services (e.g. Fire OS). health_connect reads the
    // system Health Connect store (steps/heart-rate/distance/calories/sleep/exercise records).
    SLEEP("sleep", CollectionPrivacyClass.HEALTH_METRICS, true),
    ACTIVITY_RECOGNITION("activity_recognition", CollectionPrivacyClass.BEHAVIORAL_METADATA, true, defaultEnabledOverride = false),
    HEALTH_CONNECT("health_connect", CollectionPrivacyClass.HEALTH_METRICS, true),
    // Content-free device/network state. connectivity_state — transport (WiFi/cellular/…) +
    // metered/connected transitions (no SSID/BSSID, which would be a location proxy).
    // app_network_usage — per-app tx/rx byte *counts* from NetworkStatsManager (reuses the Usage
    // Access permission; never payloads, destinations, or URLs). device_settings — a curated
    // snapshot of display/sound/accessibility/system toggles (dark mode, font scale, DND, battery
    // saver, thermal, storage, …); content-free.
    CONNECTIVITY_STATE("connectivity_state", CollectionPrivacyClass.DEVICE_STATE_METADATA, true, defaultEnabledOverride = false),
    APP_NETWORK_USAGE("app_network_usage", CollectionPrivacyClass.BEHAVIORAL_METADATA, true, defaultEnabledOverride = false),
    DEVICE_SETTINGS("device_settings", CollectionPrivacyClass.DEVICE_STATE_METADATA, true, defaultEnabledOverride = false),

    // ----- Reserved (inactive) modules (design §1A.3) -----
    TIME_USE_DIARY("time_use_diary", CollectionPrivacyClass.BEHAVIORAL_METADATA, false),
    APP_INVENTORY("app_inventory", CollectionPrivacyClass.DEVICE_CAPABILITY, false),
    // Sensing expansion — future eye-tracking. Camera-derived gaze / visual-attention estimates,
    // designed to fuse with interaction_events (shared episode/timestamp). Namespace-frozen ahead
    // of the :collection-gaze module + its camera consent path; never offered/gated/collected yet.
    GAZE_TRACKING("gaze_tracking", CollectionPrivacyClass.GAZE_TELEMETRY, false),
    // The content-bearing counterpart of interaction_events: the *text* of interacted elements,
    // read from the accessibility tree. SCREEN_CONTENT-class (highest sensitivity). Reserved
    // ahead of an explicit IRB consent path; never offered/gated/collected yet.
    INTERACTION_CONTENT("interaction_content", CollectionPrivacyClass.SCREEN_CONTENT, false),
    // Future opt-in location/mobility. Coarse or precise GPS/network position, geofence
    // transitions, mobility traces. PRECISE_LOCATION-class (most re-identifying tier). Skeleton
    // only — namespace-frozen ahead of a :collection-location module + its location-permission /
    // IRB consent path; never offered, gated, or collected yet.
    LOCATION("location", CollectionPrivacyClass.PRECISE_LOCATION, false),
    // Future opt-in communication-log metadata. Call/SMS counts, direction, timing, duration with
    // hashed identifiers — never message bodies or call audio. COMMUNICATION_METADATA-class
    // (reveals a third-party social graph). Skeleton only — namespace-frozen ahead of a
    // :collection-communication module + its CALL_LOG/SMS permission + IRB consent path; never
    // offered, gated, or collected yet. May later split into separate call/sms modules.
    COMMUNICATION_LOG("communication_log", CollectionPrivacyClass.COMMUNICATION_METADATA, false),

    // ----- Retired modules (kept for wire-decode compatibility only) -----
    // The hardware_sensors umbrella was split into the per-sensor SENSOR_* modules above
    // (per-sensor consent redesign, 2026-06-11). active = false: it is never registered,
    // gated, offered, or persisted afresh — it survives only so legacy persisted settings
    // and acknowledgments containing "hardware_sensors" still deserialize via [fromId]
    // instead of throwing. New writers use the per-sensor modules.
    HARDWARE_SENSORS("hardware_sensors", CollectionPrivacyClass.PHYSICAL_TELEMETRY, false),
    ;

    public companion object {
        private val BY_ID: Map<String, CollectionModuleId> = entries.associateBy { it.id }

        /**
         * Resolves a wire string to a [CollectionModuleId], or `null` if unknown.
         *
         * Used by deserialization to ignore unknown module IDs rather than fail
         * (design §1B.2). This is intentionally a nullable lookup, not a throwing one.
         */
        @JvmStatic
        public fun fromIdOrNull(id: String): CollectionModuleId? = BY_ID[id]

        /**
         * Jackson key/value deserialization entry point.
         *
         * Unknown IDs throw here only when used as a *value*; map-key decoding and
         * tolerant settings parsing go through [fromIdOrNull] instead.
         */
        @JvmStatic
        @JsonCreator
        public fun fromId(id: String): CollectionModuleId =
            BY_ID[id] ?: throw IllegalArgumentException("Unknown CollectionModuleId: $id")

        /** All currently active module IDs. */
        @JvmStatic
        public val activeModules: Set<CollectionModuleId> = entries.filter { it.active }.toSet()
    }
}
