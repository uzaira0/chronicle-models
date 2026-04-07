package com.openlattice.chronicle.sources;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * @author Matthew Tamayo-Rios &lt;matthew@openlattice.com&gt;
 */
@JsonTypeInfo( use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class" )
public interface SourceDevice {

}
