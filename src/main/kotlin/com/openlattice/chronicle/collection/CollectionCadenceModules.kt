package com.openlattice.chronicle.collection

/**
 * The pull/periodic modules whose per-module collection interval
 * (`CollectionModuleSetting.collectionCadence.intervalSeconds`) the Android runtime enforces
 * with a last-run gate. Every other module ignores the interval.
 *
 * Single source: Android gates exactly these, and the web study form shows the interval control
 * for exactly these (through the generated `INTERVAL_GATED_COLLECTION_MODULE_IDS`, kept in parity
 * by the ontology `intervalGated` annotation and tests/security/contract-drift-diff.py).
 */
public object CollectionCadenceModules {
    public val intervalGated: List<CollectionModuleId> = listOf(
        CollectionModuleId.CONNECTIVITY_STATE,
        CollectionModuleId.DEVICE_SETTINGS,
        CollectionModuleId.APP_NETWORK_USAGE,
        CollectionModuleId.HEALTH_CONNECT,
        CollectionModuleId.BATTERY_TELEMETRY,
    )
}
