package com.openlattice.chronicle.collection

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.OffsetDateTime

/** Network type a [AndroidAppNetworkUsageEvent] bucket is for, from `NetworkStatsManager`. */
public enum class NetworkUsageType { WIFI, CELLULAR, OTHER }

/**
 * One per-app network-usage sample produced by the `app_network_usage` collection module
 * (`NetworkStatsManager`).
 *
 * **Volume counts only — never content.** The sample carries the transmitted/received **byte counts**
 * for one app over one time bucket; it has zero visibility into payloads, destinations, domains, or
 * URLs. Reuses the Usage Access permission `usage_events` already holds. `BEHAVIORAL_METADATA`-class,
 * opt-in (default OFF). No `apiKey`, signing secret, or `participantId` field. Unknown JSON fields are
 * ignored for forward compatibility.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public data class AndroidAppNetworkUsageEvent(
    /** Stable per-event identifier, used for de-duplication on insert/upload. */
    public val id: String,
    /** Bucket read time in UTC. */
    public val timestamp: OffsetDateTime,
    /** Device-default time-zone id at sample time. */
    public val timezone: String,
    /** Package whose usage this is; or a `uid:N` form when the package is unresolvable. */
    public val packageName: String,
    /** Which network the bytes were transferred over. */
    public val networkType: NetworkUsageType,
    /** Received bytes over the bucket. Non-negative. */
    public val rxBytes: Long,
    /** Transmitted bytes over the bucket. Non-negative. */
    public val txBytes: Long,
    /** Bucket start (epoch millis). */
    public val bucketStartMillis: Long,
    /** Bucket end (epoch millis). */
    public val bucketEndMillis: Long,
) {
    init {
        require(id.isNotBlank()) { "AndroidAppNetworkUsageEvent.id must not be blank" }
        require(timezone.isNotBlank()) { "AndroidAppNetworkUsageEvent.timezone must not be blank" }
        require(packageName.isNotBlank()) { "AndroidAppNetworkUsageEvent.packageName must not be blank" }
        require(rxBytes >= 0) { "AndroidAppNetworkUsageEvent.rxBytes must be non-negative: $rxBytes" }
        require(txBytes >= 0) { "AndroidAppNetworkUsageEvent.txBytes must be non-negative: $txBytes" }
        require(bucketEndMillis >= bucketStartMillis) {
            "AndroidAppNetworkUsageEvent bucket end ($bucketEndMillis) must be >= start ($bucketStartMillis)"
        }
    }
}
