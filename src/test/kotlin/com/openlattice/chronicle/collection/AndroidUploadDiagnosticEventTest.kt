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
