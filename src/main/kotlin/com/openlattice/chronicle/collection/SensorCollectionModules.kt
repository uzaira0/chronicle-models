package com.openlattice.chronicle.collection

import com.openlattice.chronicle.android.AndroidSensorType

public object SensorCollectionModules {

    public val byType: Map<AndroidSensorType, CollectionModuleId> = mapOf(
        AndroidSensorType.accelerometer to CollectionModuleId.SENSOR_ACCELEROMETER,
        AndroidSensorType.light to CollectionModuleId.SENSOR_LIGHT,
        AndroidSensorType.proximity to CollectionModuleId.SENSOR_PROXIMITY,
        AndroidSensorType.screenOrientation to CollectionModuleId.SENSOR_SCREEN_ORIENTATION,
        AndroidSensorType.stepCounter to CollectionModuleId.SENSOR_STEP_COUNTER,
        AndroidSensorType.gyroscope to CollectionModuleId.SENSOR_GYROSCOPE,
        AndroidSensorType.magnetometer to CollectionModuleId.SENSOR_MAGNETOMETER,
        AndroidSensorType.gravity to CollectionModuleId.SENSOR_GRAVITY,
        AndroidSensorType.linearAcceleration to CollectionModuleId.SENSOR_LINEAR_ACCELERATION,
        AndroidSensorType.rotationVector to CollectionModuleId.SENSOR_ROTATION_VECTOR,
        AndroidSensorType.significantMotion to CollectionModuleId.SENSOR_SIGNIFICANT_MOTION,
        AndroidSensorType.tiltDetector to CollectionModuleId.SENSOR_TILT_DETECTOR,
    )

    public val typeOf: Map<CollectionModuleId, AndroidSensorType> =
        byType.entries.associate { (type, moduleId) -> moduleId to type }

    public val sensorModuleIds: Set<CollectionModuleId> =
        byType.values.toSet()

    public val sensorDisplayOrder: List<CollectionModuleId> = byType.values.toList()

    public fun moduleFor(type: AndroidSensorType): CollectionModuleId = byType.getValue(type)

    public fun sensorTypeOf(moduleId: CollectionModuleId): AndroidSensorType? = typeOf[moduleId]

    public fun isSensorModule(moduleId: CollectionModuleId): Boolean = moduleId in sensorModuleIds
}
