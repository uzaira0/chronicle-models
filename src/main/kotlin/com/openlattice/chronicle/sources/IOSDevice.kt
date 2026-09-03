package com.openlattice.chronicle.sources

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonTypeInfo
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.util.*

/**
 * @author alfoncenzioka &lt;alfonce@openlattice.com&gt;
 *
 * TODO: We are unfortunately stuck with this typo forever until we migrate the json stored in the db.
 * Describes an iOS device: https://developer.apple.com/documentation/uikit/uidevice
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public class IOSDevice @JsonCreator constructor(
        @field:NotBlank(message = "Device name is required")
        @field:Size(max = 255, message = "Device name exceeds maximum length")
        public val name: String,

        @field:NotBlank(message = "System name is required")
        @field:Size(max = 100, message = "System name exceeds maximum length")
        public val systemName: String,

        @field:NotBlank(message = "Model is required")
        @field:Size(max = 255, message = "Model exceeds maximum length")
        public val model: String,

        @field:NotBlank(message = "Localized model is required")
        @field:Size(max = 255, message = "Localized model exceeds maximum length")
        public val localizedModel: String,

        @field:NotBlank(message = "Version is required")
        @field:Size(max = 50, message = "Version exceeds maximum length")
        public val version: String,

        @field:NotBlank(message = "Device ID is required")
        @field:Size(max = 255, message = "Device ID exceeds maximum length")
        public val deviceId: String,

        @field:Size(max = 500, message = "APN device token exceeds maximum length")
        public val apnDeviceToken: String = "" //This is the token for device push notifications.
) : SourceDevice {

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is IOSDevice) {
            return false
        }

        return name == other.name &&
                systemName == other.systemName &&
                model == other.model &&
                localizedModel == other.localizedModel &&
                version == other.version &&
                deviceId == other.deviceId

    }

    override fun hashCode(): Int {
        return Objects.hash(name, systemName, model, localizedModel, version, deviceId)
    }

    override fun toString(): String {
        return "IOSDevice{name=$name, systemName=$systemName, model=$model, localizedModel=$localizedModel, version=$version, deviceId=$deviceId }"
    }
}
