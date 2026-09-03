package com.openlattice.chronicle.collection

/**
 * Privacy classification for data collection module output (design §1A.4).
 *
 * Each class carries a [defaultEnabled] policy that the settings resolver (Phase 3)
 * uses when no explicit server setting or local preference is present.
 *
 * Hard rule (design §1A.4, refactor plan decisions #15–17): [PHYSICAL_TELEMETRY],
 * [LOCAL_PARTICIPANT_LABEL], [MEDIA_CONTENT], and [INTERACTION_METADATA] are
 * **never** enabled implicitly — their [defaultEnabled] is `false` and only an
 * explicit server setting / preference may flip them.
 *
 * [MEDIA_CONTENT] / [INTERACTION_METADATA] were added by the sensing expansion
 * (see `docs/SENSING-EXPANSION-DESIGN.md`).
 *
 * @author uzaira0
 */
public enum class CollectionPrivacyClass(
    /**
     * Whether a module of this class is enabled by default when no explicit
     * setting is supplied. Privacy-sensitive classes are `false`.
     */
    public val defaultEnabled: Boolean,
) {
    /** App-usage event metadata, study-controlled. */
    BEHAVIORAL_METADATA(true),

    /** Battery/network/screen/power lifecycle state, enabled once enrolled. */
    DEVICE_STATE_METADATA(true),

    /** Hardware sensor samples — opt-in only, never enabled implicitly. */
    PHYSICAL_TELEMETRY(false),

    /** Participant label chosen locally on-device — never enabled implicitly. */
    LOCAL_PARTICIPANT_LABEL(false),

    /** Upload/queue health; carries no participant data. */
    OPERATIONAL_DIAGNOSTICS(true),

    /** Which modeled sensors the device exposes; report-only. */
    DEVICE_CAPABILITY(true),

    /**
     * Raw audio rendered by other apps (media playback content). Chronicle's first
     * content-bearing class — opt-in only, never enabled implicitly.
     */
    MEDIA_CONTENT(false),

    /**
     * Touch-region and scroll interaction events — content-free (no element text),
     * but captured via an Accessibility service. Opt-in only, never enabled implicitly.
     */
    INTERACTION_METADATA(false),

    /**
     * Camera-derived gaze / visual-attention estimates (a future eye-tracking module). A
     * biometric-adjacent, content-bearing-camera class — the most sensitive tier. Opt-in only,
     * never enabled implicitly. Reserved ahead of the `gaze_tracking` module's implementation.
     */
    GAZE_TELEMETRY(false),

    /**
     * On-screen *content* read from the accessibility tree (element text / contentDescription) —
     * the opposite of the content-free [INTERACTION_METADATA]. The highest-sensitivity content
     * class (may contain PII / messages). Opt-in only, never enabled implicitly, and gated behind
     * an explicit IRB consent path. Reserved ahead of any content-capture module.
     */
    SCREEN_CONTENT(false),

    /**
     * Device geographic location — coarse or precise GPS/network position, geofence transitions,
     * and mobility traces (a future opt-in module). Among the most re-identifying signals Chronicle
     * could collect, so it is its own tier: opt-in only, never enabled implicitly, and intended to
     * sit behind an explicit IRB consent path. Reserved ahead of any location module's
     * implementation — no capture path exists yet.
     */
    PRECISE_LOCATION(false),

    /**
     * Physiological / health-derived measurements — sleep inference, activity/exercise, heart rate,
     * steps-as-health and other Health Connect records. Clinically sensitive; opt-in only, never
     * enabled implicitly. Sourced from on-device classifiers (e.g. the Play Services Sleep API) and
     * the system Health Connect store, never from raw audio/video.
     */
    HEALTH_METRICS(false),

    /**
     * Ambient-sound classification labels derived ON DEVICE from short, duty-cycled
     * microphone windows (e.g. Apple SoundAnalysis) — labels + confidences only; the raw
     * audio is discarded at the classifier boundary and never persisted or uploaded.
     * The microphone is a distinct sensitivity channel even when only labels leave it
     * (it can overhear non-consenting third parties), so it is its own tier: opt-in
     * only, never enabled implicitly. Distinct from [MEDIA_CONTENT], which is mic-free.
     */
    AMBIENT_AUDIO_CONTEXT(false),

    /**
     * Communication-log metadata — call/SMS counts, direction, timing and duration (never message
     * bodies or call audio, and identifiers are hashed). Reveals a social graph including
     * non-consenting third parties, so it is its own tier: opt-in only, never enabled implicitly,
     * and gated behind an explicit IRB consent path. Reserved ahead of any communication-log
     * module's implementation — no capture path exists yet.
     */
    COMMUNICATION_METADATA(false),
    ;
}
