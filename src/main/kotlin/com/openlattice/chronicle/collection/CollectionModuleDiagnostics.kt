package com.openlattice.chronicle.collection

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * Operational telemetry for a single collection module (design §1B.3).
 *
 * Hard rule (design §1B.3, refactor plan §5.2, guarded by Semgrep): this DTO holds
 * **no** `apiKey`, `MOBILE_SIGNING_SECRET`, raw `participantId`, or raw request
 * bodies. Participant references, where needed, are redacted (hash/prefix) and live
 * in [redactedParticipantRef] — never a raw identifier. [lastError] is a redacted
 * message only.
 *
 * @author uzaira0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public data class CollectionModuleDiagnostics(
    /** The module these diagnostics describe. */
    val moduleId: CollectionModuleId,
    /** The module's privacy classification. */
    val privacyClass: CollectionPrivacyClass,
    /** Epoch millis of the module's last run, or `null` if it has never run. */
    val lastRunEpochMs: Long? = null,
    /** Outcome label of the last run (e.g. "OK", "SKIPPED", "RETRY", "FAILED"). */
    val lastResult: String? = null,
    /** Items collected on the last run. */
    val itemsCollected: Int = 0,
    /** Current queue depth for the module's data stream. */
    val queueDepth: Int = 0,
    /** Redacted message of the last error, or `null` if the last run succeeded. */
    val lastError: String? = null,
    /**
     * Redacted participant reference (hash or prefix), or `null`. Never a raw
     * `participantId` (design §1B.3).
     */
    val redactedParticipantRef: String? = null,
    /** Names of metrics not yet tracked locally (placeholder until counters exist). */
    val notTracked: Set<String> = emptySet(),
) {
    init {
        require(privacyClass == moduleId.privacyClass) {
            "CollectionModuleDiagnostics.privacyClass ($privacyClass) must match " +
                "moduleId ${moduleId.id} privacy class (${moduleId.privacyClass})"
        }
    }
}
