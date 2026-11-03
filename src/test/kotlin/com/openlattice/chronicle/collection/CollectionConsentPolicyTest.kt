package com.openlattice.chronicle.collection

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class CollectionConsentPolicyTest {
    @Test fun exhaustiveConsentChanges() {
        val rows = TestMappers.json().readTree(File("fixtures/collection-consent.json"))
        val module = CollectionModuleId.BATTERY_TELEMETRY
        rows.forEach { row ->
            val revisions = row["revisions"].map {
                TestMappers.json().treeToValue(it, AndroidDataCollectionSetting::class.java)
            }
            val decision = IssuedCollectionDecisionState.entries.singleOrNull { it.name == row["decision"].asText() }
            val policySurvives = decision != null && moduleDecisionSurvives(module, 1, revisions.last(), revisions)
            val declineStands = decision == IssuedCollectionDecisionState.DECLINED &&
                requiredDeclineStands(module, 1, revisions.last(), revisions)
            assertEquals(row["name"].asText(), row["survives"].asBoolean(), policySurvives || declineStands)
            val initial = revisions.first().effectiveModules().getValue(module)
            val latest = revisions.last().withConsentEpochs(revisions).effectiveModules().getValue(module)
            assertEquals(row["name"].asText(), policySurvives,
                decision != null && moduleDecisionSurvives(latest, 1, initial.enabled))
            assertEquals(row["name"].asText(), declineStands,
                decision == IssuedCollectionDecisionState.DECLINED && requiredDeclineStands(initial.enabled, latest))
            assertEquals(row["name"].asText(), row["halted"].asBoolean(), collectionIsHalted(revisions.last(), revisions,
                decision?.let { mapOf(module to IssuedCollectionDecision(it, 1)) }.orEmpty()))
        }
    }

    /** Proof is needed to extend an acceptance, not to keep a refusal: an unprovable ledger keeps the decline. */
    @Test fun requiredDeclineStandsWhenTheLedgerCannotProveADisable() {
        val module = CollectionModuleId.BATTERY_TELEMETRY
        val v1 = AndroidDataCollectionSetting(mapOf(module to CollectionModuleSetting(enabled = true, required = true)), settingsVersion = 1)
        val v3 = v1.copy(settingsVersion = 3)
        val declined = mapOf(module to IssuedCollectionDecision(IssuedCollectionDecisionState.DECLINED, 1))
        assertEquals("gapped ledger", true, collectionIsHalted(v3, listOf(v1, v3), declined))
        assertEquals("latest absent from the ledger", true, collectionIsHalted(v3, listOf(v1), declined))
        val disabled = v3.copy(modules = mapOf(module to CollectionModuleSetting(enabled = false, required = true)))
        assertEquals("latest disabled", false, collectionIsHalted(disabled, listOf(v1), declined))
    }

    @Test fun ledgerBehindTheCurrentSettingsForcesReconsentInsteadOfFailing() {
        val module = CollectionModuleId.BATTERY_TELEMETRY
        val issued = AndroidDataCollectionSetting(mapOf(module to CollectionModuleSetting(enabled = true)), settingsVersion = 1)
        val current = issued.copy(modules = mapOf(module to CollectionModuleSetting(enabled = true,
            collectionCadence = CollectionCadence(60))), settingsVersion = 2)
        listOf(listOf(issued), listOf(issued, issued.copy(settingsVersion = 2))).forEach { ledger ->
            assertEquals(2, current.withConsentEpochs(ledger).effectiveModules().getValue(module).consentEpoch)
        }
        assertEquals(1, issued.withConsentEpochs(listOf(issued)).effectiveModules().getValue(module).consentEpoch)
    }
}
