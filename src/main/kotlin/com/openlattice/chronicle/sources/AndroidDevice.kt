package com.openlattice.chronicle.sources

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.openlattice.chronicle.ChronicleFields
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.util.*

/**
 *
 * @author Matthew Tamayo-Rios &lt;matthew@getmethodic.com&gt;
 */

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public data class AndroidDevice(
    @field:NotBlank(message = "Device name is required")
    @field:Size(max = 255, message = "Device name exceeds maximum length")
    @param:JsonProperty(ChronicleFields.DEVICE) val device: String,

    @field:NotBlank(message = "Model is required")
    @field:Size(max = 255, message = "Model exceeds maximum length")
    @param:JsonProperty(ChronicleFields.MODEL) val model: String,

    @field:NotBlank(message = "Codename is required")
    @field:Size(max = 255, message = "Codename exceeds maximum length")
    @param:JsonProperty(ChronicleFields.CODENAME) val codename: String,

    @field:NotBlank(message = "Brand is required")
    @field:Size(max = 255, message = "Brand exceeds maximum length")
    @param:JsonProperty(ChronicleFields.BRAND) val brand: String,

    @field:NotBlank(message = "OS version is required")
    @field:Size(max = 50, message = "OS version exceeds maximum length")
    @param:JsonProperty(ChronicleFields.OS_VERSION) val osVersion: String,

    @field:NotBlank(message = "SDK version is required")
    @field:Size(max = 50, message = "SDK version exceeds maximum length")
    @param:JsonProperty(ChronicleFields.SDK_VERSION) val sdkVersion: String,

    @field:NotBlank(message = "Product is required")
    @field:Size(max = 255, message = "Product exceeds maximum length")
    @param:JsonProperty(ChronicleFields.PRODUCT) val product: String,

    @field:NotBlank(message = "Device ID is required")
    @field:Size(max = 255, message = "Device ID exceeds maximum length")
    @param:JsonProperty(ChronicleFields.DEVICE_ID) val deviceId: String,

    @param:JsonProperty(ChronicleFields.ADDITIONAL_INFO) val additionalInfo: Map<String, Any> = mapOf(),

    @field:Size(max = 500, message = "FCM registration token exceeds maximum length")
    @param:JsonProperty(ChronicleFields.FCM_REGISTRATION_TOKEN) val fcmRegistrationToken: String = ""
) : SourceDevice
