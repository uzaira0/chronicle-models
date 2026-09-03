package com.openlattice.chronicle.study

import java.util.*

public data class StudyDataSummary(
    val studyId: UUID,
    val participantCount: Long = 0,
    val usageEventsCount: Long = 0,
    val preprocessedEventsCount: Long = 0,
    val sensorDataCount: Long = 0,
    val androidSensorDataCount: Long = 0,
    val appUsageSurveyCount: Long = 0,
    val questionnaireSubmissionsCount: Long = 0,
    val tudSubmissionsCount: Long = 0,
    val lifecycleStatus: StudyLifecycleStatus = StudyLifecycleStatus.ACTIVE
)
