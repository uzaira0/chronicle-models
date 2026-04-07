package com.openlattice.chronicle.android

import com.google.common.collect.SetMultimap
import java.util.*

/**
 *
 * @author Matthew Tamayo-Rios &lt;matthew@openlattice.com&gt;
 */
public class LegacyChronicleData(data: List<SetMultimap<UUID, Any>>) : List<SetMultimap<UUID, Any>> by data {}