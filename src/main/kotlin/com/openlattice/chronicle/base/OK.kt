package com.openlattice.chronicle.base

/**
 *
 * @author Matthew Tamayo-Rios &lt;matthew@openlattice.com&gt;
 */
public data class OK( val msg : String = "SUCCESS" ) {
    public companion object {
        @JvmField
        public val ok: OK = OK()
    }
}