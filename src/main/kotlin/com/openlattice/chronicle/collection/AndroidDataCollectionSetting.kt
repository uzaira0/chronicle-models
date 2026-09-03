package com.openlattice.chronicle.collection

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.openlattice.chronicle.android.AndroidSensorSetting
import com.openlattice.chronicle.study.StudySetting
import com.openlattice.chronicle.study.StudySettingType

/**
 * Generalized Android data collection setting, bound to [StudySettingType.DataCollection]
 * (design §1B.2).
 *
 * Added additively as a new polymorphic [StudySetting] subtype. Servers and clients
 * that only know `AndroidSensor` keep working; the legacy `AndroidSensor` setting is
 * unchanged. Polymorphic (de)serialization rides the existing `@class`-discriminated
 * mechanism declared on [StudySetting] — no new discriminator is introduced.
 *
 * Deserialization is tolerant per design §1B.4:
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

    /**
     * Whether this config has adopted the per-sensor model — i.e. it carries at least one
     * explicit per-sensor (`sensor_*`) module entry. When true the config is the SOLE
     * authority on which sensors collect: a sensor it OMITS is an explicit "not in this
     * study", so the legacy device-wide `AndroidSensor` bridge must not re-enable it
     * (CollectionSettingsResolver §1B.4) and enrollment must not persist a legacy sensor
     * blob that would. Legacy (un-migrated) configs carry no per-sensor entry → false.
     */
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
        /**
         * Active modules implemented by the Android client. `ambient_audio` is intentionally
         * iOS-only and therefore cannot be part of an Android enrollment acknowledgment.
         */
        @JvmField
        public val androidSupportedModuleIds: Set<CollectionModuleId> =
            CollectionModuleId.activeModules
                .filterNotTo(LinkedHashSet()) { it == CollectionModuleId.AMBIENT_AUDIO }

        /**
         * Current schema version of this setting shape.
         *
         * v2 adds the per-module `required` flag (per-module consent design §3.2). Reads
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

        /**
         * Legacy bridge (design §1B.4): derives an [AndroidDataCollectionSetting]
         * from a legacy [AndroidSensorSetting] when no `DataCollection` setting
         * exists.
         *
         * Each sensor named in the legacy setting enables its own per-sensor module
         * (per-sensor consent redesign, 2026-06-11), carrying the legacy study-wide
         * sampling rate + duty cycle as that sensor's per-sensor policy. An empty
         * (`null` or no sensors) legacy setting yields **no** enabled modules. No
         * non-sensor module is enabled by this bridge — privacy-sensitive modules are
         * never enabled implicitly.
         */
        @JvmStatic
        public fun fromLegacy(legacy: AndroidSensorSetting?): AndroidDataCollectionSetting {
            val sensors = legacy?.sensors ?: emptySet()
            val modules = sensors.mapNotNull { sensorType ->
                val moduleId = SensorCollectionModules.moduleFor(sensorType)
                // Retired sensor modules (e.g. the Samsung vendor sensors) are never re-enabled by
                // the legacy bridge — they decode but are not offered/collected afresh.
                if (!moduleId.active) return@mapNotNull null
                moduleId to CollectionDefaults.moduleSetting(moduleId, enabled = true).copy(
                    sensorPolicy = AndroidSensorSetting(
                        sensors = setOf(sensorType),
                        samplingRateHz = legacy!!.samplingRateHz,
                        dutyCycleActiveSeconds = legacy.dutyCycleActiveSeconds,
                        dutyCyclePeriodSeconds = legacy.dutyCyclePeriodSeconds,
                    ),
                )
            }.toMap()
            return AndroidDataCollectionSetting(modules = modules)
        }
    }
}
