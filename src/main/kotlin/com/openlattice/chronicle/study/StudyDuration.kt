package com.openlattice.chronicle.study

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

/**
 * @author Matthew Tamayo-Rios &lt;matthew@openlattice.com&gt;
 */
public data class StudyDuration(
    @field:Min(value = 0, message = "Years cannot be negative")
    @field:Max(value = 100, message = "Years exceeds maximum allowed")
    val years: Short = 0,

    @field:Min(value = 0, message = "Months cannot be negative")
    @field:Max(value = 11, message = "Months must be between 0 and 11")
    val months: Short = 0,

    @field:Min(value = 0, message = "Days cannot be negative")
    @field:Max(value = 365, message = "Days exceeds maximum allowed")
    val days: Short = 0,
)