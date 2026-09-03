package com.openlattice.chronicle.collection

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.OffsetDateTime

/** What produced a [AndroidConnectivityStateEvent], from `ConnectivityManager.NetworkCallback`. */
public enum class ConnectivityEventType { AVAILABLE, LOST, CHANGED, SNAPSHOT }

/** Active network transport, collapsed from `NetworkCapabilities.TRANSPORT_*`. */
public enum class NetworkTransport { WIFI, CELLULAR, ETHERNET, VPN, BLUETOOTH, NONE, OTHER }

/**
 * One content-free connectivity-state sample produced by the `connectivity_state` collection module.
 *
 * Derived from `ConnectivityManager` / `NetworkCapabilities`: the active transport plus metered and
 * validated-internet flags. **No SSID, BSSID, IP, or cell identifiers** are captured — those would be
 * a location proxy. `DEVICE_STATE_METADATA`-class, opt-in (default OFF). No `apiKey`, signing secret,
 * or `participantId` field. Unknown JSON fields are ignored for forward compatibility.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public data class AndroidConnectivityStateEvent(
    /** Stable per-event identifier, used for de-duplication on insert/upload. */
    public val id: String,
    /** Sample time in UTC. */
    public val timestamp: OffsetDateTime,
    /** Device-default time-zone id at sample time. */
    public val timezone: String,
    /** The transition that produced this sample. */
    public val eventType: ConnectivityEventType,
    /** Active network transport at sample time. */
    public val transport: NetworkTransport,
    /** Whether a network was connected at sample time. */
    public val connected: Boolean,
    /** Whether the active network is metered (`isActiveNetworkMetered`); when known. */
    public val metered: Boolean? = null,
    /** Whether the active network has validated internet (`NET_CAPABILITY_VALIDATED`); when known. */
    public val validated: Boolean? = null,
) {
    init {
        require(id.isNotBlank()) { "AndroidConnectivityStateEvent.id must not be blank" }
        require(timezone.isNotBlank()) { "AndroidConnectivityStateEvent.timezone must not be blank" }
    }
}
