package com.openlattice.chronicle.collection

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * Ingest-contract round-trip tests for the six sensing-expansion collection DTOs — the exact wire
 * shapes the Android client posts and the backend ingests (sleep, activity_recognition,
 * health_connect, connectivity_state, app_network_usage, device_settings).
 *
 * Each case serializes to JSON and SMILE, asserts the two decode identically (the cross-format
 * invariant [CollectionSerializationTest] uses), and asserts the decoded value equals the original.
 * Both branches of every nullable field are exercised so a value that survives JSON but not SMILE
 * (or vice-versa), or a field dropped by the data-class `copy`/equals contract, fails the build.
 */
class ExpansionDtoSerializationTest {

    private val json = TestMappers.json()
    private val smile = TestMappers.smile()

    private fun <T> roundTrip(value: T, clazz: Class<T>): T {
        val jsonText = json.writeValueAsString(value)
        val fromJson = json.readValue(jsonText, clazz)
        val fromSmile = smile.readValue(smile.writeValueAsBytes(value), clazz)
        assertEquals(fromJson, fromSmile)
        return fromJson
    }

    private val ts: OffsetDateTime = OffsetDateTime.of(2026, 6, 19, 12, 0, 0, 0, ZoneOffset.UTC)

    @Test fun sleepSegmentRoundTrip() {
        val e = AndroidSleepEvent(
            id = "s1", timestamp = ts, timezone = "America/Chicago",
            eventType = SleepEventType.SEGMENT,
            segmentStartMillis = 1_000L, segmentEndMillis = 2_000L,
            segmentStatus = SleepSegmentStatus.SUCCESSFUL,
            confidence = null, light = null, motion = null,
        )
        assertEquals(e, roundTrip(e, AndroidSleepEvent::class.java))
    }

    @Test fun sleepClassifyRoundTrip() {
        val e = AndroidSleepEvent(
            id = "s2", timestamp = ts, timezone = "UTC",
            eventType = SleepEventType.CLASSIFY,
            confidence = 80, light = 3, motion = 1,
        )
        assertEquals(e, roundTrip(e, AndroidSleepEvent::class.java))
    }

    @Test fun activityRecognitionRoundTrip() {
        val withTransition = AndroidActivityRecognitionEvent(
            id = "a1", timestamp = ts, timezone = "UTC",
            activityType = DetectedActivityType.WALKING, confidence = 100,
            transitionType = ActivityTransitionType.ENTER,
        )
        assertEquals(withTransition, roundTrip(withTransition, AndroidActivityRecognitionEvent::class.java))

        val noTransition = AndroidActivityRecognitionEvent(
            id = "a2", timestamp = ts, timezone = "UTC",
            activityType = DetectedActivityType.STILL, confidence = 75, transitionType = null,
        )
        assertEquals(noTransition, roundTrip(noTransition, AndroidActivityRecognitionEvent::class.java))
    }

    @Test fun healthMetricRoundTrip() {
        val e = AndroidHealthMetricEvent(
            id = "h1", timestamp = ts, timezone = "UTC",
            metricType = HealthMetricType.HEART_RATE, value = 62.5, unit = "bpm",
            startMillis = 1_000L, endMillis = 1_000L, sourcePackage = "com.google.android.apps.fitness",
        )
        assertEquals(e, roundTrip(e, AndroidHealthMetricEvent::class.java))

        val noSource = AndroidHealthMetricEvent(
            id = "h2", timestamp = ts, timezone = "UTC",
            metricType = HealthMetricType.STEPS, value = 1234.0, unit = "count",
            startMillis = 1_000L, endMillis = 2_000L, sourcePackage = null,
        )
        assertEquals(noSource, roundTrip(noSource, AndroidHealthMetricEvent::class.java))
    }

    @Test fun connectivityStateRoundTrip() {
        val full = AndroidConnectivityStateEvent(
            id = "c1", timestamp = ts, timezone = "UTC",
            eventType = ConnectivityEventType.SNAPSHOT, transport = NetworkTransport.WIFI,
            connected = true, metered = false, validated = true,
        )
        assertEquals(full, roundTrip(full, AndroidConnectivityStateEvent::class.java))

        val nulls = AndroidConnectivityStateEvent(
            id = "c2", timestamp = ts, timezone = "UTC",
            eventType = ConnectivityEventType.LOST, transport = NetworkTransport.NONE,
            connected = false, metered = null, validated = null,
        )
        assertEquals(nulls, roundTrip(nulls, AndroidConnectivityStateEvent::class.java))
    }

    @Test fun appNetworkUsageRoundTrip() {
        val e = AndroidAppNetworkUsageEvent(
            id = "n1", timestamp = ts, timezone = "UTC",
            packageName = "com.example.app", networkType = NetworkUsageType.CELLULAR,
            rxBytes = 4096L, txBytes = 2048L, bucketStartMillis = 1_000L, bucketEndMillis = 2_000L,
        )
        assertEquals(e, roundTrip(e, AndroidAppNetworkUsageEvent::class.java))
    }

    @Test fun deviceSettingsAllSetRoundTrip() {
        val e = AndroidDeviceSettingsEvent(
            id = "d1", timestamp = ts, timezone = "UTC",
            darkMode = true, fontScale = 1.15f, accessibilityEnabled = false, dndActive = true,
            batterySaver = false, thermalStatus = ThermalStatus.MODERATE, autoRotate = true,
            locationServicesEnabled = true, storageFreeBytes = 1_000_000L, storageTotalBytes = 64_000_000L,
            screenBrightness = 128, screenBrightnessAuto = false,
            mediaVolume = 7, mediaVolumeMax = 15, ringVolume = 5, ringVolumeMax = 7,
            notificationVolume = 4, notificationVolumeMax = 7, alarmVolume = 6, alarmVolumeMax = 7,
            ringerMode = RingerMode.VIBRATE,
        )
        assertEquals(e, roundTrip(e, AndroidDeviceSettingsEvent::class.java))
    }

    @Test fun deviceSettingsAllNullRoundTrip() {
        val e = AndroidDeviceSettingsEvent(id = "d2", timestamp = ts, timezone = "UTC")
        assertEquals(e, roundTrip(e, AndroidDeviceSettingsEvent::class.java))
    }
}
