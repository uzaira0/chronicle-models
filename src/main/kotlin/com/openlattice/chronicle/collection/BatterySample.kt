package com.openlattice.chronicle.collection

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.OffsetDateTime

/** Charging state of the device battery at sample time. */
public enum class BatteryChargingState {
    UNKNOWN,
    CHARGING,
    DISCHARGING,
    NOT_CHARGING,
    FULL,
}

/** What the device is plugged into at sample time. */
public enum class BatteryPlugType {
    UNPLUGGED,
    AC,
    USB,
    WIRELESS,
    DOCK,
}

/** Reported battery health at sample time. */
public enum class BatteryHealth {
    UNKNOWN,
    GOOD,
    OVERHEAT,
    DEAD,
    OVER_VOLTAGE,
    UNSPECIFIED_FAILURE,
    COLD,
}

/**
 * One battery-telemetry sample produced by the `battery_telemetry` collection module
 * (see `docs/SENSING-EXPANSION-DESIGN.md` §5).
 *
 * This is the shared **wire contract** between the Android `:collection-battery`
 * module and the server. It is `DEVICE_STATE_METADATA`-class data — no participant
 * content — so it carries **no** `apiKey`, signing secret, or `participantId` field.
 *
 * Units follow the Android `BatteryManager` / `ACTION_BATTERY_CHANGED` conventions:
 * [temperatureDeciC] is tenths of a degree Celsius (it may be negative), and
 * [voltageMillivolts] is millivolts.
 *
 * Validation (consistent with the other collection DTOs): [levelPercent] is a
 * percentage in `0..100` and [voltageMillivolts] is non-negative. An out-of-range
 * value is rejected, not clamped.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public data class BatterySample(
    /** Stable per-sample identifier, used for de-duplication on insert/upload. */
    public val id: String,
    /** Sample time in UTC. */
    public val timestamp: OffsetDateTime,
    /** Device-default time-zone id at sample time. */
    public val timezone: String,
    /** Battery charge level as a percentage, `0..100`. */
    public val levelPercent: Int,
    /** Charging state at sample time. */
    public val chargingState: BatteryChargingState,
    /** What the device is plugged into at sample time. */
    public val plugType: BatteryPlugType,
    /** Battery temperature in tenths of a degree Celsius; may be negative. */
    public val temperatureDeciC: Int,
    /** Battery voltage in millivolts; non-negative. */
    public val voltageMillivolts: Int,
    /** Reported battery health at sample time. */
    public val health: BatteryHealth,
) {
    init {
        require(levelPercent in 0..100) {
            "BatterySample.levelPercent must be in 0..100: $levelPercent"
        }
        require(voltageMillivolts >= 0) {
            "BatterySample.voltageMillivolts must be non-negative: $voltageMillivolts"
        }
    }
}
