package com.openlattice.chronicle.collection

/**
 * What caused a participant's per-module consent decision to be reported to the server
 * (per-module consent design §3.3). Lets researchers tell an enrollment-time choice
 * apart from a later self-service toggle, a study-setting-driven re-decision, or a
 * withdrawal.
 *
 *  - [ENROLLMENT] — the decisions made in the enrollment orientation wizard.
 *  - [PARTICIPANT_TOGGLE] — the participant flipped an optional module in the
 *    "Data Sharing" management surface after enrollment.
 *  - [SETTINGS_CHANGE] — the participant re-decided because a study-setting change made
 *    a module newly required (mandatory consent-or-leave).
 *  - [WITHDRAWAL] — the participant chose to leave the study rather than accept a
 *    now-required module (an audit/notification record only; see design §7.1).
 *
 * @author uzaira0
 */
public enum class ConsentTrigger {
    ENROLLMENT,
    PARTICIPANT_TOGGLE,
    SETTINGS_CHANGE,
    WITHDRAWAL,
}
