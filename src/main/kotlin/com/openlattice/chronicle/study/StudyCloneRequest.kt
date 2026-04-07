package com.openlattice.chronicle.study

/**
 * Request to clone a study.
 *
 * @property newTitle Title for the cloned study. If blank, uses "Copy of <original title>".
 * @property includeParticipants Whether to copy participant registrations to the clone.
 * @property includeSettings Whether to copy study settings to the clone.
 */
public data class StudyCloneRequest(
    val newTitle: String = "",
    val includeParticipants: Boolean = false,
    val includeSettings: Boolean = true
)
