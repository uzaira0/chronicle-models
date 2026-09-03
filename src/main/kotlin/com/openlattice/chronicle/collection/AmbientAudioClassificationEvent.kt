package com.openlattice.chronicle.collection

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.OffsetDateTime

/**
 * One on-device sound-classification result produced by the `ambient_audio` collection module:
 * a single sound-class label + confidence within one short, duty-cycled listen window.
 *
 * Shared **wire contract** between the iOS collector (Apple SoundAnalysis) and the server, and
 * **labels-only by construction**: classification runs entirely on device and the audio is
 * discarded at the classifier boundary — no recording, transcript, voice, waveform, or any other
 * audio representation exists past it, anywhere in the pipeline. This is the mic-bearing
 * counterpart of the deliberately mic-free [AndroidAudioActivityEvent] (`audio_activity`);
 * the two module ids must never be conflated. `AMBIENT_AUDIO_CONTEXT`-class, opt-in (default OFF).
 *
 * Hard constraint: no `apiKey`, signing secret, or `participantId` field. Unknown JSON fields are
 * ignored for forward compatibility.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public data class AmbientAudioClassificationEvent(
    /** Stable per-event identifier, used for de-duplication on insert/upload. */
    public val id: String,
    /** Listen-window end time in UTC. */
    public val timestamp: OffsetDateTime,
    /** Device-default time-zone id at classification time. */
    public val timezone: String,
    /** Listen-window start (epoch millis). */
    public val windowStartMillis: Long,
    /** Listen-window end (epoch millis). */
    public val windowEndMillis: Long,
    /** Classifier sound-class identifier (e.g. `music`, `speech`, `television`). */
    public val label: String,
    /** Best classifier confidence observed for this label within the window, in `[0, 1]`. */
    public val confidence: Double,
    /** Classifier identifier/version that produced the label (e.g. `apple-soundanalysis-v1`). */
    public val classifierVersion: String? = null,
) {
    init {
        require(id.isNotBlank()) { "AmbientAudioClassificationEvent.id must not be blank" }
        require(timezone.isNotBlank()) { "AmbientAudioClassificationEvent.timezone must not be blank" }
        require(label.isNotBlank()) { "AmbientAudioClassificationEvent.label must not be blank" }
        require(confidence in 0.0..1.0) {
            "AmbientAudioClassificationEvent.confidence must be within [0, 1]: $confidence"
        }
        require(windowEndMillis >= windowStartMillis) {
            "AmbientAudioClassificationEvent window must not end before it starts: " +
                "$windowStartMillis..$windowEndMillis"
        }
    }
}
