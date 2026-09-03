package com.openlattice.chronicle.collection

import com.openlattice.chronicle.android.AndroidSensorSetting

/**
 * Safe default factory for data collection settings (design §1B.4).
 *
 * "Safe" means: a missing or invalid setting disables the affected module — it
 * **never** silently enables a privacy-sensitive module. The default `enabled`
 * value for a module is taken from its privacy class
 * ([CollectionPrivacyClass.defaultEnabled]); [CollectionPrivacyClass.PHYSICAL_TELEMETRY]
 * and [CollectionPrivacyClass.LOCAL_PARTICIPANT_LABEL] therefore default to `false`.
 *
 * @author uzaira0
 */
public object CollectionDefaults {

    /**
     * Default `enabled` flag for [moduleId]. A module-level
     * [CollectionModuleId.defaultEnabledOverride] wins when present; otherwise the value is
     * derived from the module's privacy class. Privacy-sensitive classes are `false`, and a
     * module may opt out of an otherwise-on class default (e.g. `in_app_activity_class`).
     */
    public fun defaultEnabled(moduleId: CollectionModuleId): Boolean =
        moduleId.defaultEnabledOverride ?: moduleId.privacyClass.defaultEnabled

    /**
     * A safe [CollectionModuleSetting] for [moduleId]. When [enabled] is omitted,
     * the privacy-class default is used.
     *
     * A per-sensor module gets a default [AndroidSensorSetting] sampling policy (its own
     * sensor + the 5 Hz / 30 s active / 300 s period legacy defaults) so its rate/duty are
     * always populated; non-sensor modules carry no `sensorPolicy`.
     */
    public fun moduleSetting(
        moduleId: CollectionModuleId,
        enabled: Boolean = defaultEnabled(moduleId),
    ): CollectionModuleSetting = CollectionModuleSetting(
        enabled = enabled,
        collectionCadence = CollectionCadence.DEFAULT_COLLECTION,
        uploadCadence = CollectionCadence.DEFAULT_UPLOAD,
        batteryPolicy = BatteryPolicy.DEFAULT,
        networkPolicy = NetworkPolicy.DEFAULT,
        sensorPolicy = SensorCollectionModules.sensorTypeOf(moduleId)?.let {
            AndroidSensorSetting(sensors = setOf(it))
        },
        interactionPolicy = if (moduleId == CollectionModuleId.INTERACTION_EVENTS) {
            InteractionPolicy.DEFAULT
        } else {
            null
        },
    )

    /**
     * A safe [AndroidDataCollectionSetting] populated for every active module with
     * privacy-class default enablement. Reserved/inactive IDs are excluded.
     */
    public fun androidDataCollectionSetting(): AndroidDataCollectionSetting =
        AndroidDataCollectionSetting(
            modules = CollectionModuleId.activeModules.associateWith { moduleSetting(it) },
        )
}
