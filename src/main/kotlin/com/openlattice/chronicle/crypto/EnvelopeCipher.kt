package com.openlattice.chronicle.crypto

import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.generators.HKDFBytesGenerator
import org.bouncycastle.crypto.generators.MLKEMKeyPairGenerator
import org.bouncycastle.crypto.kems.MLKEMExtractor
import org.bouncycastle.crypto.kems.MLKEMGenerator
import org.bouncycastle.crypto.params.HKDFParameters
import org.bouncycastle.crypto.params.MLKEMKeyGenerationParameters
import org.bouncycastle.crypto.params.MLKEMParameters
import org.bouncycastle.crypto.params.MLKEMPrivateKeyParameters
import org.bouncycastle.crypto.params.MLKEMPublicKeyParameters
import java.nio.ByteBuffer
import java.security.SecureRandom
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.MGF1ParameterSpec
import java.util.Base64
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource
import javax.crypto.spec.SecretKeySpec

/**
 * Hybrid (post-quantum) envelope seal/open, shared verbatim by the Android collector and the
 * backend (HIPAA-2028 W2). Lives in chronicle-models so both ends run **identical** crypto —
 * the round-trip is a parity guarantee enforced by [EnvelopeCipher]'s own tests.
 *
 * ## Construction (v2, [DEFAULT_ALG])
 * The payload is sealed with AES-256-GCM under a fresh content key. That content key is **not**
 * wrapped by a single asymmetric op; instead it is *derived* from two independent shared secrets
 * via a KEM-combiner so an attacker must break **both** to recover it (defense against both a
 * future quantum adversary and an ML-KEM implementation flaw):
 *
 *   contentKey = HKDF-SHA256( rsaSecret ‖ mlkemSecret , info = alg|version|keyId )
 *
 * - **Classical half** — a fresh 32-byte `rsaSecret` is transported under the study RSA public key
 *   with RSA-OAEP-SHA256 (MGF1-SHA256), exactly as the legacy v1 path did for the content key.
 * - **PQ half** — ML-KEM-1024 (FIPS 203) encapsulation to the study ML-KEM public key yields a
 *   32-byte `mlkemSecret` plus its ciphertext. ML-KEM decapsulation uses implicit rejection, so a
 *   wrong key/ciphertext yields a *different* secret rather than an error — the GCM tag is what
 *   fails closed.
 *
 * The combiner follows NIST SP 800-227 (final, Sep 2025) / the X-Wing construction: concatenate
 * the component secrets and run them through a KDF that preserves IND-CCA security.
 *
 * ## Wire format
 * No new envelope field: the two key ciphertexts are packed into [EncryptedEnvelope.encryptedKey]
 * as `len(rsaCt):4-byte-BE ‖ rsaCt ‖ mlkemCt`, dispatched by [EncryptedEnvelope.alg]. The legacy
 * v1 RSA-only path ([LEGACY_ALG]) is retained for decode so any prior persisted row still opens
 * (there are none today — the envelope is dormant — but the agility hook is kept).
 *
 * Pure JCA for the RSA + AES halves; BouncyCastle's low-level `org.bouncycastle.pqc.crypto.mlkem`
 * API for ML-KEM (no PQC exists in JDK 21 / Android JCA). The OAEP MGF1 digest is pinned to
 * SHA-256 explicitly: Android's default OAEP MGF1 historically falls back to SHA-1 even when the
 * main digest is SHA-256, which silently breaks interop with a JVM that uses SHA-256 for both.
 */
public object EnvelopeCipher {

    /** Algorithm identifier stamped into every [EncryptedEnvelope.alg] sealed today. */
    public const val DEFAULT_ALG: String = "RSA-OAEP-256+MLKEM1024+A256GCM"

    /** Current [EncryptedEnvelope.version] (hybrid). */
    public const val ENVELOPE_VERSION: Int = 2

    /** Legacy v1 RSA-OAEP-only algorithm — decode path only, never used for new seals. */
    public const val LEGACY_ALG: String = "RSA-OAEP-256+A256GCM"

    /** Legacy v1 envelope version — decode path only. */
    public const val LEGACY_VERSION: Int = 1

    private const val AES_KEY_BYTES = 32
    private const val SECRET_BYTES = 32
    private const val GCM_NONCE_BYTES = 12
    private const val GCM_TAG_BITS = 128
    private const val RSA_TRANSFORM = "RSA/ECB/OAEPPadding"
    private const val AES_TRANSFORM = "AES/GCM/NoPadding"

    private fun oaepParams(): OAEPParameterSpec = OAEPParameterSpec(
        "SHA-256",
        "MGF1",
        MGF1ParameterSpec.SHA256,
        PSource.PSpecified.DEFAULT,
    )

    /** Generate a fresh ML-KEM-1024 keypair (study provisioning + tests). */
    public fun generateMlkemKeyPair(): Pair<MLKEMPublicKeyParameters, MLKEMPrivateKeyParameters> {
        val generator = MLKEMKeyPairGenerator()
        generator.init(MLKEMKeyGenerationParameters(SecureRandom(), MLKEMParameters.ml_kem_1024))
        val pair = generator.generateKeyPair()
        return (pair.public as MLKEMPublicKeyParameters) to (pair.private as MLKEMPrivateKeyParameters)
    }

    /** Raw-encode an ML-KEM public key for transport (base64 of the FIPS 203 encoding). */
    public fun encodeMlkemPublicKey(publicKey: MLKEMPublicKeyParameters): String =
        Base64.getEncoder().encodeToString(publicKey.encoded)

    /** Decode a base64 raw ML-KEM-1024 public key produced by [encodeMlkemPublicKey]. */
    public fun decodeMlkemPublicKey(encoded: String): MLKEMPublicKeyParameters =
        MLKEMPublicKeyParameters(MLKEMParameters.ml_kem_1024, Base64.getDecoder().decode(encoded))

    /** Raw-encode an ML-KEM private key for custody (base64 of the FIPS 203 encoding). */
    public fun encodeMlkemPrivateKey(privateKey: MLKEMPrivateKeyParameters): String =
        Base64.getEncoder().encodeToString(privateKey.encoded)

    /** Decode a base64 raw ML-KEM-1024 private key produced by [encodeMlkemPrivateKey]. */
    public fun decodeMlkemPrivateKey(encoded: String): MLKEMPrivateKeyParameters =
        MLKEMPrivateKeyParameters(MLKEMParameters.ml_kem_1024, Base64.getDecoder().decode(encoded))

    /**
     * Seal [plaintext] for the study's [rsaPublicKey] + [mlkemPublicKey]. Generates a fresh content
     * key, RSA secret, ML-KEM encapsulation, and nonce, so two calls with the same inputs produce
     * different ciphertext. [aad] must be reproduced exactly by [open] or decryption fails — build
     * it with [aad].
     */
    public fun seal(
        rsaPublicKey: RSAPublicKey,
        mlkemPublicKey: MLKEMPublicKeyParameters,
        keyId: String,
        payloadType: EncryptedPayloadType,
        plaintext: ByteArray,
        aad: ByteArray,
        sampleCount: Int,
    ): EncryptedEnvelope {
        val random = SecureRandom()

        // Classical half: RSA-OAEP transports a fresh random secret.
        val rsaSecret = ByteArray(SECRET_BYTES).also(random::nextBytes)
        val rsaCipher = Cipher.getInstance(RSA_TRANSFORM)
        rsaCipher.init(Cipher.ENCRYPT_MODE, rsaPublicKey, oaepParams())
        val rsaCt = rsaCipher.doFinal(rsaSecret)

        // PQ half: ML-KEM-1024 encapsulation.
        val encapsulated = MLKEMGenerator(random).generateEncapsulated(mlkemPublicKey)
        val mlkemSecret = encapsulated.secret
        val mlkemCt = encapsulated.encapsulation

        val contentKey = SecretKeySpec(deriveContentKey(rsaSecret, mlkemSecret, keyId), "AES")

        val nonce = ByteArray(GCM_NONCE_BYTES).also(random::nextBytes)
        val aesCipher = Cipher.getInstance(AES_TRANSFORM)
        aesCipher.init(Cipher.ENCRYPT_MODE, contentKey, GCMParameterSpec(GCM_TAG_BITS, nonce))
        aesCipher.updateAAD(aad)
        val ciphertext = aesCipher.doFinal(plaintext)

        val encoder = Base64.getEncoder()
        return EncryptedEnvelope(
            version = ENVELOPE_VERSION,
            alg = DEFAULT_ALG,
            keyId = keyId,
            payloadType = payloadType,
            encryptedKey = encoder.encodeToString(packKeyCiphertexts(rsaCt, mlkemCt)),
            iv = encoder.encodeToString(nonce),
            ciphertext = encoder.encodeToString(ciphertext),
            sampleCount = sampleCount,
        )
    }

    /**
     * Open a hybrid [envelope] with the study's [rsaPrivateKey] + [mlkemPrivateKey] and the same
     * [aad] used to seal it. Throws (e.g. `AEADBadTagException`) on the wrong key (either half), a
     * tampered ciphertext, or a mismatched AAD — there is no partial/failed-open result to mistake
     * for success.
     */
    public fun open(
        rsaPrivateKey: RSAPrivateKey,
        mlkemPrivateKey: MLKEMPrivateKeyParameters,
        envelope: EncryptedEnvelope,
        aad: ByteArray,
    ): ByteArray {
        // Fail closed on an unexpected version/alg rather than decrypting it with the v2 transforms:
        // an algorithm-downgrade/confusion guard that also pins the version going into the AAD to the
        // trusted constant.
        require(envelope.version == ENVELOPE_VERSION) {
            "Unsupported envelope version ${envelope.version} (expected $ENVELOPE_VERSION)"
        }
        require(envelope.alg == DEFAULT_ALG) {
            "Unsupported envelope alg ${envelope.alg} (expected $DEFAULT_ALG)"
        }
        val decoder = Base64.getDecoder()
        val (rsaCt, mlkemCt) = unpackKeyCiphertexts(decoder.decode(envelope.encryptedKey))

        val rsaCipher = Cipher.getInstance(RSA_TRANSFORM)
        rsaCipher.init(Cipher.DECRYPT_MODE, rsaPrivateKey, oaepParams())
        val rsaSecret = rsaCipher.doFinal(rsaCt)

        val mlkemSecret = MLKEMExtractor(mlkemPrivateKey).extractSecret(mlkemCt)

        val contentKey = SecretKeySpec(deriveContentKey(rsaSecret, mlkemSecret, envelope.keyId), "AES")
        val nonce = decoder.decode(envelope.iv)
        val ciphertext = decoder.decode(envelope.ciphertext)

        val aesCipher = Cipher.getInstance(AES_TRANSFORM)
        aesCipher.init(Cipher.DECRYPT_MODE, contentKey, GCMParameterSpec(GCM_TAG_BITS, nonce))
        aesCipher.updateAAD(aad)
        return aesCipher.doFinal(ciphertext)
    }

    /**
     * KEM-combiner: derive the AES-256 content key from both component secrets. Binding [info] to
     * the alg/version/keyId gives domain separation and ties the derived key to the envelope context.
     */
    private fun deriveContentKey(rsaSecret: ByteArray, mlkemSecret: ByteArray, keyId: String): ByteArray {
        val ikm = rsaSecret + mlkemSecret
        val info = "chronicle-hybrid-envelope|v$ENVELOPE_VERSION|$DEFAULT_ALG|$keyId".toByteArray(Charsets.UTF_8)
        val hkdf = HKDFBytesGenerator(SHA256Digest())
        hkdf.init(HKDFParameters(ikm, null, info))
        val out = ByteArray(AES_KEY_BYTES)
        hkdf.generateBytes(out, 0, out.size)
        return out
    }

    /** Pack the two key ciphertexts into one blob: `len(rsaCt):4-byte-BE ‖ rsaCt ‖ mlkemCt`. */
    private fun packKeyCiphertexts(rsaCt: ByteArray, mlkemCt: ByteArray): ByteArray =
        ByteBuffer.allocate(Int.SIZE_BYTES + rsaCt.size + mlkemCt.size)
            .putInt(rsaCt.size)
            .put(rsaCt)
            .put(mlkemCt)
            .array()

    private fun unpackKeyCiphertexts(packed: ByteArray): Pair<ByteArray, ByteArray> {
        require(packed.size > Int.SIZE_BYTES) { "Malformed hybrid encryptedKey: too short" }
        val buffer = ByteBuffer.wrap(packed)
        val rsaLen = buffer.int
        require(rsaLen in 1..(packed.size - Int.SIZE_BYTES)) {
            "Malformed hybrid encryptedKey: rsaCt length $rsaLen out of range"
        }
        val rsaCt = ByteArray(rsaLen).also(buffer::get)
        val mlkemCt = ByteArray(buffer.remaining()).also(buffer::get)
        return rsaCt to mlkemCt
    }

    /**
     * Canonical additional-authenticated-data binding a ciphertext to its context. Both ends MUST
     * build this from trusted values (request path / auth), never from attacker-supplied envelope
     * fields, so a row cannot be replayed under a different study or participant.
     */
    public fun aad(
        version: Int,
        studyId: UUID,
        participantId: String,
        payloadType: EncryptedPayloadType,
    ): ByteArray = "v$version|$studyId|$participantId|${payloadType.id}".toByteArray(Charsets.UTF_8)
}
