package com.openlattice.chronicle.study

/**
 *
 * @author Matthew Tamayo-Rios &lt;matthew@getmethodic.com&gt;
 */
public data class ComplianceViolation(
    val reason: ViolationReason,
    val description: String
)

public enum class ViolationReason {
    NO_DATA_UPLOADED,
    NO_RECENT_DATA_UPLOADED,
    NOT_ENROLLED
}

