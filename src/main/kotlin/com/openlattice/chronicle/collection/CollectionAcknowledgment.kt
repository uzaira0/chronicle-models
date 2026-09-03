package com.openlattice.chronicle.collection

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.OffsetDateTime

/**
 * A participant's on-device per-module consent decision snapshot — the request body the
 * Android app POSTs back when its consent picture changes (design
 * `docs/COLLECTION-LOOP-CLOSURE-DESIGN.md` §5.2, generalized by the
 * per-module consent redesign, 2026-06-10).
 *
 * A snapshot records, for the modules in play at that moment, which the participant
 * **accepted** ([acknowledgedModules]), which they **declined** ([declinedModules]), and which
 * study-enabled hardware sensors are **unavailable on this device** ([unavailableModules]),
 * plus the [trigger] that produced the decision (enrollment, a self-service toggle, a
 * study-setting-driven re-decision, or withdrawal). The server diffs these into a full
 * per-participant consent history.
 *
 * [acknowledgedAt] is the **device-reported** wall-clock time of the decision; it is
 * advisory only (a device clock is spoofable). The server stamps its own authoritative
 * receipt time when it persists the entry — see [CollectionAcknowledgmentEntry.recordedAt].
 *
 * Carries no `apiKey`, signing secret, or other credential — auth rides the
 * `X-Api-Key` / `X-Chronicle-Device-Id` headers like the other v4 android writes.
 *
 * @author uzaira0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public data class CollectionAcknowledgment(
    /** The modules the participant **accepted** in this snapshot. */
    val acknowledgedModules: Set<CollectionModuleId>,
    /** Device-reported wall-clock time of the decision (advisory; see class doc). */
    val acknowledgedAt: OffsetDateTime,
    /**
     * The modules the participant **declined** in this snapshot. Defaults empty so older
     * payloads (accept-only) decode unchanged.
     */
    val declinedModules: Set<CollectionModuleId> = emptySet(),
    /**
     * Study-enabled per-sensor modules that this device cannot collect because the physical
     * sensor is absent. This is device-capability evidence, not a participant decision.
     */
    val unavailableModules: Set<CollectionModuleId> = emptySet(),
    /** What produced this decision. Defaults to [ConsentTrigger.ENROLLMENT] for legacy payloads. */
    val trigger: ConsentTrigger = ConsentTrigger.ENROLLMENT,
    /** Optional app version string, for diagnostics. */
    val appVersion: String? = null,
    /** Settings revision whose policy the participant accepted or declined. */
    val settingsVersion: Int? = null,
    /** Study consent/disclosure version presented with the settings snapshot. */
    val disclosureVersion: String? = null,
    /** Lowercase SHA-256 digest of the authoritative enrollment manifest reviewed by the participant. */
    val manifestDigest: String? = null,
) {
    init {
        require(
            acknowledgedModules.isNotEmpty() ||
                declinedModules.isNotEmpty() ||
                unavailableModules.isNotEmpty(),
        ) {
            "CollectionAcknowledgment must record at least one accepted, declined, or unavailable module"
        }
        require(acknowledgedModules.intersect(declinedModules).isEmpty()) {
            "A module cannot be both accepted and declined in the same snapshot"
        }
        require(acknowledgedModules.intersect(unavailableModules).isEmpty()) {
            "A module cannot be both accepted and unavailable in the same snapshot"
        }
        require(declinedModules.intersect(unavailableModules).isEmpty()) {
            "A module cannot be both declined and unavailable in the same snapshot"
        }
        require(unavailableModules.all(SensorCollectionModules::isSensorModule)) {
            "Only per-sensor hardware modules may be reported unavailable"
        }
        require(settingsVersion == null || settingsVersion > 0) {
            "settingsVersion must be positive when supplied: $settingsVersion"
        }
        require(disclosureVersion == null || disclosureVersion.isNotBlank()) {
            "disclosureVersion must not be blank when supplied"
        }
        require(manifestDigest == null || MANIFEST_DIGEST.matches(manifestDigest)) {
            "manifestDigest must be a lowercase SHA-256 hex digest when supplied"
        }
        require((disclosureVersion == null) == (manifestDigest == null)) {
            "disclosureVersion and manifestDigest must be supplied together"
        }
    }

    private companion object {
        val MANIFEST_DIGEST: Regex = Regex("^[0-9a-f]{64}$")
    }
}
