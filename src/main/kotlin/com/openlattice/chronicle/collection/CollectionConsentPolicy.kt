package com.openlattice.chronicle.collection

import com.openlattice.chronicle.study.StudySettings
import com.openlattice.chronicle.study.StudySettingType
import com.openlattice.chronicle.study.StudyFeature

/** A decision cites an immutable server-issued settings revision. */
public enum class IssuedCollectionDecisionState { ACCEPTED, DECLINED, UNAVAILABLE }

public data class IssuedCollectionDecision(
    val state: IssuedCollectionDecisionState,
    val settingsVersion: Int,
)

/** A decision survives every settings edit while the module stays enabled; only a disable then re-enable resets it. */
public fun moduleDecisionSurvives(
    current: CollectionModuleSetting,
    decisionVersion: Int = current.consentEpoch, previousEnabled: Boolean = true,
): Boolean = previousEnabled && current.enabled && decisionVersion >= current.consentEpoch

public fun moduleDecisionSurvives(
    module: CollectionModuleId,
    decisionVersion: Int,
    latest: AndroidDataCollectionSetting,
    revisions: List<AndroidDataCollectionSetting>,
): Boolean = issuedRun(module, decisionVersion, latest, revisions)?.let { run ->
    run.all { it.enabled }
} == true

/** A policy edit never voids an explicit required decline; only a disable resets it, as on the phone's applied state. */
public fun requiredDeclineStands(previousEnabled: Boolean, current: CollectionModuleSetting): Boolean =
    previousEnabled && current.enabled && current.required

/** Proof extends an acceptance, not a refusal: a ledger that cannot prove a disable keeps the decline. */
public fun requiredDeclineStands(
    module: CollectionModuleId,
    decisionVersion: Int,
    latest: AndroidDataCollectionSetting,
    revisions: List<AndroidDataCollectionSetting>,
): Boolean = issuedRun(module, decisionVersion, latest, revisions)?.let { run ->
    run.all { it.enabled } && requiredDeclineStands(run.first().enabled, run.last())
} ?: latest.effectiveModules()[module]?.let { requiredDeclineStands(previousEnabled = true, current = it) } == true

/** The module's policy at every issued revision from the decision through [latest]; null when the ledger cannot prove it. */
private fun issuedRun(
    module: CollectionModuleId,
    decisionVersion: Int,
    latest: AndroidDataCollectionSetting,
    revisions: List<AndroidDataCollectionSetting>,
): List<CollectionModuleSetting>? {
    val byVersion = revisions.associateBy { it.settingsVersion }
    if (byVersion.size != revisions.size || byVersion[latest.settingsVersion] != latest ||
        decisionVersion <= 0 || decisionVersion > latest.settingsVersion ||
        latest.settingsVersion.toLong() - decisionVersion + 1 > byVersion.size) return null
    return (decisionVersion..latest.settingsVersion).map { byVersion[it]?.effectiveModules()?.get(module) ?: return null }
}

/**
 * Carries disable resets across missed revisions. A module whose current policy the ledger does not record at this
 * version gets this version as its epoch: re-consent, never a stale survival.
 */
public fun AndroidDataCollectionSetting.withConsentEpochs(
    revisions: List<AndroidDataCollectionSetting>,
): AndroidDataCollectionSetting {
    val ordered = revisions.sortedBy { it.settingsVersion }
    val policies = ordered.associate { it.settingsVersion to it.effectiveModules(CollectionModuleId.activeModules) }
    return copy(modules = effectiveModules(CollectionModuleId.activeModules).mapValues { (module, current) ->
        if (policies[settingsVersion]?.get(module) != current) return@mapValues current.copy(consentEpoch = settingsVersion)
        var epoch = ordered.first().settingsVersion
        ordered.zipWithNext().forEach { (before, after) ->
            if (after.settingsVersion != before.settingsVersion + 1 || !moduleDecisionSurvives(
                policies.getValue(after.settingsVersion).getValue(module),
                previousEnabled = policies.getValue(before.settingsVersion).getValue(module).enabled)) epoch = after.settingsVersion
        }
        current.copy(consentEpoch = epoch)
    })
}

/**
 * The participant's standing decisions as the server reads them: required modules whose decline halts the whole
 * study, and enabled modules still waiting for the participant's review, which wait individually.
 */
public data class ParticipantConsentState(
    val declinedRequiredModules: Set<CollectionModuleId>,
    val modulesAwaitingDecision: Set<CollectionModuleId>,
)

/** Only an explicit required decline halts the whole study; undecided modules wait individually. */
public fun requiredDecisionHaltsCollection(enabled: Boolean, required: Boolean, declined: Boolean): Boolean =
    enabled && required && declined

/** Upload admission and the global halt use the same standing decisions as reconciliation; survival governs acceptances. */
public fun collectionIsHalted(
    latestSettings: AndroidDataCollectionSetting,
    issuedRevisions: List<AndroidDataCollectionSetting>,
    decisions: Map<CollectionModuleId, IssuedCollectionDecision>,
    uploadModules: Set<CollectionModuleId> = emptySet(),
): Boolean {
    val policies = latestSettings.effectiveModules()
    return standingRequiredDeclines(latestSettings, issuedRevisions, decisions).isNotEmpty() || uploadModules.any { module ->
        val policy = policies[module]
        val decision = decisions[module]
        policy?.enabled != true || (module.consentGated &&
            (decision?.state != IssuedCollectionDecisionState.ACCEPTED ||
                !moduleDecisionSurvives(module, decision.settingsVersion, latestSettings, issuedRevisions)))
    }
}

/** Required modules whose explicit decline still stands: any one halts the whole study. */
public fun standingRequiredDeclines(
    latestSettings: AndroidDataCollectionSetting,
    issuedRevisions: List<AndroidDataCollectionSetting>,
    decisions: Map<CollectionModuleId, IssuedCollectionDecision>,
): Set<CollectionModuleId> = latestSettings.effectiveModules().filter { (module, policy) ->
    val decision = decisions[module]
    module.consentGated && requiredDecisionHaltsCollection(policy.enabled, policy.required,
        decision?.state == IssuedCollectionDecisionState.DECLINED) &&
        requiredDeclineStands(module, checkNotNull(decision).settingsVersion, latestSettings, issuedRevisions)
}.keys

/** The participant's standing for the participants page, read by the same rules that admit uploads. */
public fun participantConsentState(
    latestSettings: AndroidDataCollectionSetting,
    issuedRevisions: List<AndroidDataCollectionSetting>,
    decisions: Map<CollectionModuleId, IssuedCollectionDecision>,
): ParticipantConsentState {
    val declined = standingRequiredDeclines(latestSettings, issuedRevisions, decisions)
    val awaiting = latestSettings.effectiveModules().filter { (module, policy) ->
        val decision = decisions[module]
        module.consentGated && policy.enabled && module !in declined &&
            (decision == null || !moduleDecisionSurvives(module, decision.settingsVersion, latestSettings, issuedRevisions))
    }.keys
    return ParticipantConsentState(declined, awaiting)
}

/** The feature switch narrows collection without rewriting the researcher's stored choices. */
public fun collectionSettingsForFeatures(settings: StudySettings, features: Set<String>): StudySettings {
    val collection = settings[StudySettingType.DataCollection] as? AndroidDataCollectionSetting ?: return settings
    val collecting = StudyFeature.CHRONICLE_DATA_COLLECTION.name in features
    val complete = collection.copy(modules = collection.effectiveModules(CollectionModuleId.activeModules).mapValues { (id, setting) ->
        if (collecting || id == CollectionModuleId.UPLOAD_TELEMETRY) setting else setting.copy(enabled = false)
    })
    return StudySettings(settings + (StudySettingType.DataCollection to complete))
}

/** Researcher input and stored policy never supply read-side consent evidence. */
public fun AndroidDataCollectionSetting.withoutConsentEpochs(): AndroidDataCollectionSetting =
    copy(modules = modules.mapValues { (_, module) ->
        module.copy(consentEpoch = AndroidDataCollectionSetting.INITIAL_SETTINGS_VERSION)
    })
