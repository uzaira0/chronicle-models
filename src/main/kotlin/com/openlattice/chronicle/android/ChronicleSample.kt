package com.openlattice.chronicle.android

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

/**
 *
 * @author Matthew Tamayo-Rios &lt;matthew@getmethodic.com&gt;
 */
// The wire name stays the class name phones already send; only listed subtypes are accepted.
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "@class")
@JsonSubTypes(JsonSubTypes.Type(value = ChronicleUsageEvent::class, name = "com.openlattice.chronicle.android.ChronicleUsageEvent"))
public interface ChronicleSample {
}