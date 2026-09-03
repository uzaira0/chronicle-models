package com.openlattice.chronicle.collection

/**
 * Network constraints applied to a module's upload behavior (design §1B.2).
 *
 * @author uzaira0
 */
public data class NetworkPolicy(
    /** Require an unmetered (e.g. Wi-Fi) connection before uploading. */
    val requireUnmetered: Boolean = false,
    /** Require any connected network before uploading. */
    val requireConnected: Boolean = true,
) {
    public companion object {
        /** Default policy: connected network required, metering ignored. */
        public val DEFAULT: NetworkPolicy = NetworkPolicy()
    }
}
