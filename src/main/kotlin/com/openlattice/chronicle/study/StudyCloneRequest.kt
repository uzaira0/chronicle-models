package com.openlattice.chronicle.study

import jakarta.validation.constraints.Size

/**
 * Request to clone a study.
 *
 * @property newTitle Title for the cloned study. If blank, uses "Copy of <original title>".
 * @property includeSettings Whether to copy study settings to the clone.
 */
public data class StudyCloneRequest(
    @field:Size(max = 2000, message = "Title exceeds maximum length")
    val newTitle: String = "",
    val includeSettings: Boolean = true
)
