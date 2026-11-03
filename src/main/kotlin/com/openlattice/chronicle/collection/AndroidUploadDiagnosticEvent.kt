package com.openlattice.chronicle.collection

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.openlattice.chronicle.generated.UploadDiagnosticConstraints.MAX_ERROR_TYPE_LENGTH
import com.openlattice.chronicle.generated.UploadDiagnosticConstraints.MAX_ISSUE_CODE_LENGTH
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

/**
 * One bounded, redacted Android diagnostic occurrence: [count] equivalent failures at one instant.
 *
 * The phone cannot see a pause or archive window the server applies, so it never merges occurrences
 * of different instants; the server filters each event by its participation window and aggregation
 * happens only after that.
 *
 * Study, participant, device, and destination identity are supplied by the authenticated v4
 * request path and connection and are deliberately not duplicated in this body. Credentials,
 * destination URLs, unrestricted exception messages, and stack traces are never valid diagnostic
 * fields. Failure detail is limited to closed issue codes, HTTP status, and a bounded exception
 * class name.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public data class AndroidUploadDiagnosticEvent(
    /** Client-generated idempotency identifier. */
    public val id: String,
    /** Local calendar day of the occurrence. */
    public val day: LocalDate,
    /** Closed Android upload family such as USAGE_LIFECYCLE or DEVICE_TELEMETRY. */
    public val moduleFamily: String,
    /** Redacted failure category; see [recognizedIssueCode]. */
    public val issueCode: String,
    /** Number of equivalent failures observed at this instant. */
    public val count: Int,
    /** The occurrence instant; equal to [lastOccurredAt]. */
    public val firstOccurredAt: OffsetDateTime,
    public val lastOccurredAt: OffsetDateTime,
    /** HTTP response status when one was received. */
    public val httpStatus: Int? = null,
    /** Bounded exception type without a stack trace. */
    public val errorType: String? = null,
) {
    init {
        require(runCatching { UUID.fromString(id) }.isSuccess) { "Diagnostic id must be a UUID" }
        require(moduleFamily in MODULE_FAMILIES) { "Invalid diagnostic module family" }
        require(issueCode.codePointCount(0, issueCode.length) <= MAX_ISSUE_CODE_LENGTH) { "Diagnostic issueCode is too long" }
        require(count > 0) { "Diagnostic count must be positive" }
        require(lastOccurredAt.isEqual(firstOccurredAt)) {
            "A diagnostic event is one instant; firstOccurredAt must equal lastOccurredAt"
        }
        require(httpStatus == null || httpStatus in 100..599) { "Invalid diagnostic HTTP status" }
        require(errorType == null || errorType.codePointCount(0, errorType.length) <= MAX_ERROR_TYPE_LENGTH) { "Diagnostic errorType is too long" }
    }

    /** A code this build does not know, from a newer app, is stored as UNRECOGNIZED_ISSUE instead of refusing the batch. */
    public fun recognizedIssueCode(): AndroidUploadIssueCode = try {
        AndroidUploadIssueCode.valueOf(issueCode)
    } catch (_: IllegalArgumentException) {
        AndroidUploadIssueCode.UNRECOGNIZED_ISSUE
    }

    private companion object {
        private val MODULE_FAMILIES = setOf(
            "USAGE_LIFECYCLE",
            "BATTERY",
            "DEVICE_TELEMETRY",
            "SENSOR",
            "APP_RUNTIME",
            "INTERACTION",
            "AUDIO_ACTIVITY",
            "AUDIO_CONTENT",
            "NOTIFICATION",
            "SLEEP",
            "ACTIVITY_RECOGNITION",
            "HEALTH",
            "CONNECTIVITY",
            "APP_NETWORK",
            "DEVICE_SETTINGS",
            "LOCAL_STORE",
        )
    }
}
