package com.openlattice.chronicle.collection

import com.openlattice.chronicle.android.AndroidSensorType

/**
 * The bijection between an [AndroidSensorType] and its dedicated [CollectionModuleId]
 * (per-sensor consent redesign, 2026-06-11).
 *
 * Each hardware sensor is its own first-class collection module — the study marks it
 * required / optional / unavailable and the participant consents to each one
 * individually, exactly like the App & Device Usage modules. There is no grouped
 * "hardware sensors" module; the retired [CollectionModuleId.HARDWARE_SENSORS] survives
 * only as a wire-decode alias for legacy persisted data.
 *
 * This object is the single source of truth tying the sensor catalog
 * ([AndroidSensorType]) to the module catalog ([CollectionModuleId]); the runtime, the
 * settings resolver, the legacy bridge, and the UI all route through it rather than
 * hard-coding the pairing.
 */
public object SensorCollectionModules {

    /**
     * Every [AndroidSensorType] mapped to its dedicated module id. **Total** — every sensor has
     * one, including the retired Samsung vendor sensors (kept here so any legacy persisted
     * `sensor_samsung_*` setting/ack and any raw vendor sample still decode). This is the
     * decode/classification bijection; the **active**, user-facing iteration sets
     * ([sensorModuleIds] / [sensorDisplayOrder]) filter out inactive modules.
     *
     * Declared in **relevance order** for screen-time research, which is the canonical display
     * order for every user-visible sensor list ([sensorModuleIds] / [sensorDisplayOrder] preserve
     * it, and the web study form mirrors it): the four high-context, low-power sensors first
     * (accelerometer, light, proximity, screen orientation), then the step counter (near-free on
     * battery, a sedentary covariate), then the raw motion/field sensors, the fused/derived ones,
     * the trigger sensors, and finally the device-specific Samsung sensors. Lookups
     * ([moduleFor] / [sensorTypeOf]) are order-independent; only iteration order is affected.
     */
    public val byType: Map<AndroidSensorType, CollectionModuleId> = mapOf(
        AndroidSensorType.accelerometer to CollectionModuleId.SENSOR_ACCELEROMETER,
        AndroidSensorType.light to CollectionModuleId.SENSOR_LIGHT,
        AndroidSensorType.proximity to CollectionModuleId.SENSOR_PROXIMITY,
        AndroidSensorType.screenOrientation to CollectionModuleId.SENSOR_SCREEN_ORIENTATION,
        AndroidSensorType.stepCounter to CollectionModuleId.SENSOR_STEP_COUNTER,
        AndroidSensorType.gyroscope to CollectionModuleId.SENSOR_GYROSCOPE,
        AndroidSensorType.magnetometer to CollectionModuleId.SENSOR_MAGNETOMETER,
        AndroidSensorType.gravity to CollectionModuleId.SENSOR_GRAVITY,
        AndroidSensorType.linearAcceleration to CollectionModuleId.SENSOR_LINEAR_ACCELERATION,
        AndroidSensorType.rotationVector to CollectionModuleId.SENSOR_ROTATION_VECTOR,
        AndroidSensorType.significantMotion to CollectionModuleId.SENSOR_SIGNIFICANT_MOTION,
        AndroidSensorType.tiltDetector to CollectionModuleId.SENSOR_TILT_DETECTOR,
        AndroidSensorType.samsungGripWifi to CollectionModuleId.SENSOR_SAMSUNG_GRIP_WIFI,
        AndroidSensorType.samsungMotion to CollectionModuleId.SENSOR_SAMSUNG_MOTION,
    )

    /** The reverse mapping: a sensor module id back to its [AndroidSensorType]. */
    public val typeOf: Map<CollectionModuleId, AndroidSensorType> =
        byType.entries.associate { (type, moduleId) -> moduleId to type }

    /**
     * **Every** per-sensor module id (every value of [byType]) — active and retired alike;
     * preserves [byType] order. This is the decode/classification set: [isSensorModule] uses it
     * so a retired `sensor_samsung_*` id is still recognized as a sensor module for legacy
     * decode, disposition-queue routing, and per-sensor-config detection. For the user-facing,
     * collectable sensors use [sensorModuleIds] instead.
     */
    public val allSensorModuleIds: Set<CollectionModuleId> = byType.values.toSet()

    /**
     * The **active** per-sensor module ids (every value of [byType] whose module is
     * [CollectionModuleId.active]); preserves [byType] order. Retired Samsung vendor sensors are
     * excluded. This is the set that drives every collectable/consent surface — the enrollment
     * wizard ([CollectionStateMachine.ACK_GATED_MODULES]), the hardware gate, and "does the study
     * collect any sensor". Decode/routing that must still see retired ids uses [allSensorModuleIds].
     */
    public val sensorModuleIds: Set<CollectionModuleId> =
        byType.values.filterTo(LinkedHashSet()) { it.active }

    /**
     * The **active** per-sensor module ids in canonical relevance order (see [byType]) — the single
     * display order every user-visible sensor list iterates: the Data Sharing tab, the enrollment
     * wizard, and (mirrored) the web study form. Retired sensors are excluded. Use [List.indexOf]
     * for a stable display rank.
     */
    public val sensorDisplayOrder: List<CollectionModuleId> = byType.values.filter { it.active }

    /** The dedicated module id for [type]. Total over [AndroidSensorType] (incl. retired sensors). */
    public fun moduleFor(type: AndroidSensorType): CollectionModuleId = byType.getValue(type)

    /** The [AndroidSensorType] [moduleId] collects, or `null` if it is not a sensor module. */
    public fun sensorTypeOf(moduleId: CollectionModuleId): AndroidSensorType? = typeOf[moduleId]

    /**
     * Whether [moduleId] is one of the per-sensor hardware modules — **including retired ones**
     * (uses [allSensorModuleIds]), so legacy decode, disposition routing, and per-sensor-config
     * detection still classify a `sensor_samsung_*` id correctly. To test for a *collectable*
     * sensor, check membership in [sensorModuleIds].
     */
    public fun isSensorModule(moduleId: CollectionModuleId): Boolean = moduleId in allSensorModuleIds
}
