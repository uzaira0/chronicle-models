package com.openlattice.chronicle.serialization

import com.fasterxml.jackson.databind.ObjectMapper
import com.openlattice.chronicle.serialization.ChronicleCallAdapterFactory.Companion.redactedForLogging
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.http.GET
import java.io.IOException
import java.io.UncheckedIOException

internal interface PingApi {
    @GET("ping")
    fun ping(): Map<String, Any>

    @GET("participant/android-pixel-aws-20260702/text")
    fun text(): String
}

class ChronicleCallAdapterFactoryTest {
    @Test
    fun `a transport failure through the Kotlin interface proxy surfaces unchecked with its IOException`() {
        val reset = IOException("connection reset")
        val api = Retrofit.Builder()
            .baseUrl("https://chronicle.example/")
            .client(OkHttpClient.Builder().addInterceptor { throw reset }.build())
            .addConverterFactory(ChronicleJacksonConverterFactory(ObjectMapper()))
            .addCallAdapterFactory(ChronicleCallAdapterFactory())
            .build()
            .create(PingApi::class.java)

        val thrown = assertThrows(UncheckedIOException::class.java) { api.ping() }

        assertSame(reset, thrown.cause)
    }
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

    /** Non-ASCII bodies decode byte-exact as UTF-8 on success and failure; a failure names only the origin. */
    @Test
    fun `responses decode exact UTF-8 and a failure exposes only the redacted origin`() {
        val text = "\u00e5 \u00fc\u00df \u65e5\u672c\u8a9e \ud83d\udcf1"
        var reply: Pair<Int, String> = 200 to "application/json"
        fun api() = Retrofit.Builder()
            .baseUrl("https://user:password@chronicle.example/study/00000000-0000-4000-8000-000000000000/")
            .client(OkHttpClient.Builder().addInterceptor { chain ->
                val (code, type) = reply
                val body = if (type == "application/json") """{"label":"$text"}""" else text
                Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(code).message("status")
                    .body(body.toByteArray(Charsets.UTF_8).toResponseBody(type.toMediaType())).build()
            }.build())
            .addConverterFactory(ChronicleJacksonConverterFactory(ObjectMapper()))
            .addCallAdapterFactory(ChronicleCallAdapterFactory())
            .build()
            .create(PingApi::class.java)

        assertEquals(mapOf("label" to text), api().ping())
        reply = 200 to "text/plain; charset=utf-8"
        assertEquals(text, api().text())
        reply = 409 to "text/plain; charset=utf-8"
        val failure = assertThrows(ChronicleCallException::class.java) { api().text() }
        assertEquals(text, failure.body)
        assertEquals("https://chronicle.example/<redacted>", failure.url)
        for (secret in listOf("user", "password", "00000000-0000-4000-8000-000000000000", "android-pixel-aws-20260702")) {
            assertFalse(failure.message.orEmpty().contains(secret))
        }
    }

    @Test
    fun `redacted URL omits default port`() {
        val redacted = "https://chronicle-screentime-app.research.bcm.edu/path".toHttpUrl()
            .redactedForLogging()

        assertEquals("https://chronicle-screentime-app.research.bcm.edu/<redacted>", redacted)
    }
}
