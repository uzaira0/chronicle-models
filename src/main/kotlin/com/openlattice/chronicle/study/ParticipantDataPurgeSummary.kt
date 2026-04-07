package com.openlattice.chronicle.study

import java.time.OffsetDateTime
import java.util.UUID

public data class ParticipantDataPurgeSummary(
    val studyId: UUID,
    val participantId: String,
    val usageEventsCount: Long = 0,
    val usageStatsCount: Long = 0,
    val preprocessedEventsCount: Long = 0,
    val sensorDataCount: Long = 0,
    val androidSensorDataCount: Long = 0,
    val appUsageSurveyCount: Long = 0,
    val questionnaireSubmissionsCount: Long = 0,
    val tudSubmissionsCount: Long = 0,
    val participantStatsCount: Long = 0,
    val uploadBufferCount: Long = 0,
    val totalRows: Long = usageEventsCount + usageStatsCount + preprocessedEventsCount + sensorDataCount +
            androidSensorDataCount + appUsageSurveyCount + questionnaireSubmissionsCount + tudSubmissionsCount +
            participantStatsCount + uploadBufferCount,
    val confirmationToken: String = "",
    val tokenExpiresAt: OffsetDateTime? = null,
)
