package com.openlattice.chronicle.collection

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.openlattice.chronicle.study.StudySetting
import com.openlattice.chronicle.study.StudySettingType

/**
 * Generalized Android data collection setting, bound to [StudySettingType.DataCollection].
 *
 * Deserialization is tolerant:
 *  - unknown module IDs in the JSON `modules` map are **ignored**, not fatal;
 *  - duplicate logical entries are normalized deterministically (last value wins,
 *    keyed by the resolved [CollectionModuleId]).
 *
 * Hard constraint: this DTO and every nested DTO carry **no** `apiKey`, signing
 * secret, or `participantId` field.
 *
 * @author uzaira0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public data class AndroidDataCollectionSetting(
    /**
     * Per-module settings. Keyed by [CollectionModuleId]; any module absent here
     * falls back to [CollectionDefaults]. Unknown wire IDs are dropped on read.
     */
    val modules: Map<CollectionModuleId, CollectionModuleSetting> = emptyMap(),
    /** Schema discriminator for forward migration. Defaults to [CURRENT_VERSION]. */
    val version: Int = CURRENT_VERSION,
    /** Server-controlled revision of the actual settings content, distinct from [version]. */
    val settingsVersion: Int = INITIAL_SETTINGS_VERSION,
) : StudySetting {

    init {
        require(settingsVersion > 0) { "settingsVersion must be positive: $settingsVersion" }
    }

    /** Whether any per-sensor module is explicitly configured. Omitted sensors stay disabled. */
    public fun hasAnySensorModule(): Boolean =
        modules.keys.any(SensorCollectionModules::isSensorModule)

    /**
     * The authoritative module settings represented by this manifest/configuration after filling
     * omitted active modules with [CollectionDefaults]. This is the shared server/client contract
     * for enrollment consent evidence; device-local legacy preferences are deliberately excluded.
     */
    public fun effectiveModules(): Map<CollectionModuleId, CollectionModuleSetting> {
        val hasExplicitSensorScope = hasAnySensorModule()
        return androidSupportedModuleIds.associateWith { moduleId ->
            modules[moduleId] ?: if (
                hasExplicitSensorScope && SensorCollectionModules.isSensorModule(moduleId)
            ) {
                CollectionDefaults.moduleSetting(moduleId, enabled = false)
            } else {
                CollectionDefaults.moduleSetting(moduleId)
            }
        }
    }

    /** Active modules enabled by [effectiveModules], in the catalog's stable order. */
    public fun effectiveEnabledModuleIds(): Set<CollectionModuleId> =
        effectiveModules()
            .filterValues(CollectionModuleSetting::enabled)
            .keys
            .toCollection(LinkedHashSet())

    public companion object {
        /** Active modules implemented by the Android client. */
        @JvmField
        public val androidSupportedModuleIds: Set<CollectionModuleId> =
            CollectionModuleId.activeModules

        /**
         * Current schema version of this setting shape.
         *
         * v2 adds the per-module `required` flag. Reads
         * stay tolerant of any version — the device diffs by `settingVersion` — so this
         * is informational; old payloads without `required` decode as all-optional.
         */
        public const val CURRENT_VERSION: Int = 2
        public const val INITIAL_SETTINGS_VERSION: Int = 1

        /**
         * Tolerant Jackson factory.
         *
         * Accepts a raw `Map<String, CollectionModuleSetting>` so an unknown module
         * ID string does not break enum-key decoding. Unknown IDs are dropped;
         * known IDs that collide deterministically resolve to the last entry.
         */
        @JvmStatic
        @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
        public fun fromJson(
            @JsonProperty("modules") rawModules: Map<String, CollectionModuleSetting>?,
            @JsonProperty("version") version: Int?,
            @JsonProperty("settingsVersion") settingsVersion: Int? = null,
        ): AndroidDataCollectionSetting {
            val normalized = LinkedHashMap<CollectionModuleId, CollectionModuleSetting>()
            rawModules?.forEach { (key, value) ->
                val moduleId = CollectionModuleId.fromIdOrNull(key)
                if (moduleId != null) {
                    // Last value wins — deterministic duplicate normalization.
                    normalized[moduleId] = value
                }
            }
            return AndroidDataCollectionSetting(
                modules = normalized,
                version = version ?: CURRENT_VERSION,
                settingsVersion = settingsVersion ?: INITIAL_SETTINGS_VERSION,
            )
        }

    }
}
