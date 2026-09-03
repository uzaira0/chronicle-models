package com.openlattice.chronicle.study

import com.openlattice.chronicle.collection.TestMappers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * [StudyEncryptionSetting] round-trips polymorphically as a [StudySetting] (via the
 * `@class` discriminator), enforces its enabled-state invariants, and never carries
 * private key material.
 */
class StudyEncryptionSettingTest {

    private val publicPem = """
        -----BEGIN PUBLIC KEY-----
        MIICIjANBgkqhkiG9w0BAQEFAAOCAg8A
        -----END PUBLIC KEY-----
    """.trimIndent()

    // Opaque base64 placeholder for the ML-KEM public key — these tests exercise the DTO's
    // invariants and serialization (the field is carried as a string), not the crypto.
    private val mlkemPub = "bWxrZW0tcHVibGljLWtleS1wbGFjZWhvbGRlcg=="

    @Test
    fun `round-trips as a polymorphic StudySetting`() {
        val mapper = TestMappers.json()
        val setting: StudySetting = StudyEncryptionSetting(
            enabled = true, keyId = "key-1", publicKeyPem = publicPem, mlkemPublicKey = mlkemPub,
        )
        val json = mapper.writeValueAsString(setting)
        assertEquals(setting, mapper.readValue(json, StudySetting::class.java))
    }

    @Test
    fun `round-trips inside StudySettings keyed by Encryption`() {
        val mapper = TestMappers.json()
        val settings = StudySettings(
            mapOf(
                StudySettingType.Encryption to StudyEncryptionSetting(
                    enabled = true, keyId = "key-1", publicKeyPem = publicPem, mlkemPublicKey = mlkemPub,
                ),
            ),
        )
        val json = mapper.writeValueAsString(settings)
        val back = mapper.readValue(json, StudySettings::class.java)
        assertEquals(settings[StudySettingType.Encryption], back[StudySettingType.Encryption])
    }

    @Test
    fun `disabled default is valid and empty`() {
        val setting = StudyEncryptionSetting()
        assertFalse(setting.enabled)
        assertEquals("", setting.keyId)
    }

    @Test
    fun `enabled without a keyId is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            StudyEncryptionSetting(enabled = true, keyId = "", publicKeyPem = publicPem)
        }
    }

    @Test
    fun `enabled without a public key is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            StudyEncryptionSetting(enabled = true, keyId = "key-1", publicKeyPem = "")
        }
    }

    @Test
    fun `private key material is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            StudyEncryptionSetting(
                enabled = true,
                keyId = "key-1",
                publicKeyPem = "-----BEGIN PRIVATE KEY-----\nMII...\n-----END PRIVATE KEY-----",
                mlkemPublicKey = mlkemPub,
            )
        }
    }
}
