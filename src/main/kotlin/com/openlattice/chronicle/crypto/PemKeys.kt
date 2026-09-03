package com.openlattice.chronicle.crypto

import java.security.KeyFactory
import java.security.interfaces.RSAPublicKey
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

/**
 * PEM parsing for the study **public** key, shared by the device (which fetches the key
 * from study settings) and the backend. Mirrors the server's existing JWT PEM loader so
 * the two stay consistent. Only the public (X.509 SubjectPublicKeyInfo) key is ever
 * handled here — the private key never leaves Vault.
 */
public object PemKeys {

    /** Parse an `-----BEGIN PUBLIC KEY-----` X.509 PEM into an [RSAPublicKey]. */
    public fun rsaPublicKey(pem: String): RSAPublicKey {
        val der = Base64.getMimeDecoder().decode(stripPem(pem, "PUBLIC KEY"))
        val spec = X509EncodedKeySpec(der)
        return KeyFactory.getInstance("RSA").generatePublic(spec) as RSAPublicKey
    }

    private fun stripPem(pem: String, label: String): String = pem
        .replace("-----BEGIN $label-----", "")
        .replace("-----END $label-----", "")
        .replace(Regex("\\s"), "")
}
