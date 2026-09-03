package com.openlattice.chronicle.study

import java.time.OffsetDateTime
import java.util.UUID

public data class ParticipantQualityScore(
    val participantId: String,
    val androidScore: Double = 0.0,
    val iosScore: Double = 0.0,
    val tudScore: Double = 0.0,
    val overallScore: Double = 0.0,
    val androidDaysInWindow: Int = 0,
    val iosDaysInWindow: Int = 0,
    val tudDaysInWindow: Int = 0,
    val lastActivity: OffsetDateTime? = null,
)

public data class DataQualityAlert(
    val alertId: UUID,
    val studyId: UUID,
    val participantId: String,
    val alertType: String,
    val message: String,
    val score: Double,
    val createdAt: OffsetDateTime,
)

public data class DataQualityDashboard(
    val studyId: UUID,
    val overallCompleteness: Double = 0.0,
    val totalParticipants: Int = 0,
    val activeParticipants: Int = 0,
    val belowThreshold: Int = 0,
    val participantScores: List<ParticipantQualityScore> = emptyList(),
    val recentAlerts: List<DataQualityAlert> = emptyList(),
    val config: DataQualityConfig = DataQualityConfig(),
)
