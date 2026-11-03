package com.openlattice.chronicle.study

import com.openlattice.chronicle.settings.AppUsageFrequency

/** Researcher-selected reporting frequency, independent of device collection policy. */
public data class AppUsageSurveySetting(
    val appUsageFrequency: AppUsageFrequency = AppUsageFrequency.DAILY,
) : StudySetting
