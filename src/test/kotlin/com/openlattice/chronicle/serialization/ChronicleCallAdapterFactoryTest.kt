package com.openlattice.chronicle.serialization

import com.openlattice.chronicle.serialization.ChronicleCallAdapterFactory.Companion.redactedForLogging
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ChronicleCallAdapterFactoryTest {
    @Test
    fun `redacted URL keeps origin and strips path query fragment and userinfo`() {
        val raw = "https://user:password@host.example.internal:8443/" +
            "chronicle/v4/study/00000000-0000-4000-8000-000000000000/" +
            "participant/android-pixel-aws-20260702/enroll?apiKey=secret#frag"

        val redacted = raw.toHttpUrl().redactedForLogging()

        assertEquals(
            "https://host.example.internal:8443/<redacted>",
            redacted,
        )
        assertFalse(redacted.contains("user"))
        assertFalse(redacted.contains("password"))
        assertFalse(redacted.contains("00000000-0000-4000-8000-000000000000"))
        assertFalse(redacted.contains("android-pixel-aws-20260702"))
        assertFalse(redacted.contains("apiKey"))
        assertFalse(redacted.contains("secret"))
        assertFalse(redacted.contains("frag"))
    }

    @Test
    fun `redacted URL omits default port`() {
        val redacted = "https://chronicle-screentime-app.research.bcm.edu/path".toHttpUrl()
            .redactedForLogging()

        assertEquals("https://chronicle-screentime-app.research.bcm.edu/<redacted>", redacted)
    }
}
