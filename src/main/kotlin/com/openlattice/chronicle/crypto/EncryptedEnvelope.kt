package com.openlattice.chronicle.crypto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * Self-describing ciphertext envelope for a single uploaded batch (HIPAA-2028 W2).
 *
 * Hybrid post-quantum envelope encryption: the payload is sealed with a fresh AES-256-GCM
 * content key; that content key is derived (HKDF) from two component secrets — one transported
 * under the study RSA public key (RSA-OAEP-SHA256), one from ML-KEM-1024 encapsulation to the
 * study ML-KEM public key — so an attacker must break **both** to recover it (see [EnvelopeCipher]).
 * The backend stores this **blind** — it never holds either study private key (Vault does), so a
 * database compromise yields only ciphertext + key ciphertexts it cannot open.
 *
 * The GCM auth tag covers an AAD bound to `version|studyId|participantId|payloadType`
 * (see [EnvelopeCipher.aad]). The AAD is **not** carried on the wire: both ends recompute
 * it from trusted context (request path / auth), so a ciphertext moved to another
 * participant or study fails to open. Only [sampleCount] (a non-sensitive count for
 * ingest metrics) travels in clear alongside the ciphertext.
 *
 * All binary fields are Base64 (standard, padded). Forward-compatible: unknown JSON
 * properties are ignored.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public data class EncryptedEnvelope(
    /** Envelope schema version. See [EnvelopeCipher.ENVELOPE_VERSION]. */
    val version: Int = EnvelopeCipher.ENVELOPE_VERSION,
    /** Algorithm identifier, e.g. [EnvelopeCipher.DEFAULT_ALG]. */
    val alg: String = EnvelopeCipher.DEFAULT_ALG,
    /** Study key id the content key was wrapped under (selects the Vault private key). */
    val keyId: String,
    /** Which logical stream this envelope carries. */
    val payloadType: EncryptedPayloadType,
    /**
     * Base64 of the packed key ciphertexts. For the hybrid alg this is
     * `len(rsaCt):4-byte-BE ‖ rsaCt ‖ mlkemCt` (RSA-OAEP secret + ML-KEM-1024 encapsulation);
     * for the legacy v1 alg it is the single RSA-OAEP-wrapped content key. See [EnvelopeCipher].
     */
    val encryptedKey: String,
    /** Base64 12-byte AES-GCM nonce. */
    val iv: String,
    /** Base64 AES-GCM ciphertext (includes the 128-bit tag). */
    val ciphertext: String,
    /** Plaintext sample count, for ingest metrics without decrypting. Non-sensitive. */
    val sampleCount: Int = 0,
) {
    init {
        require(version > 0) { "Envelope version must be positive: $version" }
        require(alg.isNotBlank()) { "Envelope alg must not be blank" }
        require(keyId.isNotBlank()) { "Envelope keyId must not be blank" }
        require(encryptedKey.isNotBlank()) { "Envelope encryptedKey must not be blank" }
        require(iv.isNotBlank()) { "Envelope iv must not be blank" }
        require(ciphertext.isNotBlank()) { "Envelope ciphertext must not be blank" }
        require(sampleCount >= 0) { "Envelope sampleCount must not be negative: $sampleCount" }
    }
}
