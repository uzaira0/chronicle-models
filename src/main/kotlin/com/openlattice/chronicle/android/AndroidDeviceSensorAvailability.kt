package com.openlattice.chronicle.android

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.OffsetDateTime

/** Why Chronicle cannot passively report the participant's exact pointer coordinates. */
public enum class InteractionPointerCaptureCapability {
    /** This OS predates AccessibilityService generic MotionEvent observation (API 34). */
    PLATFORM_API_UNAVAILABLE,

    /** The OS API exists, but selecting the touchscreen source prevents delivery to applications. */
    REQUIRES_INPUT_INTERCEPTION,
}

/**
 * A device's reported hardware/display capability profile (the `sensor_availability` module).
 *
 * Besides which modeled sensors the device exposes, this report carries the device's **static
 * display context** — screen resolution + density + natural display rotation. That context is
 * what makes raw on-screen pixel coordinates (e.g. from `interaction_events`) and orientation
 * signals (e.g. from `sensor_screen_orientation`) interpretable, so it is captured here **at
 * least once** as a general device-capability report, independent of whether any pixel-capturing
 * module is enabled. The display fields are nullable + additive (older clients omit them; old
 * servers ignore them via `@JsonIgnoreProperties`).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public data class AndroidDeviceSensorAvailability(
    val participantId: String = "",
    val deviceId: String = "",
    val availableSensors: Set<AndroidSensorType> = emptySet(),
    val unavailableSensors: Set<AndroidSensorType> = emptySet(),
    /** Display width in pixels at report time (`DisplayMetrics.widthPixels`); null if unavailable. */
    val screenWidthPixels: Int? = null,
    /** Display height in pixels at report time (`DisplayMetrics.heightPixels`); null if unavailable. */
    val screenHeightPixels: Int? = null,
    /** Display density in DPI (`DisplayMetrics.densityDpi`); null if unavailable. */
    val screenDensityDpi: Int? = null,
    /** Display rotation as a `Surface.ROTATION_*` ordinal (0..3); null if unavailable. */
    val displayRotation: Int? = null,
    /**
     * Device/API-specific reason exact finger coordinates are unavailable to Chronicle's
     * non-interfering collector. Null on older clients that did not report this capability.
     */
    val interactionPointerCaptureCapability: InteractionPointerCaptureCapability? = null,
    val reportedAt: OffsetDateTime? = null
)
