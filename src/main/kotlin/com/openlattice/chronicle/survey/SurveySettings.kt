package com.openlattice.chronicle.survey

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.openlattice.chronicle.study.StudySetting

/**
 *
 * @author Matthew Tamayo-Rios &lt;matthew@getmethodic.com&gt;
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public data class SurveySettings(
    val appUsageThresholdInSeconds: Int = 3*60,
    val deviceUsageThresholdInSeconds: Int = 3*60,
) : StudySetting