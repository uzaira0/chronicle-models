package com.openlattice.chronicle.android

import org.junit.Assert.assertEquals
import org.junit.Test

class ChronicleUsageEventTest {

    @Test
    fun originalScreenNonInteractiveLabelKeepsCanonicalNumericType() {
        assertEquals(
            ChronicleUsageEventType.SCREEN_NON_INTERACTIVE.value,
            fromInteractionType("Screen Non-interactive"),
        )
    }

    @Test
    fun temporaryCapitalizationRemainsReadableForExistingRows() {
        assertEquals(
            ChronicleUsageEventType.SCREEN_NON_INTERACTIVE.value,
            fromInteractionType("Screen Non-Interactive"),
        )
    }
}
