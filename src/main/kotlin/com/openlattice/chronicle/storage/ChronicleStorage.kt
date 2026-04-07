package com.openlattice.chronicle.storage

/**
 *
 * @author Matthew Tamayo-Rios &lt;matthew@openlattice.com&gt;
 */
public enum class ChronicleStorage(public val id: String) {

    /**
     * Most things default to platform storage. It should only be used for collecting and storing smaller data sets.
     */
    PLATFORM("default"),

    /**
     * Platform download hits the read only endpoint of the cluster and is tuned to allow deep queueing of downloads
     * without interfering with operation of the website.
     */
    PLATFORM_READ("platform_read"),
    /**
     * Chronicle storage is expected to handle larger data sets and in production is likely to live in a data warehouse
     * like redshift or snowflake
     */
    CHRONICLE("chronicle");

}