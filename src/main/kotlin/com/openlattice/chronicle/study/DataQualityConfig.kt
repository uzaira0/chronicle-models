package com.openlattice.chronicle.study

public data class DataQualityConfig(
    val expectedDaysPerWeek: Int = 5,
    val alertThresholdPercent: Int = 50,
    val evaluationWindowDays: Int = 14,
) : StudySetting {
    init {
        require(expectedDaysPerWeek in 1..7) { "expectedDaysPerWeek must be between 1 and 7" }
        require(alertThresholdPercent in 1..100) { "alertThresholdPercent must be between 1 and 100" }
        require(evaluationWindowDays in 1..365) { "evaluationWindowDays must be between 1 and 365" }
    }
}
