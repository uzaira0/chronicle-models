package com.openlattice.chronicle.collection

import java.time.OffsetDateTime
import java.util.UUID

/**
 * One append-only record in a study's participant collection-acknowledgment trail —
 * persisted server-side when a device POSTs a [CollectionAcknowledgment], and surfaced
 * to researchers alongside the immutable settings audit (design
 * `docs/COLLECTION-LOOP-CLOSURE-DESIGN.md` §5.3, §11).
 *
 * Two timestamps are kept deliberately distinct:
 * - [recordedAt] — the **server-stamped** authoritative receipt time; this is the
 *   tamper-evident anchor for "when each participant was made aware of and accepted"
 *   a module.
 * - [acknowledgedAt] — the **device-reported** time, carried through for diagnostics
 *   but never trusted as authoritative (a device clock is spoofable).
 *
 * @author uzaira0
 */
public data class CollectionAcknowledgmentEntry(
    val id: UUID = UUID.randomUUID(),
    val studyId: UUID,
    val participantId: String,
    val sourceDeviceId: String,
    /** The modules the participant **accepted** in this snapshot. */
    val acknowledgedModules: Set<CollectionModuleId>,
    /** Device-reported acknowledgment time (advisory). */
    val acknowledgedAt: OffsetDateTime,
    /** The modules the participant **declined** in this snapshot (per-module consent design §3.3). */
    val declinedModules: Set<CollectionModuleId> = emptySet(),
    /** Study-enabled per-sensor modules physically unavailable on the reporting device. */
    val unavailableModules: Set<CollectionModuleId> = emptySet(),
    /** What produced this decision (enrollment / toggle / settings-change / withdrawal). */
    val trigger: ConsentTrigger = ConsentTrigger.ENROLLMENT,
    /** Server-stamped authoritative receipt time (the audit anchor). */
    val recordedAt: OffsetDateTime = OffsetDateTime.now(),
    val appVersion: String? = null,
    /** Settings revision whose policy the participant accepted or declined. */
    val settingsVersion: Int? = null,
    /** Study consent/disclosure version presented with the settings snapshot. */
    val disclosureVersion: String? = null,
    /** Lowercase SHA-256 digest of the authoritative enrollment manifest reviewed by the participant. */
    val manifestDigest: String? = null,
) {
    init {
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
