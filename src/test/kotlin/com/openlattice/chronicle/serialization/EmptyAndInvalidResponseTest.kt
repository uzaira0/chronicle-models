package com.openlattice.chronicle.serialization

import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Timeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.lang.reflect.Type

/**
 * A 204 No Content response has a null body. The call adapter previously dereferenced it
 * unconditionally, so every `Unit`-returning operation threw NullPointerException after the
 * server had already completed the action. The Jackson converter separately turned a missing
 * content type, an unsupported content type and a JSON decoding failure into a null success
 * value, which surfaced later as an unrelated NullPointerException.
 */
class EmptyAndInvalidResponseTest {

    private class FixedCall(private val response: Response<Any>) : Call<Any> {
        override fun execute(): Response<Any> = response
        override fun enqueue(callback: Callback<Any>): Unit = throw UnsupportedOperationException()
        override fun isExecuted(): Boolean = true
        override fun cancel(): Unit = throw UnsupportedOperationException()
        override fun isCanceled(): Boolean = false
        override fun clone(): Call<Any> = FixedCall(response)
        override fun request(): Request = Request.Builder().url("https://chronicle.example/x").build()
        override fun timeout(): Timeout = Timeout.NONE
    }

    private fun raw(code: Int): okhttp3.Response = okhttp3.Response.Builder()
        .request(Request.Builder().url("https://chronicle.example/x").build())
        .protocol(Protocol.HTTP_1_1)
        .code(code)
        .message("No Content")
        .build()

    private fun adapt(returnType: Type, response: Response<Any>): Any {
        @Suppress("UNCHECKED_CAST")
        val adapter = ChronicleCallAdapterFactory().get(returnType, emptyArray(), retrofitStub())
            as retrofit2.CallAdapter<Any, Any>
        return adapter.adapt(FixedCall(response))
    }

    private fun retrofitStub() = retrofit2.Retrofit.Builder()
        .baseUrl("https://chronicle.example/")
        .addConverterFactory(ChronicleJacksonConverterFactory(ObjectMapper()))
        .build()

    @Test
    fun `a no-content response on a Unit operation does not throw`() {
        assertEquals(Unit, adapt(Unit::class.java, Response.success<Any>(null, raw(204))))
    }

    @Test
    fun `a no-content response on a value operation is reported as a call failure`() {
        assertThrows(ChronicleCallException::class.java) {
            adapt(String::class.java, Response.success<Any>(null, raw(204)))
        }
    }

    private fun convert(body: ResponseBody): Any? =
        ChronicleJacksonConverterFactory(ObjectMapper())
            .responseBodyConverter(Map::class.java, emptyArray(), retrofitStub())!!
            .convert(body)

    @Test
    fun `malformed json is propagated as an IOException`() {
        assertThrows(IOException::class.java) {
            convert("{not json".toResponseBody("application/json".toMediaType()))
        }
    }

    @Test
    fun `a missing content type on a non-empty body is propagated as an IOException`() {
        assertThrows(IOException::class.java) {
            convert("""{"a":1}""".toResponseBody(null))
        }
    }

    @Test
    fun `an unsupported content type is propagated as an IOException`() {
        assertThrows(IOException::class.java) {
            convert("PK".toResponseBody("application/zip".toMediaType()))
        }
    }
}
