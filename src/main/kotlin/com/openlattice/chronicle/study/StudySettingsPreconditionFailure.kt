package com.openlattice.chronicle.study

/**
 * Body returned with HTTP 412 when a settings write carries an `If-Match` revision that no longer
 * matches the study's current [settingsRevision].
 *
 * The current revision and the current settings map are both included so the client can re-render
 * from this response instead of issuing another GET and racing again.
 */
public data class StudySettingsPreconditionFailure(
    val settingsRevision: Long,
    val settings: Map<StudySettingType, StudySetting>,
    val message: String = "Study settings were modified by another client",
)
