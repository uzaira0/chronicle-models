package com.openlattice.chronicle.android

import com.openlattice.chronicle.generated.CollectionDefaultValues
import com.openlattice.chronicle.study.StudySetting

public data class AndroidSensorSetting(
        val sensors: Set<AndroidSensorType> = emptySet(),
        val samplingRateHz: Int = CollectionDefaultValues.SENSOR_RATE_HZ,
        val dutyCycleActiveSeconds: Int = CollectionDefaultValues.SENSOR_DUTY_ACTIVE_SECONDS,
        val dutyCyclePeriodSeconds: Int = CollectionDefaultValues.SENSOR_DUTY_PERIOD_SECONDS
) : StudySetting {
    public companion object {
        public val NO_SENSORS: AndroidSensorSetting = AndroidSensorSetting()
    }
}
