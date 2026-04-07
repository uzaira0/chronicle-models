package com.openlattice.chronicle.android

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.OffsetDateTime
import java.util.*

public data class AndroidSensorSample(
        @field:NotNull(message = "Sample ID is required")
        val id: UUID,

        @field:NotNull(message = "Sensor type is required")
        val sensor: AndroidSensorType,

        @field:NotNull(message = "Timestamp is required")
        val timestamp: OffsetDateTime,

        @field:NotBlank(message = "Timezone is required")
        @field:Size(max = 100, message = "Timezone exceeds maximum length")
        val timezone: String,

        val x: Float? = null,
        val y: Float? = null,
        val z: Float? = null,
        val w: Float? = null,
        val accuracy: Int? = null
)
