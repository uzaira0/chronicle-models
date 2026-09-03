package com.openlattice.chronicle.crypto

import com.openlattice.chronicle.collection.TestMappers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Wire-contract guarantees for [EncryptedEnvelope] and [EncryptedPayloadType]: JSON and
 * binary (Smile) round-trip, stable enum ids, forward compatibility, and constructor
 * validation — the same idiom chronicle-models uses for its other upload DTOs.
 */
class EncryptedEnvelopeSerializationTest {

    private fun sample() = EncryptedEnvelope(
        keyId = "key-1",
        payloadType = EncryptedPayloadType.SENSOR,
        encryptedKey = "QUJD",
        iv = "REVG",
        ciphertext = "R0hJ",
        sampleCount = 3,
    )

    @Test
    fun `json round-trips`() {
        val mapper = TestMappers.json()
        val json = mapper.writeValueAsString(sample())
        assertEquals(sample(), mapper.readValue(json, EncryptedEnvelope::class.java))
    }

    @Test
    fun `smile round-trips`() {
        val mapper = TestMappers.smile()
        val bytes = mapper.writeValueAsBytes(sample())
        assertEquals(sample(), mapper.readValue(bytes, EncryptedEnvelope::class.java))
    }

    @Test
    fun `payload type serializes as its stable id`() {
        val mapper = TestMappers.json()
        assertTrue(mapper.writeValueAsString(sample()).contains("\"sensor\""))
        assertEquals(EncryptedPayloadType.BATTERY, EncryptedPayloadType.fromId("battery"))
        assertEquals("usage", EncryptedPayloadType.USAGE.id)
    }

    @Test
    fun `unknown json properties are ignored`() {
        val mapper = TestMappers.json()
        val json = """
            {"version":2,"alg":"RSA-OAEP-256+MLKEM1024+A256GCM","keyId":"key-1","payloadType":"sensor",
             "encryptedKey":"QUJD","iv":"REVG","ciphertext":"R0hJ","sampleCount":3,
             "somethingFromTheFuture":true}
        """.trimIndent()
        assertEquals(sample(), mapper.readValue(json, EncryptedEnvelope::class.java))
    }

    @Test
    fun `blank ciphertext is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            EncryptedEnvelope(
                keyId = "k", payloadType = EncryptedPayloadType.SENSOR,
                encryptedKey = "QUJD", iv = "REVG", ciphertext = "  ",
            )
        }
    }

    @Test
    fun `unknown payload type id throws`() {
        assertThrows(IllegalArgumentException::class.java) { EncryptedPayloadType.fromId("eeg") }
    }
}
