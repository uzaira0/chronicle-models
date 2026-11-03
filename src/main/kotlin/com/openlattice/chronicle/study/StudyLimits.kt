package com.openlattice.chronicle.study

import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import java.time.OffsetDateTime

/**
 *
 * @author Matthew Tamayo-Rios &lt;matthew@openlattice.com&gt;
 *
 * This class describes the default limits (quotas) placed on a study.
 *
 * @param studyDuration How long a study is allowed to run for, or null when the researcher set no end.
 * @param dataRetentionDuration Time after study end before the dashboard and monitoring request explicit deletion.
 * @param studyEnds The end the researcher set; null means the study runs until someone stops it.
 * @param studyDataExpires The retention deadline; access continues until explicit deletion. Null without an end.
 *
 */
public data class StudyLimits(
    @field:Valid
    val studyDuration: StudyDuration? = null,

    @field:Valid
    @field:NotNull(message = "Data retention duration is required")
    val dataRetentionDuration: StudyDuration = StudyDuration(days = 90),

    val studyEnds: OffsetDateTime? = studyDuration?.let {
        OffsetDateTime.now()
            .plusYears(it.years.toLong())
            .plusMonths(it.months.toLong())
            .plusDays(it.days.toLong())
    },

    val studyDataExpires: OffsetDateTime? = studyEnds
        ?.plusYears(dataRetentionDuration.years.toLong())
        ?.plusMonths(dataRetentionDuration.months.toLong())
        ?.plusDays(dataRetentionDuration.days.toLong()),
)
