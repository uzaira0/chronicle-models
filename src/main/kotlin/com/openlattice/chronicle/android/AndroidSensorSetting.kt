package com.openlattice.chronicle.android

import com.openlattice.chronicle.study.StudySetting

public data class AndroidSensorSetting(
        val sensors: Set<AndroidSensorType> = emptySet(),
        val samplingRateHz: Int = 5,
        val dutyCycleActiveSeconds: Int = 30,
        val dutyCyclePeriodSeconds: Int = 300
) : StudySetting {
    public companion object {
        public val NO_SENSORS: AndroidSensorSetting = AndroidSensorSetting()
    }
}
