package com.openlattice.chronicle.collection

import com.openlattice.chronicle.generated.ChronicleHeaders

public object CollectionUploadEvidence {
    public const val SETTINGS_VERSION_HEADER: String = ChronicleHeaders.COLLECTION_VERSION_HEADER
    public const val UNKNOWN_SETTINGS_VERSION: Int = 0
    public const val DECISION_SEQ_HEADER: String = ChronicleHeaders.COLLECTION_DECISION_HEADER
    /** A row captured before the phone recorded any decision sequence; it travels with no decision header. */
    public const val UNKNOWN_DECISION_SEQ: Long = 0
}
