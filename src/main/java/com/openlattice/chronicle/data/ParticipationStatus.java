package com.openlattice.chronicle.data;

/**
 * @author alfoncenzioka &lt;alfonce@openlattice.com&gt;
 */
public enum ParticipationStatus {
    ENROLLED,
    NOT_ENROLLED,
    PAUSED,
    COLLECTION_COMPLETED,
    UNKNOWN,
    ERASED;

    public static final String ERASURE_STATUS_VERSION_HEADER = "X-Chronicle-Erasure-Status-Version";
    public static final String ERASURE_STATUS_VERSION = "1";

    public ParticipationStatus forDeviceClient(boolean supportsErasureStatus) {
        return this == ERASED && !supportsErasureStatus ? NOT_ENROLLED : this;
    }
}
