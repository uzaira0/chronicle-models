package com.openlattice.chronicle.notifications

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.openlattice.chronicle.study.StudyDuration
import com.openlattice.chronicle.study.StudySetting
import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

/**
 *
 * @author Matthew Tamayo-Rios &lt;matthew@openlattice.com&gt;
 *
 * This class holds default settings for sending participant notifications.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public data class StudyNotificationSettings(
    @field:NotNull(message = "Lab friendly name is required")
    @field:Size(max = 255, message = "Lab friendly name exceeds maximum length")
    val labFriendlyName: String,

    @field:NotNull(message = "Study friendly name is required")
    @field:Size(max = 255, message = "Study friendly name exceeds maximum length")
    val studyFriendlyName: String,

    val notifyResearchers: Boolean = false,

    val notifyOnEnrollment: Boolean = false,

    @field:Size(max = 500, message = "Researcher phone numbers exceeds maximum length")
    val researcherPhoneNumbers: String = "",

    @field:Valid
    val noDataUploaded: StudyDuration = StudyDuration(days = 1),

    @field:Valid
    val noTudSubmitted: StudyDuration = StudyDuration(days = 1),

    @field:Valid
    val noAppUsageSurveySubmitted: StudyDuration = StudyDuration(days = 1)
) : StudySetting {

    @JsonIgnore
    public fun getEnrollmentMessage(): String {
        return if (labFriendlyName.isBlank()) {
            "You've been invited to enroll in $studyFriendlyName. "
        } else {
            "You've been invited by $labFriendlyName to enroll in the $studyFriendlyName"
        }
    }
}
