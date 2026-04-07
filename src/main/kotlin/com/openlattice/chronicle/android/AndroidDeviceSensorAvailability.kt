package com.openlattice.chronicle.android

import java.time.OffsetDateTime

public data class AndroidDeviceSensorAvailability(
    val participantId: String = "",
    val deviceId: String = "",
    val availableSensors: Set<AndroidSensorType> = emptySet(),
    val unavailableSensors: Set<AndroidSensorType> = emptySet(),
    val reportedAt: OffsetDateTime? = null
)
