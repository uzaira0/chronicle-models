package com.openlattice.chronicle.study

/**
 * Lifecycle status for studies.
 *
 * ACTIVE -> ARCHIVED -> ACTIVE (unarchive)
 * ACTIVE or ARCHIVED -> SCHEDULED_FOR_DELETION: deletion requested and the study's data blocked;
 * erasure runs only when an owner starts it, and cancelling restores the earlier status.
 */
public enum class StudyLifecycleStatus {
    ACTIVE,
    ARCHIVED,
    SCHEDULED_FOR_DELETION
}
