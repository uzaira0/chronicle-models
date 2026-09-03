package com.openlattice.chronicle.study

/**
 * Lifecycle status for studies.
 *
 * ACTIVE -> ARCHIVED -> ACTIVE (unarchive)
 * ACTIVE -> SCHEDULED_FOR_DELETION -> (deleted by scheduler)
 * ARCHIVED -> SCHEDULED_FOR_DELETION -> (deleted by scheduler)
 */
public enum class StudyLifecycleStatus {
    ACTIVE,
    ARCHIVED,
    SCHEDULED_FOR_DELETION
}
