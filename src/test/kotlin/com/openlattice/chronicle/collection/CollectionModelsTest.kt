package com.openlattice.chronicle.collection

import com.openlattice.chronicle.android.AndroidSensorSetting
import com.openlattice.chronicle.android.AndroidSensorType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.time.OffsetDateTime

/**
 * Model unit tests for the Phase 2 data collection contracts (refactor plan §5.1).
 */
@Suppress("DEPRECATION")
class CollectionModelsTest {

    // ===== CollectionModuleId =====

    @Test fun testActiveModuleIdCount() {
        // 19 non-sensor active modules (the prior 12 — incl. interaction_events + in_app_activity_class
        // + the app-audio pair + notification_activity — plus the 6 sensing-expansion modules: sleep,
        // activity_recognition, health_connect, connectivity_state, app_network_usage, device_settings —
        // plus ambient_audio, the iOS SoundAnalysis on-device sound-classification module)
        // + 12 per-sensor modules (hardware_sensors + the 2 Samsung vendor sensors are decode-only).
        assertEquals(31, CollectionModuleId.activeModules.size)
    }

    @Test fun testActiveModuleIdStrings() {
        val ids = CollectionModuleId.activeModules.map { it.id }.toSet()
        assertEquals(
            setOf(
                "usage_events", "device_lifecycle",
                "user_identification", "upload_telemetry", "sensor_availability",
                "questionnaire", "battery_telemetry", "interaction_events", "in_app_activity_class",
                "audio_activity", "audio_content", "notification_activity", "ambient_audio",
                "sleep", "activity_recognition", "health_connect",
                "connectivity_state", "app_network_usage", "device_settings",
                "sensor_accelerometer", "sensor_gyroscope", "sensor_magnetometer",
                "sensor_gravity", "sensor_linear_acceleration", "sensor_rotation_vector",
                "sensor_step_counter", "sensor_light", "sensor_proximity",
                "sensor_significant_motion", "sensor_tilt_detector", "sensor_screen_orientation",
            ),
            ids,
        )
    }

    @Test fun testHardwareSensorsRetiredToDecodeAlias() {
        // The umbrella is split into per-sensor modules; it survives only as a decode alias.
        assertFalse(CollectionModuleId.HARDWARE_SENSORS.active)
        assertEquals(CollectionModuleId.HARDWARE_SENSORS, CollectionModuleId.fromId("hardware_sensors"))
    }

    @Test fun testSamsungSensorsRetiredToDecodeAlias() {
        // The Samsung vendor sensors are non-portable + uninterpretable for research: retired to
        // decode-only aliases. Inactive, absent from the collectable set, but still decode and are
        // still classified as sensor modules (for legacy routing / per-sensor-config detection).
        listOf(CollectionModuleId.SENSOR_SAMSUNG_GRIP_WIFI, CollectionModuleId.SENSOR_SAMSUNG_MOTION)
            .forEach { retired ->
                assertFalse("$retired must be inactive", retired.active)
                assertEquals(retired, CollectionModuleId.fromId(retired.id))
                assertTrue("$retired still classified as a sensor module", SensorCollectionModules.isSensorModule(retired))
                assertFalse("$retired excluded from the collectable set", retired in SensorCollectionModules.sensorModuleIds)
                assertFalse("$retired excluded from the display order", retired in SensorCollectionModules.sensorDisplayOrder)
                assertTrue("$retired retained in the decode set", retired in SensorCollectionModules.allSensorModuleIds)
            }
    }

    @Test fun testEverySensorTypeHasAModuleAndActiveSetExcludesRetired() {
        AndroidSensorType.entries.forEach { type ->
            val moduleId = SensorCollectionModules.moduleFor(type)
            assertEquals(CollectionPrivacyClass.PHYSICAL_TELEMETRY, moduleId.privacyClass)
            assertEquals(type, SensorCollectionModules.sensorTypeOf(moduleId))
            assertTrue("every sensor type maps to a sensor module (incl. retired)",
                SensorCollectionModules.isSensorModule(moduleId))
        }
        // The decode set is total over the sensor catalog; the collectable set drops the 2 retired
        // Samsung vendor sensors.
        assertEquals(AndroidSensorType.entries.size, SensorCollectionModules.allSensorModuleIds.size)
        assertEquals(AndroidSensorType.entries.size - 2, SensorCollectionModules.sensorModuleIds.size)
        SensorCollectionModules.sensorModuleIds.forEach { assertTrue("$it must be active", it.active) }
    }

    @Test fun testHasAnySensorModuleDetectsPerSensorConfig() {
        // A config carrying any sensor_* entry has adopted the per-sensor model — it is then
        // the sole authority on sensors and the legacy bridge must be suppressed for them.
        val perSensor = AndroidDataCollectionSetting(
            modules = mapOf(
                CollectionModuleId.USAGE_EVENTS to CollectionDefaults.moduleSetting(CollectionModuleId.USAGE_EVENTS),
                CollectionModuleId.SENSOR_ACCELEROMETER to
                    CollectionDefaults.moduleSetting(CollectionModuleId.SENSOR_ACCELEROMETER, enabled = true),
            ),
        )
        assertTrue(perSensor.hasAnySensorModule())

        // Even a legacy config whose only sensor key is a RETIRED Samsung module still counts as a
        // per-sensor config (isSensorModule is total) → legacy bridge stays suppressed.
        val retiredOnly = AndroidDataCollectionSetting(
            modules = mapOf(
                CollectionModuleId.SENSOR_SAMSUNG_GRIP_WIFI to
                    CollectionDefaults.moduleSetting(CollectionModuleId.SENSOR_SAMSUNG_GRIP_WIFI),
            ),
        )
        assertTrue(retiredOnly.hasAnySensorModule())

        // Only non-sensor modules (or empty) → un-migrated; the legacy bridge still applies.
        val nonSensorOnly = AndroidDataCollectionSetting(
            modules = mapOf(
                CollectionModuleId.USAGE_EVENTS to CollectionDefaults.moduleSetting(CollectionModuleId.USAGE_EVENTS),
            ),
        )
        assertFalse(nonSensorOnly.hasAnySensorModule())
        assertFalse(AndroidDataCollectionSetting().hasAnySensorModule())
    }

    @Test fun testQuestionnaireAndBatteryAreActive() {
        assertTrue(CollectionModuleId.QUESTIONNAIRE.active)
        assertTrue(CollectionModuleId.BATTERY_TELEMETRY.active)
    }

    @Test fun testHealthConnectRecordTypesHaveStableWireIds() {
        assertEquals(
            setOf(
                "steps", "distance", "heart_rate", "total_calories_burned",
                "active_calories_burned", "floors_climbed", "resting_heart_rate",
                "oxygen_saturation", "respiratory_rate", "sleep", "exercise",
                "heart_rate_variability", "body_temperature", "skin_temperature",
            ),
            HealthConnectRecordType.entries.map { it.id }.toSet(),
        )
        assertEquals(
            HealthConnectRecordType.HEART_RATE,
            HealthConnectRecordType.fromId("heart_rate"),
        )
    }

    @Test fun testHealthConnectScopeDefaultsEmptyAndCanBeStudyConfigured() {
        assertTrue(CollectionModuleSetting(enabled = false).healthConnectRecordTypes.isEmpty())
        val configured = CollectionModuleSetting(
            enabled = true,
            healthConnectRecordTypes = setOf(
                HealthConnectRecordType.STEPS,
                HealthConnectRecordType.SLEEP,
            ),
        )
        assertEquals(
            setOf(HealthConnectRecordType.STEPS, HealthConnectRecordType.SLEEP),
            configured.healthConnectRecordTypes,
        )
    }

    @Test fun testInteractionEventsIsActive() {
        // Sensing-expansion Feature C: interaction_events is now a live, content-free module.
        assertTrue(CollectionModuleId.INTERACTION_EVENTS.active)
        assertEquals("interaction_events", CollectionModuleId.INTERACTION_EVENTS.id)
    }

    @Test fun testReservedModuleIdsAreInactive() {
        assertFalse(CollectionModuleId.TIME_USE_DIARY.active)
        assertFalse(CollectionModuleId.APP_INVENTORY.active)
        // Future eye-tracking + content-capture, namespace-frozen ahead of implementation.
        assertFalse(CollectionModuleId.GAZE_TRACKING.active)
        assertFalse(CollectionModuleId.INTERACTION_CONTENT.active)
        // Future opt-in location/mobility, skeleton only.
        assertFalse(CollectionModuleId.LOCATION.active)
        // Future opt-in communication-log metadata, skeleton only.
        assertFalse(CollectionModuleId.COMMUNICATION_LOG.active)
    }

    @Test fun testReservedModuleIdStrings() {
        assertEquals("time_use_diary", CollectionModuleId.TIME_USE_DIARY.id)
        assertEquals("app_inventory", CollectionModuleId.APP_INVENTORY.id)
        assertEquals("gaze_tracking", CollectionModuleId.GAZE_TRACKING.id)
        assertEquals("interaction_content", CollectionModuleId.INTERACTION_CONTENT.id)
        assertEquals("location", CollectionModuleId.LOCATION.id)
        assertEquals("communication_log", CollectionModuleId.COMMUNICATION_LOG.id)
    }

    @Test fun testSensingExpansionModuleIdPrivacyClasses() {
        assertEquals(CollectionPrivacyClass.DEVICE_STATE_METADATA, CollectionModuleId.BATTERY_TELEMETRY.privacyClass)
        assertEquals(CollectionPrivacyClass.BEHAVIORAL_METADATA, CollectionModuleId.AUDIO_ACTIVITY.privacyClass)
        assertEquals(CollectionPrivacyClass.MEDIA_CONTENT, CollectionModuleId.AUDIO_CONTENT.privacyClass)
        assertEquals(CollectionPrivacyClass.BEHAVIORAL_METADATA, CollectionModuleId.NOTIFICATION_ACTIVITY.privacyClass)
        assertEquals(CollectionPrivacyClass.INTERACTION_METADATA, CollectionModuleId.INTERACTION_EVENTS.privacyClass)
        assertEquals(CollectionPrivacyClass.BEHAVIORAL_METADATA, CollectionModuleId.IN_APP_ACTIVITY_CLASS.privacyClass)
        assertEquals(CollectionPrivacyClass.GAZE_TELEMETRY, CollectionModuleId.GAZE_TRACKING.privacyClass)
        assertEquals(CollectionPrivacyClass.SCREEN_CONTENT, CollectionModuleId.INTERACTION_CONTENT.privacyClass)
        assertEquals(CollectionPrivacyClass.PRECISE_LOCATION, CollectionModuleId.LOCATION.privacyClass)
        assertEquals(CollectionPrivacyClass.COMMUNICATION_METADATA, CollectionModuleId.COMMUNICATION_LOG.privacyClass)
        // Passive context & health sensing-expansion modules.
        assertEquals(CollectionPrivacyClass.HEALTH_METRICS, CollectionModuleId.SLEEP.privacyClass)
        assertEquals(CollectionPrivacyClass.BEHAVIORAL_METADATA, CollectionModuleId.ACTIVITY_RECOGNITION.privacyClass)
        assertEquals(CollectionPrivacyClass.HEALTH_METRICS, CollectionModuleId.HEALTH_CONNECT.privacyClass)
        assertEquals(CollectionPrivacyClass.DEVICE_STATE_METADATA, CollectionModuleId.CONNECTIVITY_STATE.privacyClass)
        assertEquals(CollectionPrivacyClass.BEHAVIORAL_METADATA, CollectionModuleId.APP_NETWORK_USAGE.privacyClass)
        assertEquals(CollectionPrivacyClass.DEVICE_STATE_METADATA, CollectionModuleId.DEVICE_SETTINGS.privacyClass)
    }

    @Test fun testModuleIdsAreUnique() {
        val ids = CollectionModuleId.entries.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test fun testFromIdOrNullKnown() {
        assertEquals(CollectionModuleId.USAGE_EVENTS, CollectionModuleId.fromIdOrNull("usage_events"))
    }

    @Test fun testFromIdOrNullUnknownReturnsNull() {
        assertNull(CollectionModuleId.fromIdOrNull("does_not_exist"))
    }

    @Test fun testFromIdKnown() {
        assertEquals(CollectionModuleId.HARDWARE_SENSORS, CollectionModuleId.fromId("hardware_sensors"))
    }

    @Test fun testFromIdUnknownThrows() {
        try {
            CollectionModuleId.fromId("does_not_exist")
            fail("Expected IllegalArgumentException for unknown module id")
        } catch (e: IllegalArgumentException) { /* expected */ }
    }

    @Test fun testModuleIdCarriesPrivacyClass() {
        assertEquals(CollectionPrivacyClass.PHYSICAL_TELEMETRY, CollectionModuleId.HARDWARE_SENSORS.privacyClass)
        assertEquals(CollectionPrivacyClass.BEHAVIORAL_METADATA, CollectionModuleId.USAGE_EVENTS.privacyClass)
    }

    // ===== CollectionPrivacyClass =====

    @Test fun testPrivacyClassCount() {
        assertEquals(14, CollectionPrivacyClass.entries.size)
    }

    @Test fun testGazeAndContentClassesNotEnabledByDefault() {
        assertFalse(CollectionPrivacyClass.GAZE_TELEMETRY.defaultEnabled)
        assertFalse(CollectionPrivacyClass.SCREEN_CONTENT.defaultEnabled)
        assertFalse(CollectionPrivacyClass.PRECISE_LOCATION.defaultEnabled)
        assertFalse(CollectionPrivacyClass.HEALTH_METRICS.defaultEnabled)
        assertFalse(CollectionPrivacyClass.AMBIENT_AUDIO_CONTEXT.defaultEnabled)
        assertFalse(CollectionPrivacyClass.COMMUNICATION_METADATA.defaultEnabled)
    }

    @Test fun testPhysicalTelemetryNotEnabledByDefault() {
        assertFalse(CollectionPrivacyClass.PHYSICAL_TELEMETRY.defaultEnabled)
    }

    @Test fun testLocalParticipantLabelNotEnabledByDefault() {
        assertFalse(CollectionPrivacyClass.LOCAL_PARTICIPANT_LABEL.defaultEnabled)
    }

    @Test fun testMediaContentNotEnabledByDefault() {
        assertFalse(CollectionPrivacyClass.MEDIA_CONTENT.defaultEnabled)
    }

    @Test fun testInteractionMetadataNotEnabledByDefault() {
        assertFalse(CollectionPrivacyClass.INTERACTION_METADATA.defaultEnabled)
    }

    @Test fun testNonSensitiveClassesEnabledByDefault() {
        assertTrue(CollectionPrivacyClass.BEHAVIORAL_METADATA.defaultEnabled)
        assertTrue(CollectionPrivacyClass.DEVICE_STATE_METADATA.defaultEnabled)
        assertTrue(CollectionPrivacyClass.OPERATIONAL_DIAGNOSTICS.defaultEnabled)
        assertTrue(CollectionPrivacyClass.DEVICE_CAPABILITY.defaultEnabled)
    }

    // ===== CollectionCadence =====

    @Test fun testCadenceDefaults() {
        val c = CollectionCadence()
        assertEquals(900L, c.intervalSeconds)
        assertEquals(0L, c.jitterSeconds)
    }

    @Test fun testCadenceNonPositiveIntervalRejected() {
        try {
            CollectionCadence(intervalSeconds = 0L)
            fail("Expected rejection of non-positive interval")
        } catch (e: IllegalArgumentException) { /* expected */ }
    }

    @Test fun testCadenceNegativeIntervalRejected() {
        try {
            CollectionCadence(intervalSeconds = -1L)
            fail("Expected rejection of negative interval")
        } catch (e: IllegalArgumentException) { /* expected */ }
    }

    @Test fun testCadenceNegativeJitterRejected() {
        try {
            CollectionCadence(intervalSeconds = 100L, jitterSeconds = -1L)
            fail("Expected rejection of negative jitter")
        } catch (e: IllegalArgumentException) { /* expected */ }
    }

    @Test fun testCadenceJitterExceedingIntervalRejected() {
        try {
            CollectionCadence(intervalSeconds = 100L, jitterSeconds = 101L)
            fail("Expected rejection of jitter exceeding interval")
        } catch (e: IllegalArgumentException) { /* expected */ }
    }

    @Test fun testCadenceEquality() {
        assertEquals(CollectionCadence(60L, 5L), CollectionCadence(60L, 5L))
    }

    // ===== BatteryPolicy =====

    @Test fun testBatteryPolicyDefaults() {
        val b = BatteryPolicy()
        assertEquals(15, b.minLevelPercent)
        assertEquals(5, b.stopBelowCriticalPercent)
        assertTrue(b.degradeInPowerSave)
    }

    @Test fun testBatteryPolicyOutOfRangeMinRejected() {
        try {
            BatteryPolicy(minLevelPercent = 101)
            fail("Expected rejection of out-of-range minLevelPercent")
        } catch (e: IllegalArgumentException) { /* expected */ }
    }

    @Test fun testBatteryPolicyNegativeCriticalRejected() {
        try {
            BatteryPolicy(stopBelowCriticalPercent = -1)
            fail("Expected rejection of negative critical percent")
        } catch (e: IllegalArgumentException) { /* expected */ }
    }

    @Test fun testBatteryPolicyCriticalAboveMinRejected() {
        try {
            BatteryPolicy(minLevelPercent = 10, stopBelowCriticalPercent = 20)
            fail("Expected rejection of critical above minimum")
        } catch (e: IllegalArgumentException) { /* expected */ }
    }

    // ===== NetworkPolicy =====

    @Test fun testNetworkPolicyDefaults() {
        val n = NetworkPolicy()
        assertFalse(n.requireUnmetered)
        assertTrue(n.requireConnected)
    }

    // ===== CollectionModuleSetting =====

    @Test fun testModuleSettingDefaults() {
        val s = CollectionModuleSetting(enabled = true)
        assertEquals(CollectionCadence.DEFAULT_COLLECTION, s.collectionCadence)
        assertEquals(CollectionCadence.DEFAULT_UPLOAD, s.uploadCadence)
        assertEquals(BatteryPolicy.DEFAULT, s.batteryPolicy)
        assertEquals(NetworkPolicy.DEFAULT, s.networkPolicy)
        assertNull(s.sensorPolicy)
    }

    @Test fun testModuleSettingEquality() {
        assertEquals(
            CollectionModuleSetting(enabled = true),
            CollectionModuleSetting(enabled = true),
        )
    }

    @Test fun testModuleSettingAcceptsValidSensorPolicy() {
        val s = CollectionModuleSetting(
            enabled = true,
            sensorPolicy = AndroidSensorSetting(
                sensors = setOf(AndroidSensorType.accelerometer),
                samplingRateHz = 10,
                dutyCycleActiveSeconds = 30,
                dutyCyclePeriodSeconds = 300,
            ),
        )
        assertNotNull(s.sensorPolicy)
    }

    @Test fun testModuleSettingUnsafeSamplingRatesRejected() {
        listOf(-1, 0, CollectionModuleSetting.MAX_SENSOR_SAMPLING_RATE_HZ + 1).forEach { rate ->
            try {
                CollectionModuleSetting(
                    enabled = true,
                    sensorPolicy = AndroidSensorSetting(samplingRateHz = rate),
                )
                fail("Expected rejection of unsafe sampling rate: $rate")
            } catch (e: IllegalArgumentException) { /* expected */ }
        }
    }

    @Test fun testModuleSettingDutyActiveExceedingPeriodRejected() {
        try {
            CollectionModuleSetting(
                enabled = true,
                sensorPolicy = AndroidSensorSetting(
                    dutyCycleActiveSeconds = 400,
                    dutyCyclePeriodSeconds = 300,
                ),
            )
            fail("Expected rejection of dutyActive > dutyPeriod")
        } catch (e: IllegalArgumentException) { /* expected */ }
    }

    @Test fun testModuleSettingNonPositiveDutyPeriodRejected() {
        try {
            CollectionModuleSetting(
                enabled = true,
                sensorPolicy = AndroidSensorSetting(
                    dutyCycleActiveSeconds = 0,
                    dutyCyclePeriodSeconds = 0,
                ),
            )
            fail("Expected rejection of non-positive duty period")
        } catch (e: IllegalArgumentException) { /* expected */ }
    }

    // ===== CollectionModuleDiagnostics =====

    @Test fun testDiagnosticsDefaults() {
        val d = CollectionModuleDiagnostics(
            moduleId = CollectionModuleId.UPLOAD_TELEMETRY,
            privacyClass = CollectionPrivacyClass.OPERATIONAL_DIAGNOSTICS,
        )
        assertNull(d.lastRunEpochMs)
        assertNull(d.lastResult)
        assertEquals(0, d.itemsCollected)
        assertEquals(0, d.queueDepth)
        assertNull(d.lastError)
        assertNull(d.redactedParticipantRef)
        assertTrue(d.notTracked.isEmpty())
    }

    @Test fun testDiagnosticsMismatchedPrivacyClassRejected() {
        try {
            CollectionModuleDiagnostics(
                moduleId = CollectionModuleId.HARDWARE_SENSORS,
                privacyClass = CollectionPrivacyClass.BEHAVIORAL_METADATA,
            )
            fail("Expected rejection of mismatched privacy class")
        } catch (e: IllegalArgumentException) { /* expected */ }
    }

    @Test fun testDiagnosticsToString() {
        val d = CollectionModuleDiagnostics(
            moduleId = CollectionModuleId.UPLOAD_TELEMETRY,
            privacyClass = CollectionPrivacyClass.OPERATIONAL_DIAGNOSTICS,
        )
        assertNotNull(d.toString())
    }

    // ===== CollectionDefaults =====

    @Test fun testDefaultsDisablePrivacySensitiveModules() {
        assertFalse(CollectionDefaults.defaultEnabled(CollectionModuleId.HARDWARE_SENSORS))
        assertFalse(CollectionDefaults.defaultEnabled(CollectionModuleId.USER_IDENTIFICATION))
    }

    @Test fun testInAppActivityClassDefaultsOffViaModuleOverride() {
        // in_app_activity_class is BEHAVIORAL_METADATA (class default-on), but the within-app activity
        // class is a finer, more-revealing signal we want opt-in: the per-module override forces it off.
        assertEquals(false, CollectionModuleId.IN_APP_ACTIVITY_CLASS.defaultEnabledOverride)
        assertFalse(CollectionDefaults.defaultEnabled(CollectionModuleId.IN_APP_ACTIVITY_CLASS))
        // The override does not leak to other BEHAVIORAL_METADATA modules.
        assertTrue(CollectionDefaults.defaultEnabled(CollectionModuleId.USAGE_EVENTS))
    }

    @Test fun testAudioAndNotificationModulesDefaultOff() {
        // audio_activity + notification_activity are BEHAVIORAL_METADATA (class default-on) but
        // override to opt-in; audio_content is MEDIA_CONTENT (class default-off). All three off by default.
        assertFalse(CollectionDefaults.defaultEnabled(CollectionModuleId.AUDIO_ACTIVITY))
        assertFalse(CollectionDefaults.defaultEnabled(CollectionModuleId.AUDIO_CONTENT))
        assertFalse(CollectionDefaults.defaultEnabled(CollectionModuleId.NOTIFICATION_ACTIVITY))
    }

    @Test fun testSensingExpansionContextModulesDefaultOff() {
        // The 6 passive-context & health modules are all opt-in: HEALTH_METRICS is class-default off,
        // and the BEHAVIORAL/DEVICE_STATE ones override their (class-on) default to off.
        assertFalse(CollectionDefaults.defaultEnabled(CollectionModuleId.SLEEP))
        assertFalse(CollectionDefaults.defaultEnabled(CollectionModuleId.ACTIVITY_RECOGNITION))
        assertFalse(CollectionDefaults.defaultEnabled(CollectionModuleId.HEALTH_CONNECT))
        assertFalse(CollectionDefaults.defaultEnabled(CollectionModuleId.CONNECTIVITY_STATE))
        assertFalse(CollectionDefaults.defaultEnabled(CollectionModuleId.APP_NETWORK_USAGE))
        assertFalse(CollectionDefaults.defaultEnabled(CollectionModuleId.DEVICE_SETTINGS))
    }

    @Test fun testDefaultsEnableNonSensitiveModules() {
        assertTrue(CollectionDefaults.defaultEnabled(CollectionModuleId.USAGE_EVENTS))
        assertTrue(CollectionDefaults.defaultEnabled(CollectionModuleId.DEVICE_LIFECYCLE))
        assertTrue(CollectionDefaults.defaultEnabled(CollectionModuleId.UPLOAD_TELEMETRY))
        assertTrue(CollectionDefaults.defaultEnabled(CollectionModuleId.SENSOR_AVAILABILITY))
    }

    @Test fun testDefaultAggregateCoversEveryActiveModule() {
        val setting = CollectionDefaults.androidDataCollectionSetting()
        assertEquals(CollectionModuleId.activeModules, setting.modules.keys)
    }

    @Test fun testDefaultAggregateNeverImplicitlyEnablesSensitiveModule() {
        val setting = CollectionDefaults.androidDataCollectionSetting()
        assertFalse(setting.modules.getValue(CollectionModuleId.SENSOR_ACCELEROMETER).enabled)
        assertFalse(setting.modules.getValue(CollectionModuleId.USER_IDENTIFICATION).enabled)
    }

    @Test fun testSensorModuleDefaultCarriesOwnSamplingPolicy() {
        val setting = CollectionDefaults.moduleSetting(CollectionModuleId.SENSOR_LIGHT)
        val policy = setting.sensorPolicy
        assertNotNull(policy)
        assertEquals(setOf(AndroidSensorType.light), policy!!.sensors)
        assertEquals(5, policy.samplingRateHz)
        assertEquals(30, policy.dutyCycleActiveSeconds)
        assertEquals(300, policy.dutyCyclePeriodSeconds)
    }

    @Test fun testModuleSettingFactoryRespectsExplicitEnabled() {
        val s = CollectionDefaults.moduleSetting(CollectionModuleId.HARDWARE_SENSORS, enabled = true)
        assertTrue(s.enabled)
    }

    // ===== AndroidDataCollectionSetting / legacy bridge =====

    @Test fun testAggregateDefaultVersion() {
        assertEquals(AndroidDataCollectionSetting.CURRENT_VERSION, AndroidDataCollectionSetting().version)
    }

    @Test fun testFromLegacyNullEnablesNoSensorModules() {
        val setting = AndroidDataCollectionSetting.fromLegacy(null)
        assertTrue(setting.modules.isEmpty())
    }

    @Test fun testFromLegacyEmptyEnablesNoSensorModules() {
        val setting = AndroidDataCollectionSetting.fromLegacy(AndroidSensorSetting.NO_SENSORS)
        assertTrue(setting.modules.isEmpty())
    }

    @Test fun testFromLegacyNonEmptyEnablesThePerSensorModule() {
        val legacy = AndroidSensorSetting(sensors = setOf(AndroidSensorType.accelerometer))
        val setting = AndroidDataCollectionSetting.fromLegacy(legacy)
        assertEquals(setOf(CollectionModuleId.SENSOR_ACCELEROMETER), setting.modules.keys)
        assertTrue(setting.modules.getValue(CollectionModuleId.SENSOR_ACCELEROMETER).enabled)
    }

    @Test fun testFromLegacyEnablesOnePerSensorModulePerSensor() {
        val legacy = AndroidSensorSetting(
            sensors = setOf(AndroidSensorType.accelerometer, AndroidSensorType.light),
        )
        val setting = AndroidDataCollectionSetting.fromLegacy(legacy)
        assertEquals(
            setOf(CollectionModuleId.SENSOR_ACCELEROMETER, CollectionModuleId.SENSOR_LIGHT),
            setting.modules.keys,
        )
    }

    @Test fun testFromLegacyCarriesSamplingRateOntoEachSensorModule() {
        val legacy = AndroidSensorSetting(sensors = setOf(AndroidSensorType.gyroscope), samplingRateHz = 25)
        val setting = AndroidDataCollectionSetting.fromLegacy(legacy)
        val policy = setting.modules.getValue(CollectionModuleId.SENSOR_GYROSCOPE).sensorPolicy
        assertNotNull(policy)
        assertEquals(25, policy!!.samplingRateHz)
        assertEquals(setOf(AndroidSensorType.gyroscope), policy.sensors)
    }

    @Test fun testFromLegacyDropsRetiredSensorsAndKeepsActiveOnes() {
        // A legacy blob listing a retired Samsung sensor alongside an active one bridges ONLY the
        // active sensor — retired modules are never re-enabled by the legacy bridge.
        val legacy = AndroidSensorSetting(
            sensors = setOf(AndroidSensorType.samsungGripWifi, AndroidSensorType.light),
        )
        val setting = AndroidDataCollectionSetting.fromLegacy(legacy)
        assertEquals(setOf(CollectionModuleId.SENSOR_LIGHT), setting.modules.keys)
    }

    // ===== InteractionPolicy (sensing expansion) =====

    @Test fun testInteractionPolicyDefaults() {
        val i = InteractionPolicy()
        assertEquals(4, i.gridRows)
        assertEquals(3, i.gridCols)
        assertTrue(i.captureClicks)
        assertTrue(i.captureScrolls)
        // Raw accessibility-element bounds are captured by default; the old flag is an alias.
        assertTrue(i.captureElementPosition)
        assertTrue(i.captureExactPosition)
    }

    @Test fun testInteractionPolicyExactPositionOptOut() {
        val coarseOnly = InteractionPolicy(captureExactPosition = false)
        assertFalse(coarseOnly.captureExactPosition)
        assertFalse(coarseOnly.captureElementPosition)
        // Opting out of element bounds leaves the legacy coarse grid intact.
        assertEquals(4, coarseOnly.gridRows)
        assertEquals(3, coarseOnly.gridCols)
    }

    @Test fun testInteractionPolicyZeroGridRowsRejected() {
        try {
            InteractionPolicy(gridRows = 0)
            fail("Expected rejection of zero grid rows")
        } catch (e: IllegalArgumentException) { /* expected */ }
    }

    @Test fun testInteractionPolicyZeroGridColsRejected() {
        try {
            InteractionPolicy(gridCols = 0)
            fail("Expected rejection of zero grid columns")
        } catch (e: IllegalArgumentException) { /* expected */ }
    }

    // ===== CollectionModuleSetting — sensing-expansion policy slots =====

    @Test fun testModuleSettingInteractionPolicyDefaultsNull() {
        val s = CollectionModuleSetting(enabled = true)
        assertNull(s.interactionPolicy)
    }

    @Test fun testModuleSettingCarriesInteractionPolicy() {
        val s = CollectionModuleSetting(enabled = true, interactionPolicy = InteractionPolicy.DEFAULT)
        assertNotNull(s.interactionPolicy)
    }

    // ===== CollectionDataDisposition (collection loop closure) =====

    @Test fun testDispositionWireIds() {
        assertEquals("flush_then_stop", CollectionDataDisposition.FLUSH_THEN_STOP.id)
        assertEquals("discard_and_stop", CollectionDataDisposition.DISCARD_AND_STOP.id)
        assertEquals("hold_pending", CollectionDataDisposition.HOLD_PENDING.id)
    }

    @Test fun testDispositionWireIdsAreUnique() {
        val ids = CollectionDataDisposition.entries.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test fun testDispositionFromIdKnown() {
        assertEquals(
            CollectionDataDisposition.HOLD_PENDING,
            CollectionDataDisposition.fromId("hold_pending"),
        )
    }

    @Test fun testDispositionFromIdOrNullUnknownReturnsNull() {
        assertNull(CollectionDataDisposition.fromIdOrNull("does_not_exist"))
    }

    @Test fun testDispositionFromIdUnknownThrows() {
        try {
            CollectionDataDisposition.fromId("does_not_exist")
            fail("Expected IllegalArgumentException for unknown disposition id")
        } catch (e: IllegalArgumentException) { /* expected */ }
    }

    @Test fun testModuleSettingDisableDispositionDefaultsNull() {
        // An enabled module never carries a disposition.
        assertNull(CollectionModuleSetting(enabled = true).disableDisposition)
    }

    @Test fun testModuleSettingCarriesDisableDisposition() {
        val s = CollectionModuleSetting(
            enabled = false,
            disableDisposition = CollectionDataDisposition.FLUSH_THEN_STOP,
        )
        assertEquals(CollectionDataDisposition.FLUSH_THEN_STOP, s.disableDisposition)
    }

    // ===== CollectionAcknowledgment (collection loop closure) =====

    @Test fun testAcknowledgmentRejectsEmptyModules() {
        try {
            CollectionAcknowledgment(
                acknowledgedModules = emptySet(),
                acknowledgedAt = OffsetDateTime.parse("2026-06-04T12:00:00Z"),
            )
            fail("Expected rejection of empty acknowledgedModules")
        } catch (e: IllegalArgumentException) { /* expected */ }
    }

    @Test fun testAcknowledgmentAppVersionDefaultsNull() {
        val ack = CollectionAcknowledgment(
            acknowledgedModules = setOf(CollectionModuleId.USAGE_EVENTS),
            acknowledgedAt = OffsetDateTime.parse("2026-06-04T12:00:00Z"),
        )
        assertNull(ack.appVersion)
        assertNull(ack.settingsVersion)
        assertTrue(ack.unavailableModules.isEmpty())
    }

    @Test fun testAcknowledgmentRejectsUnavailableNonSensorModule() {
        try {
            CollectionAcknowledgment(
                acknowledgedModules = emptySet(),
                acknowledgedAt = OffsetDateTime.parse("2026-06-04T12:00:00Z"),
                unavailableModules = setOf(CollectionModuleId.BATTERY_TELEMETRY),
            )
            fail("Expected rejection of unavailable non-sensor module")
        } catch (e: IllegalArgumentException) { /* expected */ }
    }

    @Test fun testAcknowledgmentRejectsUnavailableDecisionOverlap() {
        try {
            CollectionAcknowledgment(
                acknowledgedModules = setOf(CollectionModuleId.SENSOR_ACCELEROMETER),
                acknowledgedAt = OffsetDateTime.parse("2026-06-04T12:00:00Z"),
                unavailableModules = setOf(CollectionModuleId.SENSOR_ACCELEROMETER),
            )
            fail("Expected rejection of accepted/unavailable overlap")
        } catch (e: IllegalArgumentException) { /* expected */ }
    }

    @Test fun testDataCollectionSettingsVersionDefaultsToInitialRevision() {
        assertEquals(
            AndroidDataCollectionSetting.INITIAL_SETTINGS_VERSION,
            AndroidDataCollectionSetting().settingsVersion,
        )
    }

    @Test fun testEffectiveEnabledModulesApplySharedDefaults() {
        val setting = AndroidDataCollectionSetting(
            modules = mapOf(
                CollectionModuleId.USAGE_EVENTS to CollectionDefaults.moduleSetting(
                    CollectionModuleId.USAGE_EVENTS,
                    enabled = false,
                ),
                CollectionModuleId.SENSOR_ACCELEROMETER to CollectionDefaults.moduleSetting(
                    CollectionModuleId.SENSOR_ACCELEROMETER,
                    enabled = true,
                ),
                CollectionModuleId.AMBIENT_AUDIO to CollectionDefaults.moduleSetting(
                    CollectionModuleId.AMBIENT_AUDIO,
                    enabled = true,
                ),
            ),
        )

        val enabled = setting.effectiveEnabledModuleIds()
        assertFalse(enabled.contains(CollectionModuleId.USAGE_EVENTS))
        assertTrue(enabled.contains(CollectionModuleId.DEVICE_LIFECYCLE))
        assertTrue(enabled.contains(CollectionModuleId.SENSOR_ACCELEROMETER))
        assertFalse(enabled.contains(CollectionModuleId.SENSOR_GYROSCOPE))
        assertFalse(enabled.contains(CollectionModuleId.AMBIENT_AUDIO))
    }

    @Test fun testAcknowledgmentEntryStampsServerTimeByDefault() {
        // recordedAt defaults to "now" (server-stamped); acknowledgedAt is device-reported.
        val entry = CollectionAcknowledgmentEntry(
            studyId = java.util.UUID.randomUUID(),
            participantId = "p1",
            sourceDeviceId = "d1",
            acknowledgedModules = setOf(CollectionModuleId.USAGE_EVENTS),
            acknowledgedAt = OffsetDateTime.parse("2020-01-01T00:00:00Z"),
        )
        assertNotNull(entry.recordedAt)
        assertTrue(entry.recordedAt.isAfter(entry.acknowledgedAt))
    }
}
