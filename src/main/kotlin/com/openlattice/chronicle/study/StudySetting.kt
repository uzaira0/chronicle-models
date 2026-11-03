package com.openlattice.chronicle.study

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.openlattice.chronicle.android.AndroidSensorSetting
import com.openlattice.chronicle.collection.AndroidDataCollectionSetting
import com.openlattice.chronicle.notifications.StudyNotificationSettings
import com.openlattice.chronicle.survey.SurveySettings
import com.openlattice.chronicle.timeusediary.TimeUseDiarySettings

/**
 *
 * @author Matthew Tamayo-Rios &lt;matthew@getmethodic.com&gt;
 */
// Stored settings carry the class name as "@class"; only these subtypes are accepted.
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "@class")
@JsonSubTypes(
    JsonSubTypes.Type(value = AndroidDataCollectionSetting::class, name = "com.openlattice.chronicle.collection.AndroidDataCollectionSetting"),
    JsonSubTypes.Type(value = AndroidSensorSetting::class, name = "com.openlattice.chronicle.android.AndroidSensorSetting"),
    JsonSubTypes.Type(value = StudyNotificationSettings::class, name = "com.openlattice.chronicle.notifications.StudyNotificationSettings"),
    JsonSubTypes.Type(value = TimeUseDiarySettings::class, name = "com.openlattice.chronicle.timeusediary.TimeUseDiarySettings"),
    JsonSubTypes.Type(value = SurveySettings::class, name = "com.openlattice.chronicle.survey.SurveySettings"),
    JsonSubTypes.Type(value = StudyParticipantPolicy::class, name = "com.openlattice.chronicle.study.StudyParticipantPolicy"),
    JsonSubTypes.Type(value = StudyWithdrawalSetting::class, name = "com.openlattice.chronicle.study.StudyWithdrawalSetting"),
    JsonSubTypes.Type(value = AppUsageSurveySetting::class, name = "com.openlattice.chronicle.study.AppUsageSurveySetting"),
)
public interface StudySetting