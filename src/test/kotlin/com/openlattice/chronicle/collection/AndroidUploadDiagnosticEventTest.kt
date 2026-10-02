package com.openlattice.chronicle.collection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class AndroidUploadDiagnosticEventTest {
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
        assertThrows(IllegalArgumentException::class.java) {
            fixture(issueCode = "RAW_EXCEPTION_TEXT")
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
            "SENSOR_SAMPLE_QUARANTINED", "SENSOR_DEAD_LETTER_DROPPED", "APP_CRASH", "APP_CRASH_NATIVE",
            "APP_ANR", "SENSOR_AGE_EXPIRED", "SENSOR_CAPACITY_DROPPED", "USAGE_QUEUE_EVICTED",
            "SAMPLE_QUARANTINED", "LOCAL_BUFFER_OVERFLOW", "LOCAL_REQUEUE_OVERFLOW", "LOCAL_WRITE_FAILED",
            "LOCAL_SHUTDOWN_DROPPED", "COLLECTION_GATE_DROPPED", "MODULE_POLICY_ERASED",
            "DISTRIBUTION_POLICY_ERASED", "DIRECT_BOOT_CAPACITY_DROPPED", "DIRECT_BOOT_CORRUPT_RECORD",
            "COLLECTION_PAUSED_STORAGE", "COLLECTION_ACCESS_MISSING",
        ).forEach { issueCode ->
            assertEquals(issueCode, fixture(issueCode = issueCode).issueCode)
        }
    }

    private fun fixture(
        moduleFamily: String = "USAGE_LIFECYCLE",
        issueCode: String = "TIMEOUT",
    ): AndroidUploadDiagnosticEvent = AndroidUploadDiagnosticEvent(
        id = UUID.randomUUID().toString(),
        day = LocalDate.parse("2026-08-26"),
        moduleFamily = moduleFamily,
        issueCode = issueCode,
        count = 2,
        firstOccurredAt = OffsetDateTime.parse("2026-08-26T12:00:00Z"),
        lastOccurredAt = OffsetDateTime.parse("2026-08-26T12:01:00Z"),
    )
}
