package com.openlattice.chronicle.study

public data class DataQualityConfig(
    val expectedDaysPerWeek: Int = 5,
    val alertThresholdPercent: Int = 50,
    val evaluationWindowDays: Int = 14,
) : StudySetting
