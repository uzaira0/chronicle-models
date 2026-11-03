package com.openlattice.chronicle.survey

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.openlattice.chronicle.study.StudySetting

/**
 *
 * @author Matthew Tamayo-Rios &lt;matthew@getmethodic.com&gt;
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public data class SurveySettings(
    /** An app is listed for a survey day once its use that day reaches this many seconds. */
    val appUsageThresholdInSeconds: Int = 60,
    val deviceUsageThresholdInSeconds: Int = 3*60,
) : StudySetting