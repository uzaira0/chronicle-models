package com.openlattice.chronicle.collection

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.openlattice.chronicle.android.AndroidSensorSetting

/**
 * Per-module configuration entry inside an [AndroidDataCollectionSetting] (design §1B.2).
 *
 * The [enabled] flag, when not explicitly supplied, should default per the module's
 * privacy class (design §1A.4) — see [CollectionDefaults]. Privacy-sensitive modules
 * are never enabled implicitly.
 *
 * Unknown JSON fields are ignored to stay forward-compatible with newer schema
 * versions, consistent with existing Chronicle models (design §1B.4).
 *
 * Hard constraint: this DTO carries **no** `apiKey`, signing secret, or
 * `participantId` field — it is pure configuration.
 *
 * @author uzaira0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public data class CollectionModuleSetting(
    /** Whether the module is active. Defaults are privacy-class-driven (§1A.4). */
    val enabled: Boolean,
    /**
     * Whether the study **requires** this module: the participant must accept it to
     * enroll, and cannot turn it off afterwards (only uninstalling/withdrawing stops
     * it). `false` (the default) means **optional** — the participant may decline it at
     * enrollment and toggle it on/off at any time. Meaningful only when [enabled] is
     * `true`. Additive and backward-compatible: a setting that omits it is optional, and
     * old clients ignore it (`@JsonIgnoreProperties(ignoreUnknown = true)`).
     * See the per-module consent redesign, 2026-06-10.
     */
    val required: Boolean = false,
    /** How often the module collects data. */
    val collectionCadence: CollectionCadence = CollectionCadence.DEFAULT_COLLECTION,
    /** How often the module's data is uploaded. */
    val uploadCadence: CollectionCadence = CollectionCadence.DEFAULT_UPLOAD,
    /** Battery-aware policy. */
    val batteryPolicy: BatteryPolicy = BatteryPolicy.DEFAULT,
    /** Network constraints for upload. */
    val networkPolicy: NetworkPolicy = NetworkPolicy.DEFAULT,
    /**
     * Per-sensor sampling policy: the sampling rate (Hz) and duty cycle this sensor
     * module collects at. Only the per-sensor `sensor_*` modules populate this — each
     * carries its own rate/duty (web-editable, mobile read-only); every non-sensor module
     * leaves it `null`. `sensors` holds the single owning [AndroidSensorType]. (The field
     * also still decodes the legacy `hardware_sensors.sensorPolicy` shape for old data.)
     */
    val sensorPolicy: AndroidSensorSetting? = null,
    /**
     * Interaction-event policy. Only `interaction_events` populates this; every
     * other module leaves it `null` (see `docs/SENSING-EXPANSION-DESIGN.md` §3.3).
     */
    val interactionPolicy: InteractionPolicy? = null,
    /**
     * Exact Health Connect record families approved for this study. Meaningful only for the
     * `health_connect` module. Empty is privacy-safe: the client requests and reads no health
     * records, even if the module itself is enabled.
     */
    val healthConnectRecordTypes: Set<HealthConnectRecordType> = emptySet(),
    /**
     * For a mid-study **disable** ([enabled] = false), the researcher-chosen
     * disposition for data this module has already collected on-device but not yet
     * uploaded. Set only when [enabled] is `false`; left `null` (and ignored by the
     * device) while the module is enabled (design
     * `docs/COLLECTION-LOOP-CLOSURE-DESIGN.md` §5.1). Rides the existing settings
     * object — no separate transport channel — and is ignored by old app builds
     * (`@JsonIgnoreProperties(ignoreUnknown = true)`). The device acts on it only on
     * the `ACTIVE → INACTIVE` transition.
     */
    val disableDisposition: CollectionDataDisposition? = null,
) {
    public companion object {
        /** Android's normal-rate sensor ceiling; prevents zero-period/max-speed collection. */
        public const val MAX_SENSOR_SAMPLING_RATE_HZ: Int = 200
    }

    init {
        // Validate the legacy sensor policy at this boundary. AndroidSensorSetting
        // itself is intentionally left unchanged (it has no init validation); the
        // new wrapping DTO rejects out-of-range values per design §1B.4 so a bad
        // sensor policy cannot enter the generalized setting silently.
        val policy = sensorPolicy
        if (policy != null) {
            require(policy.samplingRateHz in 1..MAX_SENSOR_SAMPLING_RATE_HZ) {
                "CollectionModuleSetting.sensorPolicy.samplingRateHz must be between 1 and " +
                    "$MAX_SENSOR_SAMPLING_RATE_HZ: " +
                    "${policy.samplingRateHz}"
            }
            require(policy.dutyCycleActiveSeconds >= 0) {
                "CollectionModuleSetting.sensorPolicy.dutyCycleActiveSeconds must be non-negative: " +
                    "${policy.dutyCycleActiveSeconds}"
            }
            require(policy.dutyCyclePeriodSeconds > 0) {
                "CollectionModuleSetting.sensorPolicy.dutyCyclePeriodSeconds must be positive: " +
                    "${policy.dutyCyclePeriodSeconds}"
            }
            require(policy.dutyCycleActiveSeconds <= policy.dutyCyclePeriodSeconds) {
                "CollectionModuleSetting.sensorPolicy.dutyCycleActiveSeconds " +
                    "(${policy.dutyCycleActiveSeconds}) must not exceed dutyCyclePeriodSeconds " +
                    "(${policy.dutyCyclePeriodSeconds})"
            }
        }
    }
}
