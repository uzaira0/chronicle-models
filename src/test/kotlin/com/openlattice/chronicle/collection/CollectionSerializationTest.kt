package com.openlattice.chronicle.collection

import com.fasterxml.jackson.core.type.TypeReference
import com.openlattice.chronicle.android.AndroidSensorSetting
import com.openlattice.chronicle.android.AndroidSensorType
import com.openlattice.chronicle.study.StudySetting
import com.openlattice.chronicle.study.StudySettingType
import com.openlattice.chronicle.study.StudySettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.OffsetDateTime
import java.util.UUID

/**
 * Serialization round-trip and backward-compatibility tests for the Phase 2
 * collection contracts (refactor plan §5.1 steps 16-19 and §5.2).
 */
class CollectionSerializationTest {

    private val json = TestMappers.json()
    private val smile = TestMappers.smile()

    private fun <T> roundTrip(value: T, clazz: Class<T>): T {
        val jsonText = json.writeValueAsString(value)
        val fromJson = json.readValue(jsonText, clazz)
        val fromSmile = smile.readValue(smile.writeValueAsBytes(value), clazz)
        assertEquals(fromJson, fromSmile)
        return fromJson
    }

    // ===== DTO round-trip =====

    @Test fun testCadenceRoundTrip() {
        val c = CollectionCadence(intervalSeconds = 120L, jitterSeconds = 30L)
        assertEquals(c, roundTrip(c, CollectionCadence::class.java))
    }

    @Test fun testBatteryPolicyRoundTrip() {
        val b = BatteryPolicy(minLevelPercent = 25, stopBelowCriticalPercent = 8, degradeInPowerSave = false)
        assertEquals(b, roundTrip(b, BatteryPolicy::class.java))
    }

    @Test fun testNetworkPolicyRoundTrip() {
        val n = NetworkPolicy(requireUnmetered = true, requireConnected = true)
        assertEquals(n, roundTrip(n, NetworkPolicy::class.java))
    }

    @Test fun testInteractionPolicyRoundTrip() {
        val p = InteractionPolicy(
            gridRows = 6,
            gridCols = 4,
            captureClicks = true,
            captureScrolls = false,
            captureExactPosition = false,
        )
        assertEquals(p, roundTrip(p, InteractionPolicy::class.java))
    }

    @Test fun testLegacyInteractionPolicyUsesExactFlagAsElementBoundsAlias() {
        val restored = json.readValue(
            """{"gridRows":4,"gridCols":3,"captureExactPosition":false}""",
            InteractionPolicy::class.java,
        )
        assertFalse(restored.captureExactPosition)
        assertFalse(restored.captureElementPosition)
    }

    @Test fun testInteractionEventRoundTrip() {
        val click = AndroidInteractionEvent(
            id = "evt-1",
            timestamp = OffsetDateTime.parse("2026-06-18T12:00:00Z"),
            timezone = "UTC",
            eventType = InteractionEventType.CLICK,
            gridRows = 4,
            gridCols = 3,
            gridRow = 2,
            gridCol = 1,
            elementRole = "android.widget.Button",
            foregroundPackage = "com.example.app",
            positionSource = InteractionPositionSource.ACCESSIBILITY_NODE_BOUNDS,
            nodeBoundsLeft = 500,
            nodeBoundsTop = 900,
            nodeBoundsRight = 542,
            nodeBoundsBottom = 1168,
            displayId = 0,
            rawX = 521,
            rawY = 1034,
            screenWidth = 1080,
            screenHeight = 1920,
            screenDensityDpi = 420,
            normalizedX = 0.4827,
            normalizedY = 0.5391,
        )
        assertEquals(click, roundTrip(click, AndroidInteractionEvent::class.java))

        val scroll = click.copy(
            id = "evt-2",
            eventType = InteractionEventType.SCROLL,
            scrollDeltaX = -30,
            scrollDeltaY = 240,
            eventTimeMillis = 987_654L,
            episodeId = "ep-9",
            dwellMillisSincePrev = 350L,
            orientation = 1,
            screenDensityDpi = 420,
            scrollVelocityX = -85.5,
            scrollVelocityY = 684.0,
            scrollReversed = true,
        )
        assertEquals(scroll, roundTrip(scroll, AndroidInteractionEvent::class.java))

        // Grid-only mode: raw bounds, context, and legacy center fields are all omitted.
        val coarseOnly = click.copy(
            id = "evt-3",
            positionSource = null,
            nodeBoundsLeft = null,
            nodeBoundsTop = null,
            nodeBoundsRight = null,
            nodeBoundsBottom = null,
            displayId = null,
            rawX = null,
            rawY = null,
            screenWidth = null,
            screenHeight = null,
            screenDensityDpi = null,
            normalizedX = null,
            normalizedY = null,
        )
        assertEquals(coarseOnly, roundTrip(coarseOnly, AndroidInteractionEvent::class.java))
    }

    @Test fun testAudioActivityEventRoundTrip() {
        // Full Tier-2 media-session-change sample (attribution + device-audio state).
        val full = AndroidAudioActivityEvent(
            id = "aud-1",
            timestamp = OffsetDateTime.parse("2026-06-19T10:15:30Z"),
            timezone = "America/Chicago",
            eventType = AudioEventType.MEDIA_SESSION_CHANGE,
            audioActive = true,
            audioPackage = "com.spotify.music",
            contentType = AudioContentType.MUSIC,
            playbackState = AudioPlaybackState.PLAYING,
            outputRoute = AudioOutputRoute.BLUETOOTH,
            mediaVolume = 9,
            maxMediaVolume = 15,
            ringerMode = AudioRingerMode.NORMAL,
            dndActive = false,
            callActive = false,
        )
        assertEquals(full, roundTrip(full, AndroidAudioActivityEvent::class.java))

        // Headphone-disconnect route event, Tier-1-only (no listener grant → attribution null).
        val routeEvent = AndroidAudioActivityEvent(
            id = "aud-2",
            timestamp = OffsetDateTime.parse("2026-06-19T10:16:00Z"),
            timezone = "America/Chicago",
            eventType = AudioEventType.ROUTE_CHANGE,
            audioActive = false,
            outputRoute = AudioOutputRoute.WIRED_HEADPHONES,
            routeConnected = false,
            mediaVolume = 0,
            maxMediaVolume = 15,
            ringerMode = AudioRingerMode.VIBRATE,
        )
        assertEquals(routeEvent, roundTrip(routeEvent, AndroidAudioActivityEvent::class.java))
    }

    @Test fun testNotificationActivityEventRoundTrip() {
        val posted = AndroidNotificationActivityEvent(
            id = "notif-1",
            timestamp = OffsetDateTime.parse("2026-06-19T10:15:30Z"),
            timezone = "America/Chicago",
            eventType = NotificationEventType.POSTED,
            packageName = "com.whatsapp",
            category = "msg",
            ongoing = false,
            importance = 3,
        )
        assertEquals(posted, roundTrip(posted, AndroidNotificationActivityEvent::class.java))

        val removed = AndroidNotificationActivityEvent(
            id = "notif-2",
            timestamp = OffsetDateTime.parse("2026-06-19T10:17:00Z"),
            timezone = "America/Chicago",
            eventType = NotificationEventType.REMOVED,
            packageName = "com.google.android.gm",
        )
        assertEquals(removed, roundTrip(removed, AndroidNotificationActivityEvent::class.java))
    }

    @Test fun testAudioContentEventRoundTrip() {
        val content = AndroidAudioContentEvent(
            id = "audc-1",
            timestamp = OffsetDateTime.parse("2026-06-19T10:15:30Z"),
            timezone = "America/Chicago",
            audioPackage = "com.spotify.music",
            title = "Some Track",
            artist = "Some Artist",
            album = "Some Album",
            durationMillis = 213_000L,
            positionMillis = 42_000L,
        )
        assertEquals(content, roundTrip(content, AndroidAudioContentEvent::class.java))

        // Minimal: only the required package; all metadata absent.
        val minimal = AndroidAudioContentEvent(
            id = "audc-2",
            timestamp = OffsetDateTime.parse("2026-06-19T10:16:00Z"),
            timezone = "America/Chicago",
            audioPackage = "com.google.android.youtube",
        )
        assertEquals(minimal, roundTrip(minimal, AndroidAudioContentEvent::class.java))
    }

    @Test fun testSleepEventRoundTrip() {
        val segment = AndroidSleepEvent(
            id = "slp-1",
            timestamp = OffsetDateTime.parse("2026-06-19T07:00:00Z"),
            timezone = "America/Chicago",
            eventType = SleepEventType.SEGMENT,
            segmentStartMillis = 1_750_000_000_000L,
            segmentEndMillis = 1_750_028_800_000L,
            segmentStatus = SleepSegmentStatus.SUCCESSFUL,
        )
        assertEquals(segment, roundTrip(segment, AndroidSleepEvent::class.java))

        val classify = AndroidSleepEvent(
            id = "slp-2",
            timestamp = OffsetDateTime.parse("2026-06-19T03:10:00Z"),
            timezone = "America/Chicago",
            eventType = SleepEventType.CLASSIFY,
            confidence = 87,
            light = 2,
            motion = 0,
        )
        assertEquals(classify, roundTrip(classify, AndroidSleepEvent::class.java))
    }

    @Test fun testActivityRecognitionEventRoundTrip() {
        val periodic = AndroidActivityRecognitionEvent(
            id = "act-1",
            timestamp = OffsetDateTime.parse("2026-06-19T10:15:30Z"),
            timezone = "America/Chicago",
            activityType = DetectedActivityType.WALKING,
            confidence = 72,
        )
        assertEquals(periodic, roundTrip(periodic, AndroidActivityRecognitionEvent::class.java))

        val transition = AndroidActivityRecognitionEvent(
            id = "act-2",
            timestamp = OffsetDateTime.parse("2026-06-19T10:20:00Z"),
            timezone = "America/Chicago",
            activityType = DetectedActivityType.IN_VEHICLE,
            confidence = 95,
            transitionType = ActivityTransitionType.ENTER,
        )
        assertEquals(transition, roundTrip(transition, AndroidActivityRecognitionEvent::class.java))
    }

    @Test fun testHealthMetricEventRoundTrip() {
        val steps = AndroidHealthMetricEvent(
            id = "hc-1",
            timestamp = OffsetDateTime.parse("2026-06-19T10:15:30Z"),
            timezone = "America/Chicago",
            metricType = HealthMetricType.STEPS,
            value = 1234.0,
            unit = "count",
            startMillis = 1_750_000_000_000L,
            endMillis = 1_750_003_600_000L,
            sourcePackage = "com.google.android.apps.fitness",
        )
        assertEquals(steps, roundTrip(steps, AndroidHealthMetricEvent::class.java))

        val hr = AndroidHealthMetricEvent(
            id = "hc-2",
            timestamp = OffsetDateTime.parse("2026-06-19T10:16:00Z"),
            timezone = "America/Chicago",
            metricType = HealthMetricType.HEART_RATE,
            value = 64.0,
            unit = "bpm",
            startMillis = 1_750_003_600_000L,
            endMillis = 1_750_003_600_000L,
        )
        assertEquals(hr, roundTrip(hr, AndroidHealthMetricEvent::class.java))
    }

    @Test fun testConnectivityStateEventRoundTrip() {
        val wifi = AndroidConnectivityStateEvent(
            id = "con-1",
            timestamp = OffsetDateTime.parse("2026-06-19T10:15:30Z"),
            timezone = "America/Chicago",
            eventType = ConnectivityEventType.AVAILABLE,
            transport = NetworkTransport.WIFI,
            connected = true,
            metered = false,
            validated = true,
        )
        assertEquals(wifi, roundTrip(wifi, AndroidConnectivityStateEvent::class.java))

        val lost = AndroidConnectivityStateEvent(
            id = "con-2",
            timestamp = OffsetDateTime.parse("2026-06-19T10:18:00Z"),
            timezone = "America/Chicago",
            eventType = ConnectivityEventType.LOST,
            transport = NetworkTransport.NONE,
            connected = false,
        )
        assertEquals(lost, roundTrip(lost, AndroidConnectivityStateEvent::class.java))
    }

    @Test fun testAppNetworkUsageEventRoundTrip() {
        val usage = AndroidAppNetworkUsageEvent(
            id = "net-1",
            timestamp = OffsetDateTime.parse("2026-06-19T10:15:30Z"),
            timezone = "America/Chicago",
            packageName = "com.google.android.youtube",
            networkType = NetworkUsageType.CELLULAR,
            rxBytes = 41_943_040L,
            txBytes = 524_288L,
            bucketStartMillis = 1_750_000_000_000L,
            bucketEndMillis = 1_750_003_600_000L,
        )
        assertEquals(usage, roundTrip(usage, AndroidAppNetworkUsageEvent::class.java))
    }

    @Test fun testDeviceSettingsEventRoundTrip() {
        val full = AndroidDeviceSettingsEvent(
            id = "set-1",
            timestamp = OffsetDateTime.parse("2026-06-19T10:15:30Z"),
            timezone = "America/Chicago",
            darkMode = true,
            fontScale = 1.15f,
            accessibilityEnabled = false,
            dndActive = true,
            batterySaver = false,
            thermalStatus = ThermalStatus.NONE,
            autoRotate = true,
            locationServicesEnabled = true,
            storageFreeBytes = 12_884_901_888L,
            storageTotalBytes = 128_849_018_880L,
            screenBrightness = 200,
            screenBrightnessAuto = true,
            mediaVolume = 11,
            mediaVolumeMax = 15,
            ringVolume = 3,
            ringVolumeMax = 7,
            notificationVolume = 2,
            notificationVolumeMax = 7,
            alarmVolume = 5,
            alarmVolumeMax = 7,
            ringerMode = RingerMode.NORMAL,
        )
        assertEquals(full, roundTrip(full, AndroidDeviceSettingsEvent::class.java))

        // Partial snapshot: only the required identity fields populated.
        val minimal = AndroidDeviceSettingsEvent(
            id = "set-2",
            timestamp = OffsetDateTime.parse("2026-06-19T10:16:00Z"),
            timezone = "America/Chicago",
        )
        assertEquals(minimal, roundTrip(minimal, AndroidDeviceSettingsEvent::class.java))
    }

    @Test fun testModuleSettingRoundTrip() {
        val s = CollectionModuleSetting(
            enabled = true,
            collectionCadence = CollectionCadence(60L, 5L),
            sensorPolicy = AndroidSensorSetting(sensors = setOf(AndroidSensorType.accelerometer)),
        )
        assertEquals(s, roundTrip(s, CollectionModuleSetting::class.java))
    }

    @Test fun testRemovedCapturePolicyIsIgnoredAndNeverPublished() {
        val legacyPayload = """{
            "enabled":true,
            "audioCapturePolicy":{
                "captureWindowSeconds":30,
                "captureIntervalSeconds":600,
                "maxDailyCaptureMinutes":60,
                "gateOnForegroundMedia":true,
                "excludedAppPackages":["example.private.app"]
            }
        }""".trimIndent()

        val decoded = json.readValue(legacyPayload, CollectionModuleSetting::class.java)
        assertTrue(decoded.enabled)

        val constructed = CollectionModuleSetting(enabled = true)
        val serialized = json.writeValueAsString(constructed)
        assertFalse(serialized.contains("audioCapturePolicy"))
        assertFalse(serialized.contains("captureWindowSeconds"))
        assertTrue(json.readValue(serialized, CollectionModuleSetting::class.java).enabled)
    }

    @Test fun testDisabledModuleSettingCarriesDispositionThroughRoundTrip() {
        val s = CollectionModuleSetting(
            enabled = false,
            disableDisposition = CollectionDataDisposition.DISCARD_AND_STOP,
        )
        val restored = roundTrip(s, CollectionModuleSetting::class.java)
        assertEquals(s, restored)
        assertEquals(CollectionDataDisposition.DISCARD_AND_STOP, restored.disableDisposition)
    }

    @Test fun testDispositionSerializesAsStableWireString() {
        val text = json.writeValueAsString(CollectionDataDisposition.HOLD_PENDING)
        assertEquals("\"hold_pending\"", text)
    }

    @Test fun testUnknownDispositionRejectedDuringDeserialization() {
        val payload = """{"enabled":false,"disableDisposition":"not_a_real_disposition"}"""
        try {
            json.readValue(payload, CollectionModuleSetting::class.java)
            org.junit.Assert.fail("Expected failure decoding an unknown disposition")
        } catch (expected: Exception) { /* expected */ }
    }

    @Test fun testCollectionAcknowledgmentRoundTrip() {
        val ack = CollectionAcknowledgment(
            acknowledgedModules = setOf(
                CollectionModuleId.SENSOR_ACCELEROMETER,
                CollectionModuleId.BATTERY_TELEMETRY,
            ),
            acknowledgedAt = OffsetDateTime.parse("2026-06-04T12:00:00Z"),
            unavailableModules = setOf(CollectionModuleId.SENSOR_GYROSCOPE),
            appVersion = "1.2.3",
            settingsVersion = 7,
            disclosureVersion = "consent-2026-08-17",
            manifestDigest = "a".repeat(64),
        )
        assertEquals(ack, roundTrip(ack, CollectionAcknowledgment::class.java))
    }

    @Test fun testCollectionAcknowledgmentEntryRoundTrip() {
        val entry = CollectionAcknowledgmentEntry(
            id = UUID.fromString("00000000-0000-0000-0000-000000000001"),
            studyId = UUID.fromString("00000000-0000-0000-0000-000000000002"),
            participantId = "participant-1",
            sourceDeviceId = "device-1",
            acknowledgedModules = setOf(CollectionModuleId.USAGE_EVENTS),
            acknowledgedAt = OffsetDateTime.parse("2026-06-04T12:00:00Z"),
            unavailableModules = setOf(CollectionModuleId.SENSOR_GYROSCOPE),
            recordedAt = OffsetDateTime.parse("2026-06-04T12:00:05Z"),
            appVersion = null,
            settingsVersion = 7,
            disclosureVersion = "consent-2026-08-17",
            manifestDigest = "a".repeat(64),
        )
        assertEquals(entry, roundTrip(entry, CollectionAcknowledgmentEntry::class.java))
    }

    @Test fun testAcknowledgmentModuleIdsSerializeAsWireStrings() {
        val ack = CollectionAcknowledgment(
            acknowledgedModules = setOf(CollectionModuleId.SENSOR_ACCELEROMETER),
            acknowledgedAt = OffsetDateTime.parse("2026-06-04T12:00:00Z"),
        )
        assertTrue(json.writeValueAsString(ack).contains("sensor_accelerometer"))
    }

    @Test fun testModuleSettingRequiredRoundTrips() {
        val s = CollectionModuleSetting(enabled = true, required = true)
        val restored = roundTrip(s, CollectionModuleSetting::class.java)
        assertEquals(s, restored)
        assertTrue(restored.required)
    }

    @Test fun testModuleSettingRequiredDefaultsFalseForLegacyPayload() {
        val restored = json.readValue("""{"enabled":true}""", CollectionModuleSetting::class.java)
        assertFalse("a payload without `required` is optional", restored.required)
    }

    @Test fun testAcknowledgmentDecisionSnapshotRoundTrips() {
        val ack = CollectionAcknowledgment(
            acknowledgedModules = setOf(CollectionModuleId.USAGE_EVENTS),
            acknowledgedAt = OffsetDateTime.parse("2026-06-10T12:00:00Z"),
            declinedModules = setOf(CollectionModuleId.BATTERY_TELEMETRY),
            trigger = ConsentTrigger.PARTICIPANT_TOGGLE,
        )
        assertEquals(ack, roundTrip(ack, CollectionAcknowledgment::class.java))
    }

    @Test fun testAcknowledgmentLegacyPayloadDefaultsDeclinedEmptyAndTriggerEnrollment() {
        val legacy = """{"acknowledgedModules":["usage_events"],"acknowledgedAt":"2026-06-10T12:00:00Z"}"""
        val restored = json.readValue(legacy, CollectionAcknowledgment::class.java)
        assertTrue(restored.declinedModules.isEmpty())
        assertTrue(restored.unavailableModules.isEmpty())
        assertEquals(ConsentTrigger.ENROLLMENT, restored.trigger)
    }

    @Test fun testAcknowledgmentEntryDecisionRoundTrips() {
        val entry = CollectionAcknowledgmentEntry(
            id = UUID.fromString("00000000-0000-0000-0000-000000000001"),
            studyId = UUID.fromString("00000000-0000-0000-0000-000000000002"),
            participantId = "p-1",
            sourceDeviceId = "d-1",
            acknowledgedModules = setOf(CollectionModuleId.USAGE_EVENTS),
            acknowledgedAt = OffsetDateTime.parse("2026-06-10T12:00:00Z"),
            declinedModules = setOf(CollectionModuleId.SENSOR_LIGHT),
            trigger = ConsentTrigger.SETTINGS_CHANGE,
            recordedAt = OffsetDateTime.parse("2026-06-10T12:00:05Z"),
        )
        assertEquals(entry, roundTrip(entry, CollectionAcknowledgmentEntry::class.java))
    }

    @Test fun testPureDeclineSnapshotIsValid() {
        val ack = CollectionAcknowledgment(
            acknowledgedModules = emptySet(),
            acknowledgedAt = OffsetDateTime.parse("2026-06-10T12:00:00Z"),
            declinedModules = setOf(CollectionModuleId.BATTERY_TELEMETRY),
            trigger = ConsentTrigger.PARTICIPANT_TOGGLE,
        )
        assertEquals(ack, roundTrip(ack, CollectionAcknowledgment::class.java))
    }

    @Test fun testDiagnosticsRoundTrip() {
        val d = CollectionModuleDiagnostics(
            moduleId = CollectionModuleId.UPLOAD_TELEMETRY,
            privacyClass = CollectionPrivacyClass.OPERATIONAL_DIAGNOSTICS,
            lastRunEpochMs = 1_700_000_000_000L,
            lastResult = "OK",
            itemsCollected = 12,
            queueDepth = 3,
            redactedParticipantRef = "a1b2c3",
            notTracked = setOf("replayCount"),
        )
        assertEquals(d, roundTrip(d, CollectionModuleDiagnostics::class.java))
    }

    @Test fun testAggregateRoundTrip() {
        val setting = CollectionDefaults.androidDataCollectionSetting()
        assertEquals(setting, roundTrip(setting, AndroidDataCollectionSetting::class.java))
    }

    @Test fun testModuleIdSerializesAsStableString() {
        val text = json.writeValueAsString(CollectionModuleId.HARDWARE_SENSORS)
        assertEquals("\"hardware_sensors\"", text)
    }

    // ===== Polymorphic StudySetting (de)serialization =====

    @Test fun testAggregateRoundTripsAsPolymorphicStudySetting() {
        val original: StudySetting = CollectionDefaults.androidDataCollectionSetting()
        val text = json.writeValueAsString(original)
        assertTrue(
            "polymorphic payload must carry the @class discriminator",
            text.contains(AndroidDataCollectionSetting::class.java.name),
        )
        val restored = json.readValue(text, StudySetting::class.java)
        assertTrue(restored is AndroidDataCollectionSetting)
        assertEquals(original, restored)
    }

    @Test fun testStudySettingsMapWithDataCollectionRoundTrips() {
        val settings = StudySettings(
            mapOf(StudySettingType.DataCollection to CollectionDefaults.androidDataCollectionSetting()),
        )
        val text = json.writeValueAsString(settings)
        val restored = json.readValue(text, object : TypeReference<StudySettings>() {})
        assertEquals(settings, restored)
    }

    // ===== Legacy AndroidSensorSetting compatibility (refactor plan §5.2 step 1) =====

    @Test fun testLegacyAndroidSensorSettingJsonStillRoundTripsUnchanged() {
        val legacy = AndroidSensorSetting(
            sensors = setOf(AndroidSensorType.accelerometer, AndroidSensorType.gyroscope),
            samplingRateHz = 10,
            dutyCycleActiveSeconds = 20,
            dutyCyclePeriodSeconds = 200,
        )
        val text = json.writeValueAsString(legacy)
        assertEquals(legacy, json.readValue(text, AndroidSensorSetting::class.java))
    }

    @Test fun testLegacyAndroidSensorSettingRoundTripsAsPolymorphicStudySetting() {
        val legacy: StudySetting = AndroidSensorSetting(sensors = setOf(AndroidSensorType.light))
        val text = json.writeValueAsString(legacy)
        assertTrue(text.contains(AndroidSensorSetting::class.java.name))
        assertEquals(legacy, json.readValue(text, StudySetting::class.java))
    }

    @Test fun testStudySettingsWithLegacyAndroidSensorRoundTrips() {
        val settings = StudySettings(
            mapOf(StudySettingType.AndroidSensor to AndroidSensorSetting.NO_SENSORS),
        )
        val text = json.writeValueAsString(settings)
        val restored = json.readValue(text, object : TypeReference<StudySettings>() {})
        assertEquals(settings, restored)
    }

    // ===== Legacy bridge fallback (refactor plan §5.2 steps 3-5) =====

    @Test fun testEmptyLegacySensorSettingBridgesToNoSensorModules() {
        val bridged = AndroidDataCollectionSetting.fromLegacy(AndroidSensorSetting.NO_SENSORS)
        assertTrue(bridged.modules.isEmpty())
    }

    @Test fun testNonEmptyLegacySensorSettingBridgesToThePerSensorModuleOnly() {
        val legacy = AndroidSensorSetting(sensors = setOf(AndroidSensorType.proximity))
        val bridged = AndroidDataCollectionSetting.fromLegacy(legacy)
        assertEquals(setOf(CollectionModuleId.SENSOR_PROXIMITY), bridged.modules.keys)
        assertTrue(bridged.modules.getValue(CollectionModuleId.SENSOR_PROXIMITY).enabled)
    }

    @Test fun testMissingDataCollectionFallsBackToLegacyAndroidSensor() {
        // Simulates resolver fallback order: no DataCollection setting present,
        // a legacy AndroidSensor setting is, so the bridge derives the aggregate.
        val studySettings = StudySettings(
            mapOf(
                StudySettingType.AndroidSensor to
                    AndroidSensorSetting(sensors = setOf(AndroidSensorType.accelerometer)),
            ),
        )
        val dataCollection = studySettings[StudySettingType.DataCollection] as? AndroidDataCollectionSetting
        val legacy = studySettings[StudySettingType.AndroidSensor] as? AndroidSensorSetting
        val resolved = dataCollection ?: AndroidDataCollectionSetting.fromLegacy(legacy)
        assertTrue(resolved.modules.getValue(CollectionModuleId.SENSOR_ACCELEROMETER).enabled)
    }

    // ===== Tolerant deserialization (refactor plan §5.2 steps 11-12) =====

    @Test fun testUnknownModuleIdDoesNotCrashDeserialization() {
        val payload = """
            {
              "@class":"${AndroidDataCollectionSetting::class.java.name}",
              "version":1,
              "modules":{
                "usage_events":{"enabled":true},
                "totally_unknown_module":{"enabled":true}
              }
            }
        """.trimIndent()
        val restored = json.readValue(payload, AndroidDataCollectionSetting::class.java)
        assertEquals(setOf(CollectionModuleId.USAGE_EVENTS), restored.modules.keys)
    }

    @Test fun testUnknownTopLevelFieldIgnored() {
        val payload = """
            {
              "@class":"${AndroidDataCollectionSetting::class.java.name}",
              "version":1,
              "modules":{},
              "futureUnknownField":"ignored"
            }
        """.trimIndent()
        val restored = json.readValue(payload, AndroidDataCollectionSetting::class.java)
        assertTrue(restored.modules.isEmpty())
    }

    @Test fun testDuplicateModuleEntriesNormalizeDeterministically() {
        // A JSON object with a repeated key — last value wins, deterministically.
        val payload = """
            {
              "@class":"${AndroidDataCollectionSetting::class.java.name}",
              "version":1,
              "modules":{
                "usage_events":{"enabled":true},
                "usage_events":{"enabled":false}
              }
            }
        """.trimIndent()
        val restored = json.readValue(payload, AndroidDataCollectionSetting::class.java)
        assertEquals(1, restored.modules.size)
        assertFalse(restored.modules.getValue(CollectionModuleId.USAGE_EVENTS).enabled)
    }

    @Test fun testMissingModulesDefaultsToEmpty() {
        val payload = """
            {"@class":"${AndroidDataCollectionSetting::class.java.name}","version":2}
        """.trimIndent()
        val restored = json.readValue(payload, AndroidDataCollectionSetting::class.java)
        assertTrue(restored.modules.isEmpty())
        assertEquals(2, restored.version)
    }

    // ===== Validation surfaces through deserialization =====

    @Test(expected = Exception::class)
    fun testNegativeCadenceRejectedDuringDeserialization() {
        val payload = """{"intervalSeconds":-1,"jitterSeconds":0}"""
        json.readValue(payload, CollectionCadence::class.java)
    }

    @Test(expected = Exception::class)
    fun testOutOfRangeBatteryPolicyRejectedDuringDeserialization() {
        val payload = """{"minLevelPercent":200,"stopBelowCriticalPercent":5,"degradeInPowerSave":true}"""
        json.readValue(payload, BatteryPolicy::class.java)
    }

    @Test(expected = Exception::class)
    fun testNegativeSamplingRateRejectedDuringDeserialization() {
        val payload = """
            {"enabled":true,"sensorPolicy":{"sensors":[],"samplingRateHz":-10,
            "dutyCycleActiveSeconds":30,"dutyCyclePeriodSeconds":300}}
        """.trimIndent()
        json.readValue(payload, CollectionModuleSetting::class.java)
    }

    @Test(expected = Exception::class)
    fun testDutyActiveExceedingPeriodRejectedDuringDeserialization() {
        val payload = """
            {"enabled":true,"sensorPolicy":{"sensors":[],"samplingRateHz":5,
            "dutyCycleActiveSeconds":500,"dutyCyclePeriodSeconds":300}}
        """.trimIndent()
        json.readValue(payload, CollectionModuleSetting::class.java)
    }

    @Test(expected = Exception::class)
    fun testMissingPrivacyClassRejectedDuringDeserialization() {
        // privacyClass is a non-null property with no default — omitting it must fail.
        val payload = """{"moduleId":"upload_telemetry","itemsCollected":0}"""
        json.readValue(payload, CollectionModuleDiagnostics::class.java)
    }

    // ===== Secret-leakage guards (refactor plan §5.2 steps 17-18) =====

    @Test fun testDiagnosticsNeverSerializeSecretFields() {
        val d = CollectionModuleDiagnostics(
            moduleId = CollectionModuleId.UPLOAD_TELEMETRY,
            privacyClass = CollectionPrivacyClass.OPERATIONAL_DIAGNOSTICS,
            redactedParticipantRef = "hash-abcdef",
        )
        val text = json.writeValueAsString(d).lowercase()
        assertFalse(text.contains("apikey"))
        assertFalse(text.contains("mobile_signing_secret"))
        assertFalse(text.contains("signingsecret"))
        // Only a redacted reference field exists; there is no raw participantId field.
        assertFalse(text.contains("\"participantid\""))
    }

    @Test fun testSettingDtosNeverSerializeSecretFields() {
        val setting = CollectionDefaults.androidDataCollectionSetting()
        val text = json.writeValueAsString(setting).lowercase()
        assertFalse(text.contains("apikey"))
        assertFalse(text.contains("signingsecret"))
        assertFalse(text.contains("participantid"))
        assertFalse(text.contains("password"))
    }
}
