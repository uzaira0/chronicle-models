package com.openlattice.chronicle.collection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class AndroidUploadDiagnosticEventTest {
    @Test
    fun `issue codes are bounded even when a newer app supplies them`() {
        assertEquals(AndroidUploadIssueCode.UNRECOGNIZED_ISSUE, fixture(issueCode = "X".repeat(128)).recognizedIssueCode())
        assertThrows(IllegalArgumentException::class.java) { fixture(issueCode = "X".repeat(129)) }
    }

    @Test
    fun `accepts 128 Unicode characters in a diagnostic code`() {
        val issueCode = "\uD83D\uDE00".repeat(128)
        assertEquals(AndroidUploadIssueCode.UNRECOGNIZED_ISSUE, fixture(issueCode = issueCode).recognizedIssueCode())
    }

    @Test
    fun `rejects 129 Unicode characters in a diagnostic code`() {
        assertThrows(IllegalArgumentException::class.java) { fixture(issueCode = "\uD83D\uDE00".repeat(129)) }
    }

    @Test
    fun `accepts 128 Unicode characters in a diagnostic error type`() {
        val errorType = "\uD83D\uDE00".repeat(128)
        assertEquals(errorType, fixture(errorType = errorType).errorType)
    }

    @Test
    fun `rejects 129 Unicode characters in a diagnostic error type`() {
        assertThrows(IllegalArgumentException::class.java) { fixture(errorType = "\uD83D\uDE00".repeat(129)) }
    }

    @Test
    fun `accepts only documented diagnostic categories`() {
        val event = fixture()

        assertEquals("USAGE_LIFECYCLE", event.moduleFamily)
        assertEquals("TIMEOUT", event.issueCode)
    }

    @Test
    fun `rejects arbitrary categories`() {
        assertThrows(IllegalArgumentException::class.java) {
            fixture(moduleFamily = "A_FUTURE_UNDECLARED_MODULE")
        }
    }

    @Test
    fun `accepts every retained diagnostic module family`() {
        listOf(
            "USAGE_LIFECYCLE", "BATTERY", "DEVICE_TELEMETRY", "SENSOR", "APP_RUNTIME",
            "INTERACTION", "AUDIO_ACTIVITY", "AUDIO_CONTENT", "NOTIFICATION", "SLEEP",
            "ACTIVITY_RECOGNITION", "HEALTH", "CONNECTIVITY", "APP_NETWORK", "DEVICE_SETTINGS",
            "LOCAL_STORE",
        ).forEach { family ->
            assertEquals(family, fixture(moduleFamily = family).moduleFamily)
        }
    }

    @Test
    fun `accepts every retained diagnostic issue code`() {
        listOf(
            "DESTINATION_MISSING", "DESTINATION_IDENTITY_MISMATCH", "DESTINATION_SOURCE_DEVICE_MISSING",
            "DESTINATION_SETUP_INCOMPLETE", "DESTINATION_DISABLED", "DESTINATION_NONCANONICAL",
            "DESTINATION_CREDENTIAL_INCOMPLETE", "HTTP_SERVER_ERROR", "HTTP_CLIENT_ERROR", "TIMEOUT",
            "DNS_FAILURE", "TLS_FAILURE", "CONNECTION_FAILURE", "UPLOAD_FAILURE",
            "APP_CRASH", "APP_CRASH_NATIVE", "APP_ANR",
            "SAMPLE_QUARANTINED", "LOCAL_BUFFER_OVERFLOW", "LOCAL_REQUEUE_OVERFLOW", "LOCAL_WRITE_FAILED",
            "LOCAL_SHUTDOWN_DROPPED", "COLLECTION_GATE_DROPPED", "MODULE_POLICY_ERASED",
            "DIRECT_BOOT_CAPACITY_DROPPED", "DIRECT_BOOT_CORRUPT_RECORD",
            "COLLECTION_PAUSED_STORAGE", "COLLECTION_ACCESS_MISSING",
        ).forEach { issueCode ->
            assertEquals(issueCode, fixture(issueCode = issueCode).recognizedIssueCode().name)
        }
    }

    /** A newer app's code, or text that is no code at all, is never stored as sent. */
    @Test
    fun `an issue code this build does not know is recognized as UNRECOGNIZED_ISSUE`() {
        listOf("A_CODE_FROM_A_NEWER_APP", "SENSOR_AGE_EXPIRED", "RAW_EXCEPTION_TEXT at com.example.Foo").forEach { issueCode ->
            assertEquals(AndroidUploadIssueCode.UNRECOGNIZED_ISSUE, fixture(issueCode = issueCode).recognizedIssueCode())
        }
    }

    /** A span cannot be split at a pause the phone never saw, so the server could not filter it. */
    @Test
    fun `rejects an event spanning more than one instant`() {
        assertThrows(IllegalArgumentException::class.java) {
            fixture(lastOccurredAt = OffsetDateTime.parse("2026-08-26T12:01:00Z"))
        }
        assertEquals(2, fixture(lastOccurredAt = OffsetDateTime.parse("2026-08-26T09:00:00-03:00")).count)
    }

    private fun fixture(
        moduleFamily: String = "USAGE_LIFECYCLE",
        issueCode: String = "TIMEOUT",
        lastOccurredAt: OffsetDateTime = OffsetDateTime.parse("2026-08-26T12:00:00Z"),
        errorType: String? = null,
    ): AndroidUploadDiagnosticEvent = AndroidUploadDiagnosticEvent(
        id = UUID.randomUUID().toString(),
        day = LocalDate.parse("2026-08-26"),
        moduleFamily = moduleFamily,
        issueCode = issueCode,
        count = 2,
        firstOccurredAt = OffsetDateTime.parse("2026-08-26T12:00:00Z"),
        lastOccurredAt = lastOccurredAt,
        errorType = errorType,
    )
}
