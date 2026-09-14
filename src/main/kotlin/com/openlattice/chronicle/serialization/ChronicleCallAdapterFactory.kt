package com.openlattice.chronicle.serialization

import com.google.common.base.Charsets
import org.apache.commons.io.IOUtils
import org.slf4j.LoggerFactory
import okhttp3.HttpUrl
import retrofit2.Call
import retrofit2.CallAdapter
import retrofit2.Retrofit
import java.io.IOException
import java.lang.reflect.Type

/**
 *
 * @author Matthew Tamayo-Rios &lt;matthew@getmethodic.com&gt;
 */
public class ChronicleCallAdapterFactory : CallAdapter.Factory() {
    override fun get(returnType: Type, annotations: Array<Annotation>, retrofit: Retrofit): CallAdapter<*, *> {
        return object : CallAdapter<Any, Any> {
            override fun responseType(): Type {
                return returnType
            }

            override fun adapt(call: Call<Any>): Any {
                return try {
                    val response = call.execute()
                    val code = response.code()
                    if (code >= 400) {
                        val body = response.errorBody()?.let {
                            IOUtils.toString(it.byteStream(), Charsets.UTF_8)
                        } ?: "Unknown error"
                        val url = call.request().url.redactedForLogging()
                        val message = response.message()
                        val exMsg = "Call to $url failed with code $code and message $message"
                        logger.error(exMsg)
                        throw ChronicleCallException(exMsg, url, body, code)
                    }
                    // A 204/205 (and any other empty-bodied success) yields a null body. That is the
                    // correct outcome for the many Unit/Void operations; only a value-returning call
                    // with no body is an error.
                    response.body() ?: if (isVoid(returnType)) {
                        Unit
                    } else {
                        val url = call.request().url.redactedForLogging()
                        throw ChronicleCallException(
                            "Call to $url returned code $code with an empty body but $returnType was expected",
                            url,
                            "",
                            code
                        )
                    }
                } catch (e: IOException) {
                    logger.error("Call to ${call.request().url.redactedForLogging()} failed due to exception.", e)
                    throw e
                }
            }
        }
    }

    public companion object {
        private val logger = LoggerFactory.getLogger(ChronicleCallAdapterFactory::class.java)

        internal fun isVoid(type: Type): Boolean =
            type == Unit::class.java || type == Void::class.java || type == Void.TYPE

        internal fun HttpUrl.redactedForLogging(): String {
            val defaultPort = when (scheme) {
                "http" -> 80
                "https" -> 443
                else -> -1
            }
            val portSuffix = if (port != defaultPort) ":$port" else ""
            return "$scheme://$host$portSuffix/<redacted>"
        }
    }
}
