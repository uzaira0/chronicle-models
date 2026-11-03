package com.openlattice.chronicle.survey

import com.openlattice.chronicle.study.StudySetting

/**
 *
 * @author Matthew Tamayo-Rios &lt;matthew@getmethodic.com&gt;
 */
public data class SurveySettings(
    /** An app is listed for a survey day once its use that day reaches this many seconds. */
    val appUsageThresholdInSeconds: Int = 60,
    val deviceUsageThresholdInSeconds: Int = 3*60,
) : StudySetting