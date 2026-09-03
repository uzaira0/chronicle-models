package com.openlattice.chronicle.study

import com.google.common.base.Preconditions
import com.openlattice.chronicle.sensorkit.SensorSetting
import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.OffsetDateTime

/**
 * @author Solomon Tang <solomon@openlattice.com>
 */
public data class StudyUpdate(
    @field:Size(max = 255, message = "Title exceeds maximum length")
    val title: String? = null,

    @field:Size(max = 4000, message = "Description exceeds maximum length")
    val description: String? = null,

    @field:DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @field:DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    val lat: Double? = null,

    @field:DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @field:DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    val lon: Double? = null,

    @field:Size(max = 255, message = "Group exceeds maximum length")
    val group: String? = null,

    @field:Size(max = 50, message = "Version exceeds maximum length")
    val version: String? = null,

    @field:Valid
    val settings: StudySettings? = null,

    val modules: Map<String, Any>? = null,

    @field:Size(max = 255, message = "Contact exceeds maximum length")
    val contact: String? = null,

    val notificationsEnabled: Boolean? = null,

    @field:Size(max = 36, message = "Storage name exceeds maximum length")
    @field:Pattern(regexp = "^[a-zA-Z]*$", message = "Storage name must contain only alphabetic characters")
    val storage: String? = null,
) {
    init {
        Preconditions.checkArgument(
            title == null || title.isNotBlank(),
            "Title cannot be blank."
        )
        Preconditions.checkArgument(
            contact == null || contact.isNotBlank(),
            "Contact cannot be blank."
        )
    }
}

