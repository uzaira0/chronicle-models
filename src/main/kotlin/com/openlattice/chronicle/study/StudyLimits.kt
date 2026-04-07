package com.openlattice.chronicle.study

import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import java.time.OffsetDateTime
import java.util.*

/**
 *
 * @author Matthew Tamayo-Rios &lt;matthew@openlattice.com&gt;
 *
 * This class describes the default limits (quotas) placed on a study.
 *
 * @param studyDuration How long a study is allowed to run for
 * @param dataRetentionDuration The amount of time after a study ends that data will be retained by the platform.
 * @param studyEndDateTime The end datetime the study can run until.
 * @param studyDataExpirationDateTime The datetime at which the study will be expired.
 * @param participantLimit The number of participants allowed in a study.
 * @param features The modules enabled for this study.
 *
 */
public data class StudyLimits(
    @field:Valid
    @field:NotNull(message = "Study duration is required")
    val studyDuration: StudyDuration = StudyDuration(years = 1),

    @field:Valid
    @field:NotNull(message = "Data retention duration is required")
    val dataRetentionDuration: StudyDuration = StudyDuration(days = 90),

    @field:NotNull(message = "Study end date is required")
    val studyEnds: OffsetDateTime = OffsetDateTime.now()
        .plusYears(studyDuration.years.toLong())
        .plusMonths(studyDuration.months.toLong())
        .plusDays(studyDuration.days.toLong()),

    @field:NotNull(message = "Study data expiration date is required")
    val studyDataExpires: OffsetDateTime = studyEnds
        .plusYears(dataRetentionDuration.years.toLong())
        .plusMonths(dataRetentionDuration.months.toLong())
        .plusDays(dataRetentionDuration.days.toLong()),

    @field:Min(value = 1, message = "Participant limit must be at least 1")
    @field:Max(value = 100000, message = "Participant limit exceeds maximum allowed")
    val participantLimit: Int = 25,

    @field:NotNull(message = "Features are required")
    val features: EnumSet<StudyFeature> = EnumSet.of(
        StudyFeature.CHRONICLE,
        StudyFeature.CHRONICLE_DATA_COLLECTION,
        StudyFeature.CHRONICLE_SURVEYS
    ),
)