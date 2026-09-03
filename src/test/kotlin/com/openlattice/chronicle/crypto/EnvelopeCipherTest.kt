package com.openlattice.chronicle.crypto

import org.bouncycastle.crypto.params.MLKEMPrivateKeyParameters
import org.bouncycastle.crypto.params.MLKEMPublicKeyParameters
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.BeforeClass
import org.junit.Test
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.util.Base64
import java.util.UUID

/**
 * Parity + correctness guarantees for the hybrid seal/open round-trip shared by the Android
 * collector and the backend. If these pass, both ends — running the same [EnvelopeCipher] —
 * agree, and a ciphertext only opens with the right RSA key, the right ML-KEM key, the right
 * AAD, and an untampered body. The "both halves must break" tests pin the post-quantum hybrid
 * property: compromising either the RSA private key alone OR the ML-KEM private key alone is not
 * enough to recover the plaintext.
 */
class EnvelopeCipherTest {

    companion object {
        private lateinit var rsaKeyPair: KeyPair
        private lateinit var mlkemPublicKey: MLKEMPublicKeyParameters
        private lateinit var mlkemPrivateKey: MLKEMPrivateKeyParameters

        @BeforeClass
        @JvmStatic
        fun generateKeys() {
            val gen = KeyPairGenerator.getInstance("RSA")
            gen.initialize(4096)
            rsaKeyPair = gen.generateKeyPair()
            val (pub, priv) = EnvelopeCipher.generateMlkemKeyPair()
            mlkemPublicKey = pub
            mlkemPrivateKey = priv
        }
    }

    private val rsaPublic: RSAPublicKey get() = rsaKeyPair.public as RSAPublicKey
    private val rsaPrivate: RSAPrivateKey get() = rsaKeyPair.private as RSAPrivateKey

    private val studyId = UUID.fromString("11111111-1111-1111-1111-111111111111")
    private val participantId = "participant-42"
    private fun sensorAad() = EnvelopeCipher.aad(
        EnvelopeCipher.ENVELOPE_VERSION, studyId, participantId, EncryptedPayloadType.SENSOR,
    )

    private fun seal(
        plaintext: ByteArray,
        aad: ByteArray = sensorAad(),
        keyId: String = "key-1",
        payloadType: EncryptedPayloadType = EncryptedPayloadType.SENSOR,
        sampleCount: Int = 1,
    ) = EnvelopeCipher.seal(rsaPublic, mlkemPublicKey, keyId, payloadType, plaintext, aad, sampleCount)

    @Test
    fun `seal then open round-trips the plaintext`() {
        val plaintext = """[{"sensor":"accelerometer","x":0.1}]""".toByteArray()
        val aad = sensorAad()
        val envelope = seal(plaintext, aad)
        val opened = EnvelopeCipher.open(rsaPrivate, mlkemPrivateKey, envelope, aad)
        assertArrayEquals(plaintext, opened)
    }

    @Test
    fun `envelope carries the declared hybrid metadata`() {
        val envelope = seal(
            "x".toByteArray(),
            EnvelopeCipher.aad(EnvelopeCipher.ENVELOPE_VERSION, studyId, participantId, EncryptedPayloadType.BATTERY),
            keyId = "key-9",
            payloadType = EncryptedPayloadType.BATTERY,
            sampleCount = 7,
        )
        assertEquals(2, envelope.version)
        assertEquals(EnvelopeCipher.ENVELOPE_VERSION, envelope.version)
        assertEquals("RSA-OAEP-256+MLKEM1024+A256GCM", envelope.alg)
        assertEquals(EnvelopeCipher.DEFAULT_ALG, envelope.alg)
        assertEquals("key-9", envelope.keyId)
        assertEquals(EncryptedPayloadType.BATTERY, envelope.payloadType)
        assertEquals(7, envelope.sampleCount)
    }

    @Test
    fun `sealing twice yields different nonce, key ciphertext, and body`() {
        val plaintext = "same".toByteArray()
        val aad = sensorAad()
        val a = seal(plaintext, aad)
        val b = seal(plaintext, aad)
        assertNotEquals(a.iv, b.iv)
        assertNotEquals(a.encryptedKey, b.encryptedKey)
        assertNotEquals(a.ciphertext, b.ciphertext)
    }

    @Test
    fun `open with a mismatched AAD fails`() {
        val envelope = seal("secret".toByteArray())
        val wrongAad = EnvelopeCipher.aad(
            EnvelopeCipher.ENVELOPE_VERSION, studyId, "someone-else", EncryptedPayloadType.SENSOR,
        )
        assertThrows(Exception::class.java) {
            EnvelopeCipher.open(rsaPrivate, mlkemPrivateKey, envelope, wrongAad)
        }
    }

    @Test
    fun `open with the wrong RSA private key fails - PQ key alone is not enough`() {
        val aad = sensorAad()
        val envelope = seal("secret".toByteArray(), aad)
        val otherRsa = KeyPairGenerator.getInstance("RSA").apply { initialize(4096) }
            .generateKeyPair().private as RSAPrivateKey
        // Correct ML-KEM key, wrong RSA key: must still fail (both halves required).
        assertThrows(Exception::class.java) {
            EnvelopeCipher.open(otherRsa, mlkemPrivateKey, envelope, aad)
        }
    }

    @Test
    fun `open with the wrong ML-KEM private key fails - RSA key alone is not enough`() {
        val aad = sensorAad()
        val envelope = seal("secret".toByteArray(), aad)
        val otherMlkem = EnvelopeCipher.generateMlkemKeyPair().second
        // Correct RSA key, wrong ML-KEM key: ML-KEM implicit rejection yields a different secret,
        // so the derived content key is wrong and the GCM tag fails closed.
        assertThrows(Exception::class.java) {
            EnvelopeCipher.open(rsaPrivate, otherMlkem, envelope, aad)
        }
    }

    @Test
    fun `tampered ciphertext fails the auth tag`() {
        val aad = sensorAad()
        val envelope = seal("secret".toByteArray(), aad)
        val tampered = envelope.copy(ciphertext = flipLastChar(envelope.ciphertext))
        assertThrows(Exception::class.java) {
            EnvelopeCipher.open(rsaPrivate, mlkemPrivateKey, tampered, aad)
        }
    }

    @Test
    fun `tampered key ciphertext fails`() {
        val aad = sensorAad()
        val envelope = seal("secret".toByteArray(), aad)
        val tampered = envelope.copy(encryptedKey = flipLastChar(envelope.encryptedKey))
        assertThrows(Exception::class.java) {
            EnvelopeCipher.open(rsaPrivate, mlkemPrivateKey, tampered, aad)
        }
    }

    @Test
    fun `ciphertext does not contain the plaintext`() {
        val envelope = seal("TOP-SECRET-MARKER".toByteArray())
        assertFalse(envelope.ciphertext.contains("TOP-SECRET-MARKER"))
    }

    @Test
    fun `ML-KEM public key survives encode-decode round-trip and still opens`() {
        val aad = sensorAad()
        val reDecodedPub = EnvelopeCipher.decodeMlkemPublicKey(
            EnvelopeCipher.encodeMlkemPublicKey(mlkemPublicKey),
        )
        val envelope = EnvelopeCipher.seal(
            rsaPublic, reDecodedPub, "key-1", EncryptedPayloadType.SENSOR, "secret".toByteArray(), aad, 1,
        )
        val reDecodedPriv = EnvelopeCipher.decodeMlkemPrivateKey(
            EnvelopeCipher.encodeMlkemPrivateKey(mlkemPrivateKey),
        )
        assertArrayEquals(
            "secret".toByteArray(),
            EnvelopeCipher.open(rsaPrivate, reDecodedPriv, envelope, aad),
        )
    }

    private fun flipLastChar(b64: String): String {
        val bytes = Base64.getDecoder().decode(b64)
        bytes[bytes.lastIndex] = (bytes.last().toInt() xor 0x01).toByte()
        return Base64.getEncoder().encodeToString(bytes)
    }
}
