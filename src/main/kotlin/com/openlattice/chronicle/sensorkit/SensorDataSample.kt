package com.openlattice.chronicle.sensorkit

import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.OffsetDateTime
import java.util.*

/**
 * @author alfoncenzioka &lt;alfonce@openlattice.com&gt;
 *
 * An instance of this class encapsulates a sample recorded by SensorKit framework
 */

public data class SensorDataSample(
        @field:NotNull(message = "Sample ID is required")
        val id: UUID,

        @field:NotNull(message = "Date recorded is required")
        val dateRecorded: OffsetDateTime,

        @field:Min(value = 0, message = "Duration cannot be negative")
        val duration: Double,

        @field:NotBlank(message = "Data is required")
        @field:Size(max = 1000000, message = "Data exceeds maximum length")
        val data: String,

        @field:NotBlank(message = "Device is required")
        @field:Size(max = 255, message = "Device exceeds maximum length")
        val device: String,

        @field:NotBlank(message = "Timezone is required")
        @field:Size(max = 100, message = "Timezone exceeds maximum length")
        val timezone: String,

        @field:NotNull(message = "Sensor type is required")
        val sensor: SensorType,

        @field:NotNull(message = "Start date is required")
        val startDate: OffsetDateTime,

        @field:NotNull(message = "End date is required")
        val endDate: OffsetDateTime
)

public data class SensorSourceDevice(
        @field:NotBlank(message = "Model is required")
        @field:Size(max = 255, message = "Model exceeds maximum length")
        val model: String,

        @field:NotBlank(message = "Name is required")
        @field:Size(max = 255, message = "Name exceeds maximum length")
        val name: String,

        @field:NotBlank(message = "System name is required")
        @field:Size(max = 100, message = "System name exceeds maximum length")
        val systemName: String,

        @field:NotBlank(message = "System version is required")
        @field:Size(max = 50, message = "System version exceeds maximum length")
        val systemVersion: String
)

public data class AccelerometerBatchData(
        @field:Min(value = 1, message = "Schema version must be positive")
        val schemaVersion: Int,

        @field:Min(value = 1, message = "Frequency must be positive")
        val frequencyHz: Double,

        @field:NotBlank(message = "Provenance is required")
        @field:Size(max = 100, message = "Provenance exceeds maximum length")
        val provenance: String,

        @field:Size(min = 1, max = 1000, message = "Accelerometer batch must contain 1 to 1000 samples")
        val samples: List<AccelerometerSample>
)

public data class AccelerometerSample(
        @field:Min(value = 0, message = "Sample offset cannot be negative")
        val offsetSeconds: Double,
        val xG: Double,
        val yG: Double,
        val zG: Double
)

public data class PedometerBatchData(
        @field:Min(value = 1, message = "Schema version must be positive")
        val schemaVersion: Int,

        @field:NotBlank(message = "Provenance is required")
        @field:Size(max = 100, message = "Provenance exceeds maximum length")
        val provenance: String,

        @field:Min(value = 0, message = "Step count cannot be negative")
        val numberOfSteps: Int,

        @field:DecimalMin(value = "0.0", message = "Distance cannot be negative")
        val distanceMeters: Double? = null,

        @field:Min(value = 0, message = "Floors ascended cannot be negative")
        val floorsAscended: Int? = null,

        @field:Min(value = 0, message = "Floors descended cannot be negative")
        val floorsDescended: Int? = null,

        @field:DecimalMin(value = "0.0", message = "Average active pace cannot be negative")
        val averageActivePaceSecondsPerMeter: Double? = null,

        @field:DecimalMin(value = "0.0", message = "Current pace cannot be negative")
        val currentPaceSecondsPerMeter: Double? = null,

        @field:DecimalMin(value = "0.0", message = "Cadence cannot be negative")
        val currentCadenceStepsPerSecond: Double? = null
)

public data class MotionActivityEventData(
        @field:Min(value = 1, message = "Schema version must be positive")
        val schemaVersion: Int,

        @field:NotBlank(message = "Provenance is required")
        @field:Size(max = 100, message = "Provenance exceeds maximum length")
        val provenance: String,

        val stationary: Boolean,
        val walking: Boolean,
        val running: Boolean,
        val automotive: Boolean,
        val cycling: Boolean,
        val unknown: Boolean,

        @field:NotBlank(message = "Confidence is required")
        @field:Size(max = 20, message = "Confidence exceeds maximum length")
        val confidence: String
)

public data class CompactNumericSensorPayload(
        @field:Min(value = 1, message = "Schema version must be positive")
        val schemaVersion: Int,

        @field:NotBlank(message = "Encoding is required")
        @field:Size(max = 100, message = "Encoding exceeds maximum length")
        val encoding: String,

        @field:Min(value = 1, message = "Sample count must be positive")
        @field:Max(value = 1000000, message = "Sample count exceeds maximum")
        val sampleCount: Int,

        @field:DecimalMin(value = "0.0", inclusive = false, message = "Frequency must be positive")
        val nominalFrequencyHz: Double? = null,

        @field:NotBlank(message = "Provenance is required")
        @field:Size(max = 100, message = "Provenance exceeds maximum length")
        val provenance: String,

        @field:NotBlank(message = "Time unit is required")
        @field:Size(max = 50, message = "Time unit exceeds maximum length")
        val timeUnit: String,

        @field:Min(value = 1, message = "Uncompressed byte count must be positive")
        @field:Max(value = 8388608, message = "Uncompressed payload exceeds maximum length")
        val uncompressedByteCount: Int,

        @field:Size(min = 1, max = 128, message = "Payload must define 1 to 128 numeric channels")
        val channels: List<CompactNumericChannelDefinition>,

        @field:NotBlank(message = "Payload is required")
        @field:Size(max = 1000000, message = "Payload exceeds maximum length")
        val payload: String
)

public data class CompactNumericChannelDefinition(
        @field:NotBlank(message = "Channel name is required")
        @field:Size(max = 100, message = "Channel name exceeds maximum length")
        val name: String,

        @field:NotBlank(message = "Channel unit is required")
        @field:Size(max = 100, message = "Channel unit exceeds maximum length")
        val unit: String,

        @field:DecimalMin(value = "0.0", inclusive = false, message = "Channel scale must be positive")
        val scale: Double? = null
)

public data class AppUsage(
        @field:Min(value = 0, message = "Usage time cannot be negative")
        val usageTime: Double,

        val textInputSessions: Map<String, Double>,

        @field:NotBlank(message = "Bundle identifier is required")
        @field:Size(max = 500, message = "Bundle identifier exceeds maximum length")
        val bundleIdentifier: String
)

public data class DeviceUsageData(
        @field:Min(value = 0, message = "Total screen wakes cannot be negative")
        val totalScreenWakes: Int,

        @field:Min(value = 0, message = "Total unlocks cannot be negative")
        val totalUnlocks: Int,

        @field:Min(value = 0, message = "Total unlock duration cannot be negative")
        val totalUnlockDuration: Double,

        val appUsage: Map<String, List<AppUsage>>,

        val webUsage: Map<String, Double>
)

public data class PhoneUsageData(
        @field:Min(value = 0, message = "Total incoming calls cannot be negative")
        val totalIncomingCalls: Int,

        @field:Min(value = 0, message = "Total outgoing calls cannot be negative")
        val totalOutgoingCalls: Int,

        @field:Min(value = 0, message = "Total phone duration cannot be negative")
        val totalPhoneDuration: Double,

        @field:Min(value = 0, message = "Total unique contacts cannot be negative")
        val totalUniqueContacts: Int
)

public data class MessagesUsageData(
        @field:Min(value = 0, message = "Total incoming messages cannot be negative")
        val totalIncomingMessages: Int,

        @field:Min(value = 0, message = "Total outgoing messages cannot be negative")
        val totalOutgoingMessages: Int,

        @field:Min(value = 0, message = "Total unique contacts cannot be negative")
        val totalUniqueContacts: Int
)

//NOTE: optional parameters are currently only available for iOS 15.0+
public data class KeyboardMetricsData(
        @field:Min(value = 0, message = "Total words cannot be negative")
        val totalWords: Int,

        @field:Min(value = 0, message = "Total altered words cannot be negative")
        val totalAlteredWords: Int,

        @field:Min(value = 0, message = "Total taps cannot be negative")
        val totalTaps: Int,

        @field:Min(value = 0, message = "Total drags cannot be negative")
        val totalDrags: Int,

        @field:Min(value = 0, message = "Total deletes cannot be negative")
        val totalDeletes: Int,

        @field:Min(value = 0, message = "Total emojis cannot be negative")
        val totalEmojis: Int,

        @field:Min(value = 0, message = "Total paths cannot be negative")
        val totalPaths: Int,

        @field:Min(value = 0, message = "Total path time cannot be negative")
        val totalPathTime: Double,

        @field:Min(value = 0, message = "Total path length cannot be negative")
        val totalPathLength: Double,

        @field:Min(value = 0, message = "Total auto corrections cannot be negative")
        val totalAutoCorrections: Int,

        @field:Min(value = 0, message = "Total space corrections cannot be negative")
        val totalSpaceCorrections: Int,

        @field:Min(value = 0, message = "Total retro corrections cannot be negative")
        val totalRetroCorrections: Int,

        @field:Min(value = 0, message = "Total transposition corrections cannot be negative")
        val totalTranspositionCorrections: Int,

        @field:Min(value = 0, message = "Total insert key corrections cannot be negative")
        val totalInsertKeyCorrections: Int,

        @field:Min(value = 0, message = "Total skip touch corrections cannot be negative")
        val totalSkipTouchCorrections: Int,

        @field:Min(value = 0, message = "Total near key corrections cannot be negative")
        val totalNearKeyCorrections: Int,

        @field:Min(value = 0, message = "Total substitution corrections cannot be negative")
        val totalSubstitutionCorrections: Int,

        @field:Min(value = 0, message = "Total hit test corrections cannot be negative")
        val totalHitTestCorrections: Int,

        @field:Min(value = 0, message = "Total typing duration cannot be negative")
        val totalTypingDuration: Double,

        val totalPathPauses: Int?,
        val totalPauses: Int?,
        val totalTypingEpisodes: Int?,
        val pathTypingSpeed: Double?, // QuickWords per minute,
        val typingSpeed: Double?, // typing rate in characters per second,
        val emojiCountBySentiment: Map<String, Int>,
        val wordCountBySentiment: Map<String, Int>
)
