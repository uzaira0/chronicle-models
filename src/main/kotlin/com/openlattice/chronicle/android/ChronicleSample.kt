package com.openlattice.chronicle.android

import com.fasterxml.jackson.annotation.JsonTypeInfo

/**
 *
 * @author Matthew Tamayo-Rios &lt;matthew@getmethodic.com&gt;
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public interface ChronicleSample {
}