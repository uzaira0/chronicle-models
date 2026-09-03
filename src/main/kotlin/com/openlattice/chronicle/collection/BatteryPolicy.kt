package com.openlattice.chronicle.collection

/**
 * Battery-aware collection policy (design §1B.2).
 *
 * Validation (design §1B.4): percentage thresholds must be within `0..100`; an
 * out-of-range value is rejected rather than silently accepted.
 *
 * @author uzaira0
 */
public data class BatteryPolicy(
    /** Minimum battery level (percent) for the module to run. */
    val minLevelPercent: Int = 15,
    /** Battery level (percent) at or below which the module stops entirely. */
    val stopBelowCriticalPercent: Int = 5,
    /** Whether the module degrades (reduced cadence) in OS power-save mode. */
    val degradeInPowerSave: Boolean = true,
) {
    init {
        require(minLevelPercent in 0..100) {
            "BatteryPolicy.minLevelPercent must be in 0..100: $minLevelPercent"
        }
        require(stopBelowCriticalPercent in 0..100) {
            "BatteryPolicy.stopBelowCriticalPercent must be in 0..100: $stopBelowCriticalPercent"
        }
        require(stopBelowCriticalPercent <= minLevelPercent) {
            "BatteryPolicy.stopBelowCriticalPercent ($stopBelowCriticalPercent) must not exceed " +
                "minLevelPercent ($minLevelPercent)"
        }
    }

    public companion object {
        /** Conservative default policy. */
        public val DEFAULT: BatteryPolicy = BatteryPolicy()
    }
}
