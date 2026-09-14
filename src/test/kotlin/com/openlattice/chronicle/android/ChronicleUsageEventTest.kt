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
    fun hiddenNotificationEventTypesRoundTripThroughTheirLabels() {
        assertEquals(10, fromInteractionType("Notification Seen"))
        assertEquals(12, fromInteractionType("Notification Interruption"))
        // Rows uploaded by clients before the labels existed keep their numeric type.
        assertEquals(10, fromInteractionType("Unknown importance: 10"))
    }

    @Test
    fun temporaryCapitalizationRemainsReadableForExistingRows() {
        assertEquals(
            ChronicleUsageEventType.SCREEN_NON_INTERACTIVE.value,
            fromInteractionType("Screen Non-Interactive"),
        )
    }
}
