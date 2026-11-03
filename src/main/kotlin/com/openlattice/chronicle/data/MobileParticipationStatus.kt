package com.openlattice.chronicle.data

import java.time.OffsetDateTime

/**
 * A closed capture window on the server's clock: a pause, completion, archive or processing block, `[start, end)`.
 * [end] is null while the window lasts. The phone converts both ends to its own clock and collects nothing inside.
 */
public data class CaptureWindow(val start: OffsetDateTime, val end: OffsetDateTime? = null)

/**
 * The participant's latest hard stop (Mark not enrolled or withdrawal, any writer): [generation] counts every one, and
 * [at] is when the latest began, on the server's clock. A phone that missed the stop (it was enrolled again before the
 * phone polled) learns of it here, drops every queued row captured before [at] and clears what has no capture time.
 * The phone maps [at] through its synced server clock before judging its locally timestamped rows.
 */
public data class HardStop(val generation: Long, val at: OffsetDateTime)

/**
 * The phone's status poll: the participation status, every closed capture window, the latest hard stop (null before
 * the first), and the server's clock at the response, which the phone's server clock is measured from.
 */
public data class MobileParticipationStatus(
    val participationStatus: ParticipationStatus,
    val serverTime: OffsetDateTime,
    val captureWindows: List<CaptureWindow> = emptyList(),
    val hardStop: HardStop? = null,
)
