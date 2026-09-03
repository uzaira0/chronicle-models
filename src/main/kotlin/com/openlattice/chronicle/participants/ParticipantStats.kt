package com.openlattice.chronicle.participants

import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

/**
 * @author alfoncenzioka &lt;alfonce@openlattice.com&gt;
 */
public data class ParticipantStats(
    val studyId: UUID,
    val participantId: String,
    val androidLastPing: OffsetDateTime?=null,
    val androidFirstDate: OffsetDateTime? = null,
    val androidLastDate: OffsetDateTime? = null,
    val androidUniqueDates: Set<LocalDate> = setOf(),
    val iosLastPing: OffsetDateTime?=null,
    val iosFirstDate: OffsetDateTime? = null,
    val iosLastDate: OffsetDateTime? = null,
    val iosUniqueDates: Set<LocalDate> = setOf(),
    val tudFirstDate: OffsetDateTime? = null,
    val tudLastDate: OffsetDateTime? = null,
    val tudUniqueDates: Set<LocalDate> = setOf()
)
