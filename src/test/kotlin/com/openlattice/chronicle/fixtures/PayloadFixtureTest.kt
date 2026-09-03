package com.openlattice.chronicle.fixtures

import com.fasterxml.jackson.core.JacksonException
import com.fasterxml.jackson.databind.ObjectMapper
import com.openlattice.chronicle.collection.CollectionModuleId
import com.openlattice.chronicle.collection.TestMappers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

/**
 * JVM gates for the canonical payload fixtures (shared-contracts Tranche 3,
 * docs/shared-contracts/03-domain-contract-catalog.md "Upload Envelope Schema
 * Versions and Fixture Names").
 *
 * The fixtures under `fixtures/payloads/` are the cross-language parity baseline:
 * Swift, TypeScript, and backend ingestion checks consume the same files in later
 * tranches. This test proves the JVM baseline: the registry validates, every
 * fixture decodes with the shared Jackson conventions ([TestMappers]), decode ->
 * encode -> decode is stable, and every `invalid-*` fixture is rejected.
 */
class PayloadFixtureTest {

    private val json = TestMappers.json()
    private val repoRoot = resolveRepoRoot()
    private val knownModuleIds = CollectionModuleId.entries.map { it.id }.toSet()

    // ===== Registry validation =====

    @Test
    fun registryLoadsAndEveryModuleIdFixtureFileAndJvmClassResolves() {
        val registry = FixtureRegistry.load(repoRoot, knownModuleIds)
        assertEquals(FixtureRegistry.SCHEMA_VERSION, registry.schemaVersion)
        assertTrue("registry must declare fixture families", registry.families.isNotEmpty())

        // Module-id membership and fixture-file existence are already enforced by
        // FixtureRegistry.load (it throws on violation); only the checks the loader
        // does NOT perform live here.
        registry.families.forEach { family ->
            family.jvmClass?.let { className ->
                // Must resolve on the models classpath, or the registry is lying.
                Class.forName(className)
            }
            assertTrue(
                "family '${family.family}' must have a valid.json fixture",
                family.fixtureFiles.any { it.endsWith("/valid.json") },
            )
        }
    }

    @Test
    fun registryCoversAllDocRequiredFamilies() {
        val required = setOf(
            "android-sensor-data", "ios-sensorkit-data", "screen-time-usage",
            "device-settings", "battery-telemetry", "interaction-events",
            "app-audio-activity", "app-audio-content", "ambient-audio", "notification-activity",
            "sleep-events", "activity-recognition", "health-metrics",
            "connectivity-state", "app-network-usage", "encrypted-payloads",
            "ios-device", "user-identification",
        )
        val families = FixtureRegistry.load(repoRoot, knownModuleIds).families.map { it.family }.toSet()
        assertEquals("registry families must match the doc-03 catalog set", required, families)
    }

    @Test
    fun malformedRegistryEntriesAreRejected() {
        val validFamily = """
            {
              "family": "battery-telemetry",
              "collectionModuleId": "battery_telemetry",
              "payloadSchemaVersion": 1,
              "jvmClass": "com.openlattice.chronicle.collection.BatterySample",
              "fixtureFiles": ["fixtures/payloads/battery-telemetry/valid.json"],
              "timeSemantics": "OffsetDateTime ISO-8601 with offset",
              "scopingFields": ["studyId", "participantId"],
              "backendHandler": "BatteryTelemetryUploadService",
              "backendTable": "battery_telemetry"
            }
        """.trimIndent()

        fun registryWith(families: String, schemaVersion: String = FixtureRegistry.SCHEMA_VERSION) =
            """{"schemaVersion": "$schemaVersion", "families": [$families]}"""

        val malformed = mapOf(
            "wrong schemaVersion" to registryWith(validFamily, schemaVersion = "bogus/v9"),
            "empty families" to registryWith(""),
            "duplicate family names" to registryWith("$validFamily, $validFamily"),
            "unknown module id" to registryWith(
                validFamily.replace("\"battery_telemetry\",", "\"not_a_module\","),
            ),
            "non-kebab-case family" to registryWith(
                validFamily.replace("\"family\": \"battery-telemetry\"", "\"family\": \"Battery_Telemetry\""),
            ),
            "payloadSchemaVersion below 1" to registryWith(
                validFamily.replace("\"payloadSchemaVersion\": 1", "\"payloadSchemaVersion\": 0"),
            ),
            "empty fixtureFiles" to registryWith(
                validFamily.replace(
                    "[\"fixtures/payloads/battery-telemetry/valid.json\"]",
                    "[]",
                ),
            ),
            "fixture file outside family dir" to registryWith(
                validFamily.replace(
                    "fixtures/payloads/battery-telemetry/valid.json",
                    "fixtures/payloads/other-family/valid.json",
                ),
            ),
            "blank backendTable" to registryWith(
                validFamily.replace("\"backendTable\": \"battery_telemetry\"", "\"backendTable\": \"\""),
            ),
            "missing timeSemantics" to registryWith(
                validFamily.replace("\"timeSemantics\": \"OffsetDateTime ISO-8601 with offset\",", ""),
            ),
        )

        malformed.forEach { (reason, registryJson) ->
            try {
                FixtureRegistry.parse(registryJson, knownModuleIds)
                fail("Expected malformed registry to be rejected: $reason")
            } catch (expected: IllegalArgumentException) {
                // expected
            }
        }
    }

    // ===== Round-trip (JVM canonical baseline) =====

    @Test
    fun validFixturesRoundTripSemantically() {
        val registry = FixtureRegistry.load(repoRoot, knownModuleIds)
        var familiesChecked = 0
        registry.families.filter { it.jvmClass != null }.forEach { family ->
            val clazz = Class.forName(family.jvmClass)
            family.fixtureFiles.filterNot { it.contains("/invalid-") }.forEach { path ->
                val first = decodeFixture(json, File(repoRoot, path).readText(), clazz)
                assertTrue("fixture $path decoded to nothing", first.isNotEmpty())
                val reEncoded = json.writeValueAsString(if (first.size == 1) first.single() else first)
                val second = decodeFixture(json, reEncoded, clazz)
                assertEquals("semantic round-trip drift for $path", first, second)
            }
            familiesChecked++
        }
        // Floor, not equality-with-the-filter (which is true by construction): if a
        // registry edit nulls out jvmClass en masse, zero round-trips must FAIL here,
        // not pass vacuously. 15 of 16 families carry a jvmClass today.
        assertTrue(
            "round-trip coverage collapsed: only $familiesChecked families exercised (expected >= 12)",
            familiesChecked >= 12,
        )
    }

    // ===== Negative fixtures =====

    @Test
    fun invalidFixturesFailDecodeOrInitValidation() {
        val registry = FixtureRegistry.load(repoRoot, knownModuleIds)
        val invalidFixtures = registry.families
            .filter { it.jvmClass != null }
            .flatMap { family ->
                family.fixtureFiles.filter { it.contains("/invalid-") }.map { it to family.jvmClass!! }
            }
        assertTrue("expected malformed fixture coverage across families", invalidFixtures.size >= 6)

        invalidFixtures.forEach { (path, className) ->
            val clazz = Class.forName(className)
            try {
                decodeFixture(json, File(repoRoot, path).readText(), clazz)
                fail("Expected invalid fixture to fail decode/validation: $path")
            } catch (expected: JacksonException) {
                // Jackson decode failure, including wrapped init{} require() violations.
            } catch (expected: IllegalArgumentException) {
                // Direct DTO validation failure outside Jackson's wrapping.
            }
        }
    }

    // ===== Active-module coverage =====

    @Test
    fun registryCoversEveryActiveModuleWithABackendUploadTable() {
        // Canonical module -> fixture-family expectation, hardcoded so drift is loud:
        // adding an active module (or an upload table) without a fixture family must
        // break this test until the registry or the documented exceptions change.
        val moduleToFamily = mapOf(
            "sensor_accelerometer" to "android-sensor-data",
            "sensor_gyroscope" to "android-sensor-data",
            "sensor_magnetometer" to "android-sensor-data",
            "sensor_gravity" to "android-sensor-data",
            "sensor_linear_acceleration" to "android-sensor-data",
            "sensor_rotation_vector" to "android-sensor-data",
            "sensor_step_counter" to "android-sensor-data",
            "sensor_light" to "android-sensor-data",
            "sensor_proximity" to "android-sensor-data",
            "sensor_significant_motion" to "android-sensor-data",
            "sensor_tilt_detector" to "android-sensor-data",
            "sensor_screen_orientation" to "android-sensor-data",
            "battery_telemetry" to "battery-telemetry",
            "interaction_events" to "interaction-events",
            "audio_activity" to "app-audio-activity",
            "audio_content" to "app-audio-content",
            "ambient_audio" to "ambient-audio",
            "notification_activity" to "notification-activity",
            "sleep" to "sleep-events",
            "activity_recognition" to "activity-recognition",
            "health_connect" to "health-metrics",
            "connectivity_state" to "connectivity-state",
            "app_network_usage" to "app-network-usage",
            "device_settings" to "device-settings",
            "user_identification" to "user-identification",
        )

        // Documented exceptions: active modules whose payloads do not (yet) have a
        // fixture family in the doc-03 catalog.
        val documentedExceptions = mapOf(
            "usage_events" to "legacy ChronicleUsageEvent pipeline (AppDataUploadService -> chronicle_usage_events); fixture family deferred to the legacy-usage tranche",
            "in_app_activity_class" to "activity-class refinement riding usage_events payloads; no separate upload envelope",
            "device_lifecycle" to "device-state transitions ride the usage-event pipeline; no separate upload envelope",
            "upload_telemetry" to "operational diagnostics only; no backend upload table",
            "sensor_availability" to "device capability snapshot (android_device_sensor_availability); not in the doc-03 fixture catalog",
            "questionnaire" to "survey pipeline, not a collection upload payload",
        )

        val activeIds = CollectionModuleId.activeModules.map { it.id }.toSet()
        assertEquals(
            "every active CollectionModuleId must be fixture-mapped or a documented exception",
            activeIds,
            moduleToFamily.keys + documentedExceptions.keys,
        )

        val registry = FixtureRegistry.load(repoRoot, knownModuleIds)
        val familiesByName = registry.families.associateBy { it.family }
        moduleToFamily.forEach { (moduleId, familyName) ->
            val family = familiesByName[familyName]
            assertTrue("module '$moduleId' expects registry family '$familyName'", family != null)
            family!!.collectionModuleId?.let { declared ->
                assertEquals(
                    "family '$familyName' declares a different module than expected",
                    moduleId,
                    declared,
                )
            }
        }

        // Every non-null collectionModuleId in the registry must be an ACTIVE module
        // (no family may be pinned to a reserved/retired id).
        registry.families.mapNotNull { it.collectionModuleId }.forEach { moduleId ->
            assertTrue(
                "registry family references non-active module id '$moduleId'",
                moduleId in activeIds,
            )
        }
    }

    companion object {
        /** Decodes a fixture (root array -> element list, root object -> singleton list). */
        private fun decodeFixture(mapper: ObjectMapper, text: String, clazz: Class<*>): List<Any> {
            val root = mapper.readTree(text)
            return if (root.isArray) {
                require(root.size() > 0) { "fixture array must not be empty" }
                root.map { mapper.treeToValue(it, clazz) }
            } else {
                listOf(mapper.treeToValue(root, clazz))
            }
        }

        /**
         * Resolves the chronicle-models repo root. Prefers the `chronicle.models.root`
         * system property (set by build.gradle for all Test tasks) and falls back to
         * walking up from the working directory until `fixtures/payloads/registry.json`
         * is found, so IDE test runs work without Gradle.
         */
        fun resolveRepoRoot(): File {
            System.getProperty("chronicle.models.root")?.let { return File(it) }
            var dir: File? = File(System.getProperty("user.dir")).absoluteFile
            while (dir != null) {
                if (File(dir, "fixtures/payloads/registry.json").isFile) return dir
                dir = dir.parentFile
            }
            error("Unable to locate chronicle-models repo root (fixtures/payloads/registry.json)")
        }
    }
}
