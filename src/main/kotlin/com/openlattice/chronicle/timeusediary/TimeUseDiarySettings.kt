package com.openlattice.chronicle.timeusediary

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.openlattice.chronicle.study.StudySetting

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public data class TimeUseDiarySettings(
    val enableChangesForSherbrookeUniversity: Boolean = false,
    val enableChangesForOhioStateUniversity: Boolean = false,
    val language: String = "en",
    // 12- or 24-hour clock for the diary UI. Upstream parity (methodic-labs):
    // `clockFormatLocked` prevents the participant from switching the format.
    val clockFormat: Int = 12,
    val clockFormatLocked: Boolean = false
) : StudySetting
