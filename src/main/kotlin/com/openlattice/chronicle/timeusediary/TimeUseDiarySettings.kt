package com.openlattice.chronicle.timeusediary

import com.openlattice.chronicle.study.StudySetting

public data class TimeUseDiarySettings(
    val enableChangesForSherbrookeUniversity: Boolean = false,
    val enableChangesForOhioStateUniversity: Boolean = false,
    val language: String = "en",
    // 12- or 24-hour clock for the diary UI. Upstream parity (methodic-labs):
    // `clockFormatLocked` prevents the participant from switching the format.
    val clockFormat: Int = 12,
    val clockFormatLocked: Boolean = false,
    val timeZone: String = "UTC",
) : StudySetting {
    init { require(timeZone in java.time.ZoneId.getAvailableZoneIds()) { "Time Use Diary requires an IANA time zone" } }
}
