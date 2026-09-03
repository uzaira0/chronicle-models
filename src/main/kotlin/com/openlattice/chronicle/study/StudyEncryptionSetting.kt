package com.openlattice.chronicle.study

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.openlattice.chronicle.crypto.EnvelopeCipher

/**
 * Per-study payload-encryption configuration, bound to [StudySettingType.Encryption]
 * (HIPAA-2028 W2). A polymorphic [StudySetting] subtype riding the existing
 * `@class` discriminator — additive, so clients that don't know it keep working and a
 * study without this setting simply uploads plaintext (over TLS) as before.
 *
 * Carries the study **public** keys only (RSA + ML-KEM). The matching private keys live
 * exclusively in Vault and are fetched solely at authorized export/decrypt time — they must
 * never appear in this DTO, in study settings, or on a device. The [publicKeyPem] guard
 * rejects any PEM that smells like private key material.
 *
 * When [enabled] is false the other fields are advisory and the device stays on the
 * plaintext path. Enabling e2ee for a study is a DB-direct settings change (the web
 * surface is out of scope), consistent with the rest of the collection config.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public data class StudyEncryptionSetting(
    /** When true the device seals every upload batch and posts to the encrypted endpoint. */
    val enabled: Boolean = false,
    /** Current study key id; stamped into each [com.openlattice.chronicle.crypto.EncryptedEnvelope]. */
    val keyId: String = "",
    /** Algorithm identifier; defaults to [EnvelopeCipher.DEFAULT_ALG]. */
    val algorithm: String = EnvelopeCipher.DEFAULT_ALG,
    /** X.509 SubjectPublicKeyInfo PEM for the study RSA public key (classical half). Public only. */
    val publicKeyPem: String = "",
    /**
     * Base64 raw (FIPS 203) ML-KEM-1024 public key for the post-quantum half of the hybrid
     * key-wrap (see [EnvelopeCipher.decodeMlkemPublicKey]). Public only. Empty for legacy
     * RSA-only settings; required when [enabled] under the hybrid [algorithm].
     */
    val mlkemPublicKey: String = "",
) : StudySetting {
    init {
        if (enabled) {
            require(keyId.isNotBlank()) { "Enabled encryption setting requires a non-blank keyId" }
            require(algorithm.isNotBlank()) { "Enabled encryption setting requires a non-blank algorithm" }
            require(publicKeyPem.isNotBlank()) { "Enabled encryption setting requires a non-blank publicKeyPem" }
            require(mlkemPublicKey.isNotBlank() || algorithm == EnvelopeCipher.LEGACY_ALG) {
                "Enabled hybrid encryption setting requires a non-blank mlkemPublicKey"
            }
        }
        require(!publicKeyPem.contains("PRIVATE")) {
            "publicKeyPem must not contain private key material"
        }
    }
}
