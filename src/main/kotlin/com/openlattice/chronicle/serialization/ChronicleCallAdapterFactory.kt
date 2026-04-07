package com.openlattice.chronicle.serialization

import com.google.common.base.Charsets
import org.apache.commons.io.IOUtils
import org.slf4j.LoggerFactory
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
                        val url = call.request().url.toString()
                        val message = response.message()
                        val exMsg = "Call to $url failed with code $code and message $message"
                        logger.error(exMsg)
                        throw ChronicleCallException(exMsg, url, body, code)
                    }
                    response.body()!!
                } catch (e: IOException) {
                    logger.error("Call to ${call.request().url} failed due to exception.", e)
                    throw e
                }
            }
        }
    }

    public companion object {
        private val logger = LoggerFactory.getLogger(ChronicleCallAdapterFactory::class.java)
    }
}