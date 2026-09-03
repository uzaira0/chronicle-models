package com.openlattice.chronicle.collection

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.OffsetDateTime

/** Device thermal state, from `PowerManager.getCurrentThermalStatus()`. */
public enum class ThermalStatus { NONE, LIGHT, MODERATE, SEVERE, CRITICAL, EMERGENCY, SHUTDOWN, UNKNOWN }

/** Ringer mode, from `AudioManager.getRingerMode()`. Content-free; just how the device is set. */
public enum class RingerMode { NORMAL, SILENT, VIBRATE, UNKNOWN }

/**
 * One content-free device-settings snapshot produced by the `device_settings` collection module.
 *
 * A curated set of display / sound / accessibility / system toggles read from framework getters and
 * `Settings.*`. Every field is content-free and identity-free; nothing here reveals what the
 * participant does, only how the device is configured. All fields are nullable so a partial snapshot
 * (a value unavailable on a given OS/OEM) still serializes. `DEVICE_STATE_METADATA`-class, opt-in
 * (default OFF). No `apiKey`, signing secret, or `participantId` field. Unknown JSON fields are
 * ignored for forward compatibility.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public data class AndroidDeviceSettingsEvent(
    /** Stable per-event identifier, used for de-duplication on insert/upload. */
    public val id: String,
    /** Snapshot time in UTC. */
    public val timestamp: OffsetDateTime,
    /** Device-default time-zone id at sample time. */
    public val timezone: String,
    /** Whether system dark/night mode is active (`UiModeManager`); when known. */
    public val darkMode: Boolean? = null,
    /** Font scale (`Configuration.fontScale`); when known. Must be positive. */
    public val fontScale: Float? = null,
    /** Whether any accessibility service is enabled; when known. */
    public val accessibilityEnabled: Boolean? = null,
    /** Whether Do-Not-Disturb / a non-`ALL` interruption filter is active; when known. */
    public val dndActive: Boolean? = null,
    /** Whether battery-saver / power-save mode is active (`PowerManager`); when known. */
    public val batterySaver: Boolean? = null,
    /** Current thermal status; when known. */
    public val thermalStatus: ThermalStatus? = null,
    /** Whether auto-rotate is enabled (`Settings.System.ACCELEROMETER_ROTATION`); when known. */
    public val autoRotate: Boolean? = null,
    /** Whether location services are toggled on (the OS switch, NOT a location); when known. */
    public val locationServicesEnabled: Boolean? = null,
    /** Free internal storage in bytes; when known. Non-negative. */
    public val storageFreeBytes: Long? = null,
    /** Total internal storage in bytes; when known. At least 1. */
    public val storageTotalBytes: Long? = null,
    /**
     * Screen brightness level (`Settings.System.SCREEN_BRIGHTNESS`, typically 0–255); when known.
     * Only directly meaningful when [screenBrightnessAuto] is false — in adaptive mode this is the
     * last manual anchor, not the displayed brightness. Non-negative.
     */
    public val screenBrightness: Int? = null,
    /** Whether adaptive/automatic brightness is on (`SCREEN_BRIGHTNESS_MODE`); when known. */
    public val screenBrightnessAuto: Boolean? = null,
    /** Media/music stream volume step (`AudioManager.STREAM_MUSIC`); when known. Non-negative. */
    public val mediaVolume: Int? = null,
    /** Max media stream volume on this device — makes [mediaVolume] interpretable. Non-negative. */
    public val mediaVolumeMax: Int? = null,
    /** Ringtone stream volume step (`AudioManager.STREAM_RING`); when known. Non-negative. */
    public val ringVolume: Int? = null,
    /** Max ringtone stream volume on this device. Non-negative. */
    public val ringVolumeMax: Int? = null,
    /** Notification stream volume step (`AudioManager.STREAM_NOTIFICATION`); when known. Non-negative. */
    public val notificationVolume: Int? = null,
    /** Max notification stream volume on this device. Non-negative. */
    public val notificationVolumeMax: Int? = null,
    /** Alarm stream volume step (`AudioManager.STREAM_ALARM`); when known. Non-negative. */
    public val alarmVolume: Int? = null,
    /** Max alarm stream volume on this device. Non-negative. */
    public val alarmVolumeMax: Int? = null,
    /** Ringer mode (normal / vibrate / silent), from `AudioManager.getRingerMode()`; when known. */
    public val ringerMode: RingerMode? = null,
) {
    init {
        require(id.isNotBlank()) { "AndroidDeviceSettingsEvent.id must not be blank" }
        require(timezone.isNotBlank()) { "AndroidDeviceSettingsEvent.timezone must not be blank" }
        fontScale?.let { require(it > 0f) { "AndroidDeviceSettingsEvent.fontScale must be positive: $it" } }
        storageFreeBytes?.let { require(it >= 0) { "AndroidDeviceSettingsEvent.storageFreeBytes must be non-negative: $it" } }
        storageTotalBytes?.let { require(it >= 1) { "AndroidDeviceSettingsEvent.storageTotalBytes must be at least 1: $it" } }
        screenBrightness?.let { require(it >= 0) { "AndroidDeviceSettingsEvent.screenBrightness must be non-negative: $it" } }
        mediaVolume?.let { require(it >= 0) { "AndroidDeviceSettingsEvent.mediaVolume must be non-negative: $it" } }
        mediaVolumeMax?.let { require(it >= 0) { "AndroidDeviceSettingsEvent.mediaVolumeMax must be non-negative: $it" } }
        ringVolume?.let { require(it >= 0) { "AndroidDeviceSettingsEvent.ringVolume must be non-negative: $it" } }
        ringVolumeMax?.let { require(it >= 0) { "AndroidDeviceSettingsEvent.ringVolumeMax must be non-negative: $it" } }
        notificationVolume?.let { require(it >= 0) { "AndroidDeviceSettingsEvent.notificationVolume must be non-negative: $it" } }
        notificationVolumeMax?.let { require(it >= 0) { "AndroidDeviceSettingsEvent.notificationVolumeMax must be non-negative: $it" } }
        alarmVolume?.let { require(it >= 0) { "AndroidDeviceSettingsEvent.alarmVolume must be non-negative: $it" } }
        alarmVolumeMax?.let { require(it >= 0) { "AndroidDeviceSettingsEvent.alarmVolumeMax must be non-negative: $it" } }
    }
}
