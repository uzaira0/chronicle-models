package com.openlattice.chronicle.collection

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.OffsetDateTime

/**
 * One battery-telemetry sample produced by the iOS `battery_telemetry` collection
 * module.
 *
 * This is the shared **wire contract** between the iOS collector and the server.
 * It is `DEVICE_STATE_METADATA`-class data — no participant content — so it carries
 * **no** `apiKey`, signing secret, or `participantId` field.
 *
 * Deliberately slimmer than [BatterySample]: iOS exposes only the charge level, a
 * coarse charging state (`UIDevice.batteryState`), and Low Power Mode. Temperature,
 * voltage, health, and plug type do not exist on iOS and are stored as NULL server-side,
 * never fabricated.
 *
 * [chargingState] reuses [BatteryChargingState]; the iOS mapping is
 * unknown→UNKNOWN, unplugged→DISCHARGING, charging→CHARGING, full→FULL
 * (NOT_CHARGING is never produced on iOS).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public data class IosBatterySample(
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
    /** Whether Low Power Mode was active at sample time; null when unknown. */
    public val lowPowerMode: Boolean? = null,
) {
    init {
        require(levelPercent in 0..100) {
            "IosBatterySample.levelPercent must be in 0..100: $levelPercent"
        }
    }
}
