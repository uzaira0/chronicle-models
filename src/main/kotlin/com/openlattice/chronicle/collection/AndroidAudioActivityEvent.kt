package com.openlattice.chronicle.collection

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.OffsetDateTime

/**
 * What triggered an [AndroidAudioActivityEvent] sample. The audio module is event-driven: it
 * emits a sample on each relevant device-audio transition (plus periodic snapshots), so the
 * server sees discrete headphone-plug, play/pause, volume, and call transitions rather than only
 * polled state.
 */
public enum class AudioEventType {
    /** Periodic full-state snapshot (not tied to a specific transition). */
    SNAPSHOT,
    /** Media playback started/stopped/changed (`AudioManager` playback callback / media session). */
    PLAYBACK_CHANGE,
    /** An output device connected or disconnected (`AudioDeviceCallback`); see [routeConnected]. */
    ROUTE_CHANGE,
    /** Audio is "becoming noisy" — output fell back to speaker, e.g. headphones unplugged. */
    BECOMING_NOISY,
    /** A media-stream volume changed. */
    VOLUME_CHANGE,
    /** A phone/VoIP call started or ended (`AudioManager.getMode`); see [callActive]. */
    CALL_CHANGE,
    /** An active media session appeared, disappeared, or changed playback/owner. */
    MEDIA_SESSION_CHANGE,
}

/** The kind of audio being rendered, from `AudioAttributes.CONTENT_TYPE_*` / an active media session. */
public enum class AudioContentType { MUSIC, SPEECH, MOVIE, SONIFICATION, UNKNOWN }

/** Playback state of the active media session, from `PlaybackState.STATE_*`. */
public enum class AudioPlaybackState { PLAYING, PAUSED, STOPPED, BUFFERING, NONE }

/** Where audio is being routed, collapsed from `AudioDeviceInfo.getType()`. */
public enum class AudioOutputRoute { SPEAKER, EARPIECE, WIRED_HEADPHONES, BLUETOOTH, USB, HEARING_AID, OTHER, UNKNOWN }

/** Ringer mode, from `AudioManager.getRingerMode()`. */
public enum class AudioRingerMode { SILENT, VIBRATE, NORMAL, UNKNOWN }

/**
 * One content-free app-audio-activity sample produced by the `audio_activity` collection module
 * (see `docs/SENSING-EXPANSION-DESIGN.md` §4.1).
 *
 * Shared **wire contract** between the Android `:collection-audio` module and the server, and
 * **mic-free by construction**: every field is derived from the device's own playback/output state
 * — `AudioManager` and `AudioDeviceCallback` (Tier 1, no permission: [audioActive], [outputRoute],
 * [routeConnected], [mediaVolume]/[maxMediaVolume], [ringerMode], [dndActive], [callActive]) and,
 * when notification-listener access is granted, the active `MediaSession` (Tier 2: [audioPackage],
 * [contentType], [playbackState]). The microphone is never opened and no audio waveform is captured.
 * `BEHAVIORAL_METADATA`-class, opt-in. *What* is playing (track title/artist) is NOT here — that is
 * the separate, content-bearing [AndroidAudioContentEvent] (`audio_content`, `MEDIA_CONTENT`).
 *
 * The sample is event-driven: [eventType] names the transition that produced it (headphone
 * connect/disconnect, play/pause, volume change, call start/end, media-session change) and every
 * sample also carries the current device-audio state so each event is self-describing. Tier-1-only
 * samples (no listener grant) leave [audioPackage], [contentType], and [playbackState] `null`.
 *
 * Hard constraint: no `apiKey`, signing secret, or `participantId` field. Unknown JSON fields are
 * ignored for forward compatibility.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public data class AndroidAudioActivityEvent(
    /** Stable per-event identifier, used for de-duplication on insert/upload. */
    public val id: String,
    /** Sample time in UTC. */
    public val timestamp: OffsetDateTime,
    /** Device-default time-zone id at sample time. */
    public val timezone: String,
    /** The transition that produced this sample. */
    public val eventType: AudioEventType,
    /** Whether media audio was actively playing at sample time (`AudioManager.isMusicActive`). */
    public val audioActive: Boolean,
    /**
     * Package of the app producing audio, when resolvable from an active media session (Tier 2,
     * needs notification-listener access). `null` on a Tier-1-only sample.
     */
    public val audioPackage: String? = null,
    /** Content category of the active playback (music/speech/movie…), when known. */
    public val contentType: AudioContentType? = null,
    /** Active media-session playback state, when known. */
    public val playbackState: AudioPlaybackState? = null,
    /** Where audio is routed (speaker / headphones / Bluetooth…), when known. */
    public val outputRoute: AudioOutputRoute? = null,
    /**
     * For an [AudioEventType.ROUTE_CHANGE]: `true` if [outputRoute] connected, `false` if it
     * disconnected. `null` for non-route events.
     */
    public val routeConnected: Boolean? = null,
    /** Current media-stream volume (`STREAM_MUSIC`); `null` if unavailable. Non-negative. */
    public val mediaVolume: Int? = null,
    /** Max media-stream volume, so [mediaVolume] is interpretable; `null` if unavailable. `>= 1`. */
    public val maxMediaVolume: Int? = null,
    /** Ringer mode (silent / vibrate / normal), when known. */
    public val ringerMode: AudioRingerMode? = null,
    /** Whether Do-Not-Disturb / a non-`ALL` interruption filter was active, when known. */
    public val dndActive: Boolean? = null,
    /**
     * Whether a phone/VoIP call was in progress at sample time (`AudioManager.getMode()` is
     * `MODE_IN_CALL`/`MODE_IN_COMMUNICATION`). Content-free — no number, party, or call audio. `null`
     * if undetermined.
     */
    public val callActive: Boolean? = null,
) {
    init {
        require(id.isNotBlank()) { "AndroidAudioActivityEvent.id must not be blank" }
        require(timezone.isNotBlank()) { "AndroidAudioActivityEvent.timezone must not be blank" }
        mediaVolume?.let { require(it >= 0) { "AndroidAudioActivityEvent.mediaVolume must be non-negative: $it" } }
        maxMediaVolume?.let { require(it >= 1) { "AndroidAudioActivityEvent.maxMediaVolume must be at least 1: $it" } }
    }
}
