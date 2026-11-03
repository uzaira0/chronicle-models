package com.openlattice.chronicle.sources;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Phones send and the server stores the subtype's class name as "@class"; only these subtypes are accepted.
 *
 * @author Matthew Tamayo-Rios &lt;matthew@openlattice.com&gt;
 */
@JsonTypeInfo( use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "@class" )
@JsonSubTypes( {
        @JsonSubTypes.Type( value = AndroidDevice.class, name = "com.openlattice.chronicle.sources.AndroidDevice" ),
        @JsonSubTypes.Type( value = IOSDevice.class, name = "com.openlattice.chronicle.sources.IOSDevice" )
} )
public interface SourceDevice {

}
