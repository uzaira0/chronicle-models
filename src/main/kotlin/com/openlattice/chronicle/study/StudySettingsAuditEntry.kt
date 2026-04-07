package com.openlattice.chronicle.study

import java.time.OffsetDateTime
import java.util.UUID

public data class StudySettingsAuditEntry(
    val id: UUID = UUID.randomUUID(),
    val studyId: UUID,
    val changedBy: String,
    val changedAt: OffsetDateTime = OffsetDateTime.now(),
    val sourceIp: String? = null,
    val settingKey: StudySettingType,
    val beforeValue: Map<String, Any?>? = null,
    val afterValue: Map<String, Any?>,
    val changeSummary: String
)
