package com.openlattice.chronicle.collection

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

/**
 * One bounded, redacted Android upload-diagnostic aggregate.
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
    /** Local calendar day used for bounded aggregation. */
    public val day: LocalDate,
    /** Closed Android upload family such as USAGE_LIFECYCLE or DEVICE_TELEMETRY. */
    public val moduleFamily: String,
    /** Closed, redacted failure category. */
    public val issueCode: String,
    /** Number of equivalent failures represented by this aggregate. */
    public val count: Int,
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
        require(issueCode in ISSUE_CODES) { "Invalid diagnostic issue code" }
        require(count > 0) { "Diagnostic count must be positive" }
        require(!lastOccurredAt.isBefore(firstOccurredAt)) {
            "Diagnostic lastOccurredAt must not precede firstOccurredAt"
        }
        require(httpStatus == null || httpStatus in 100..599) { "Invalid diagnostic HTTP status" }
        require(errorType == null || errorType.length <= 128) { "Diagnostic errorType is too long" }
    }

    private companion object {
        private val MODULE_FAMILIES = setOf(
            "USAGE_LIFECYCLE",
            "BATTERY",
            "DEVICE_TELEMETRY",
        )
        private val ISSUE_CODES = setOf(
            "DESTINATION_MISSING",
            "DESTINATION_IDENTITY_MISMATCH",
            "DESTINATION_SOURCE_DEVICE_MISSING",
            "DESTINATION_SETUP_INCOMPLETE",
            "DESTINATION_DISABLED",
            "DESTINATION_NONCANONICAL",
            "DESTINATION_CREDENTIAL_INCOMPLETE",
            "HTTP_SERVER_ERROR",
            "HTTP_CLIENT_ERROR",
            "TIMEOUT",
            "DNS_FAILURE",
            "TLS_FAILURE",
            "CONNECTION_FAILURE",
            "UPLOAD_FAILURE",
        )
    }
}
