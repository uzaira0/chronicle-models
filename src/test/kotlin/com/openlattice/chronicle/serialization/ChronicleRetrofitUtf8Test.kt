package com.openlattice.chronicle.serialization

import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.http.GET

/**
 * Drives the real Retrofit stack through [ChronicleJacksonConverterFactory] and
 * [ChronicleCallAdapterFactory] with a non-ASCII UTF-8 body, so the plain-text and
 * error-body decoders must both read UTF-8 exactly.
 */
class ChronicleRetrofitUtf8Test {

    interface TextApi {
        @GET("/text")
        fun text(): String
    }

    @Test
    fun plainTextBodyIsDecodedAsUtf8() {
        val api = apiReturning(200, NON_ASCII_TEXT.toByteArray(Charsets.UTF_8))

        assertEquals(NON_ASCII_TEXT, api.text())
    }

    @Test
    fun errorBodyIsDecodedAsUtf8() {
        val api = apiReturning(400, NON_ASCII_TEXT.toByteArray(Charsets.UTF_8))

        try {
            api.text()
            fail("a 400 response must throw")
        } catch (e: ChronicleCallException) {
            assertEquals(400, e.code)
            assertEquals(NON_ASCII_TEXT, e.body)
        }
    }

    private fun apiReturning(code: Int, body: ByteArray): TextApi {
        val httpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(code)
                    .message(if (code < 400) "OK" else "Bad Request")
                    .body(body.toResponseBody("text/plain; charset=utf-8".toMediaType()))
                    .build()
            }
            .build()
        return Retrofit.Builder()
            .baseUrl("http://localhost:8081/")
            .client(httpClient)
            .addConverterFactory(ChronicleJacksonConverterFactory(ObjectMapper()))
            .addCallAdapterFactory(ChronicleCallAdapterFactory())
            .build()
            .create(TextApi::class.java)
    }

    private companion object {
        const val NON_ASCII_TEXT = "Ñandú café 研究 – ✓"
    }
}
