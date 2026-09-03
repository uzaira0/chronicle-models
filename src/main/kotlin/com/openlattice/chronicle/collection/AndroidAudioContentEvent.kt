package com.openlattice.chronicle.collection

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.OffsetDateTime

/**
 * One media-metadata sample produced by the `audio_content` collection module
 * (see `docs/SENSING-EXPANSION-DESIGN.md` §4.2).
 *
 * This is the shared **wire contract** between the Android `:collection-audio` module and the
 * server for the content-bearing layer. It records *what* the participant is playing — the
 * active media session's [title]/[artist]/[album] (`MediaMetadata`) plus its [audioPackage] and
 * playback timing — read from `MediaSessionManager` (notification-listener access required).
 *
 * It is **still mic-free**: no audio waveform is captured. This is the metadata the producing app
 * publishes, NOT the audio. (Raw playback-audio capture via `MediaProjection`/`RECORD_AUDIO` is a
 * deliberately separate, unbuilt escalation.) Because it can carry PII (a track/episode title), it
 * is `MEDIA_CONTENT`-class, opt-in, never enabled implicitly, and gated behind explicit consent.
 *
 * Hard constraint: this DTO carries **no** `apiKey`, signing secret, or `participantId` field.
 * Unknown JSON fields are ignored for forward compatibility.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public data class AndroidAudioContentEvent(
    /** Stable per-event identifier, used for de-duplication on insert/upload. */
    public val id: String,
    /** Sample time in UTC. */
    public val timestamp: OffsetDateTime,
    /** Device-default time-zone id at sample time. */
    public val timezone: String,
    /** Package of the app producing the audio (the media-session owner). */
    public val audioPackage: String,
    /** Media title (`MediaMetadata.METADATA_KEY_TITLE`); `null` if the app published none. */
    public val title: String? = null,
    /** Media artist (`METADATA_KEY_ARTIST`); `null` if absent. */
    public val artist: String? = null,
    /** Media album (`METADATA_KEY_ALBUM`); `null` if absent. */
    public val album: String? = null,
    /** Total media duration in milliseconds (`METADATA_KEY_DURATION`); `null` if unknown. `>= 0`. */
    public val durationMillis: Long? = null,
    /** Playback position in milliseconds at sample time (`PlaybackState.getPosition`); `null` if unknown. `>= 0`. */
    public val positionMillis: Long? = null,
) {
    init {
        require(id.isNotBlank()) { "AndroidAudioContentEvent.id must not be blank" }
        require(timezone.isNotBlank()) { "AndroidAudioContentEvent.timezone must not be blank" }
        require(audioPackage.isNotBlank()) { "AndroidAudioContentEvent.audioPackage must not be blank" }
        durationMillis?.let { require(it >= 0) { "AndroidAudioContentEvent.durationMillis must be non-negative: $it" } }
        positionMillis?.let { require(it >= 0) { "AndroidAudioContentEvent.positionMillis must be non-negative: $it" } }
    }
}
