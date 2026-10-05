package com.openlattice.chronicle.util

import okhttp3.Interceptor
import okhttp3.Response
import okio.Buffer
import java.io.IOException
import java.security.MessageDigest
import java.util.UUID

/**
 * Install one instance per logical researcher create, and reuse it on every retry.
 * Persist [key] with the request if retrying after restart. The first route/body binding is
 * immutable: editing the payload requires an explicitly new intent rather than another retry.
 */
public class ResearcherCreateIntent(public val key: String = UUID.randomUUID().toString()) : Interceptor {
    private var binding: Pair<String, List<Byte>>? = null

    init {
        require(key.length in 1..128 && key.matches(Regex("[A-Za-z0-9_-]+"))) { "Invalid create intent key" }
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val route = request.url.encodedPath.removePrefix("/chronicle").trimEnd('/')
        if (request.method != "POST" || !(route == "/v3/study" ||
            Regex("/v3/study/[0-9a-fA-F-]+/(api-keys|export/async)").matches(route) ||
            Regex("/v3/survey/[0-9a-fA-F-]+/questionnaire").matches(route))) return chain.proceed(request)
        val body = request.body ?: throw IOException("Create intent requires a request body")
        if (body.isOneShot() || body.isDuplex()) throw IOException("Create intent requires a replayable body")
        val bytes = Buffer().also(body::writeTo).readByteArray()
        val current = request.url.toString() to MessageDigest.getInstance("SHA-256").digest(bytes).toList()
        synchronized(this) {
            if (binding != null && binding != current) throw IOException("Create intent cannot retry a changed route or body")
            binding = current
        }
        val explicit = request.header("Idempotency-Key")
        if (explicit != null && explicit != key) throw IOException("Create intent key does not match request")
        return chain.proceed(request.newBuilder().header("Idempotency-Key", key).build())
    }
}
