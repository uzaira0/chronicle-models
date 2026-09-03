package com.openlattice.chronicle.collection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test
import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * Model unit tests for the [AndroidInteractionEvent] wire contract
 * (see `docs/SENSING-EXPANSION-DESIGN.md` §6).
 */
class AndroidInteractionEventTest {

    private fun event(
        eventType: InteractionEventType = InteractionEventType.CLICK,
        gridRows: Int = 4,
        gridCols: Int = 3,
        gridRow: Int = 1,
        gridCol: Int = 2,
        positionSource: InteractionPositionSource? = null,
        nodeBoundsLeft: Int? = null,
        nodeBoundsTop: Int? = null,
        nodeBoundsRight: Int? = null,
        nodeBoundsBottom: Int? = null,
        displayId: Int? = null,
        rawX: Int? = 540,
        rawY: Int? = 480,
        screenWidth: Int? = 1080,
        screenHeight: Int? = 1920,
        normalizedX: Double? = 0.5,
        normalizedY: Double? = 0.25,
        scrollDeltaX: Int? = null,
        scrollDeltaY: Int? = null,
        eventTimeMillis: Long? = 1_000L,
        episodeId: String? = "ep-0",
        dwellMillisSincePrev: Long? = 0L,
        orientation: Int? = 0,
        screenDensityDpi: Int? = 320,
        scrollVelocityX: Double? = null,
        scrollVelocityY: Double? = null,
        scrollReversed: Boolean? = null,
    ): AndroidInteractionEvent = AndroidInteractionEvent(
        id = "interaction-event-1",
        timestamp = OffsetDateTime.of(2026, 6, 18, 12, 0, 0, 0, ZoneOffset.UTC),
        timezone = "UTC",
        eventType = eventType,
        gridRows = gridRows,
        gridCols = gridCols,
        gridRow = gridRow,
        gridCol = gridCol,
        elementRole = "android.widget.Button",
        foregroundPackage = "com.example.app",
        positionSource = positionSource,
        nodeBoundsLeft = nodeBoundsLeft,
        nodeBoundsTop = nodeBoundsTop,
        nodeBoundsRight = nodeBoundsRight,
        nodeBoundsBottom = nodeBoundsBottom,
        displayId = displayId,
        rawX = rawX,
        rawY = rawY,
        screenWidth = screenWidth,
        screenHeight = screenHeight,
        normalizedX = normalizedX,
        normalizedY = normalizedY,
        scrollDeltaX = scrollDeltaX,
        scrollDeltaY = scrollDeltaY,
        eventTimeMillis = eventTimeMillis,
        episodeId = episodeId,
        dwellMillisSincePrev = dwellMillisSincePrev,
        orientation = orientation,
        screenDensityDpi = screenDensityDpi,
        scrollVelocityX = scrollVelocityX,
        scrollVelocityY = scrollVelocityY,
        scrollReversed = scrollReversed,
    )

    @Test fun testValidEventConstructs() {
        val e = event()
        assertEquals(InteractionEventType.CLICK, e.eventType)
        assertEquals(1, e.gridRow)
        assertEquals(2, e.gridCol)
    }

    @Test fun testCarriesExactPositionBundle() {
        val e = event(rawX = 521, rawY = 1034, screenWidth = 1080, screenHeight = 1920, normalizedX = 0.4827, normalizedY = 0.5391)
        assertEquals(521, e.rawX)
        assertEquals(1034, e.rawY)
        assertEquals(1080, e.screenWidth)
        assertEquals(1920, e.screenHeight)
        assertEquals(0.4827, e.normalizedX!!, 1e-9)
        assertEquals(0.5391, e.normalizedY!!, 1e-9)
    }

    @Test fun testCarriesRawNodeBoundsProvenanceIncludingSignedEdges() {
        val e = event(
            positionSource = InteractionPositionSource.ACCESSIBILITY_NODE_BOUNDS,
            nodeBoundsLeft = -20,
            nodeBoundsTop = -10,
            nodeBoundsRight = 200,
            nodeBoundsBottom = 100,
            displayId = 0,
        )
        assertEquals(InteractionPositionSource.ACCESSIBILITY_NODE_BOUNDS, e.positionSource)
        assertEquals(-20, e.nodeBoundsLeft)
        assertEquals(-10, e.nodeBoundsTop)
        assertEquals(200, e.nodeBoundsRight)
        assertEquals(100, e.nodeBoundsBottom)
        assertEquals(0, e.displayId)
    }

    @Test fun testPartialNodeBoundsRejected() {
        try {
            event(
                positionSource = InteractionPositionSource.ACCESSIBILITY_NODE_BOUNDS,
                nodeBoundsLeft = 1,
            )
            fail("Expected rejection of partial node bounds")
        } catch (e: IllegalArgumentException) { /* expected */ }
    }

    @Test fun testRawNodeBoundsRequireDisplayContext() {
        try {
            event(
                positionSource = InteractionPositionSource.ACCESSIBILITY_NODE_BOUNDS,
                nodeBoundsLeft = 1,
                nodeBoundsTop = 2,
                nodeBoundsRight = 3,
                nodeBoundsBottom = 4,
                displayId = null,
            )
            fail("Expected rejection of position without display id")
        } catch (e: IllegalArgumentException) { /* expected */ }
    }

    @Test fun testExactPositionOmittedInCoarseMode() {
        val e = event(rawX = null, rawY = null, screenWidth = null, screenHeight = null, normalizedX = null, normalizedY = null)
        assertNull(e.rawX)
        assertNull(e.rawY)
        assertNull(e.screenWidth)
        assertNull(e.screenHeight)
        assertNull(e.normalizedX)
        assertNull(e.normalizedY)
    }

    @Test fun testNegativeRawCoordinateRejected() {
        try {
            event(rawX = -1)
            fail("Expected rejection of negative rawX")
        } catch (e: IllegalArgumentException) { /* expected */ }
    }

    @Test fun testZeroScreenWidthRejected() {
        try {
            event(screenWidth = 0)
            fail("Expected rejection of zero screenWidth")
        } catch (e: IllegalArgumentException) { /* expected */ }
    }

    @Test fun testNormalizedXOutOfRangeRejected() {
        try {
            event(normalizedX = 1.5)
            fail("Expected rejection of normalizedX > 1")
        } catch (e: IllegalArgumentException) { /* expected */ }
    }

    @Test fun testNormalizedYNegativeRejected() {
        try {
            event(normalizedY = -0.01)
            fail("Expected rejection of negative normalizedY")
        } catch (e: IllegalArgumentException) { /* expected */ }
    }

    @Test fun testScrollEventCarriesSignedDeltas() {
        val e = event(eventType = InteractionEventType.SCROLL, scrollDeltaX = -40, scrollDeltaY = 120)
        assertEquals(-40, e.scrollDeltaX)
        assertEquals(120, e.scrollDeltaY)
    }

    @Test fun testClickEventOmitsScrollDeltas() {
        assertNull(event().scrollDeltaX)
        assertNull(event().scrollDeltaY)
    }

    @Test fun testZeroGridRowsRejected() {
        try {
            event(gridRows = 0, gridRow = 0)
            fail("Expected rejection of zero gridRows")
        } catch (e: IllegalArgumentException) { /* expected */ }
    }

    @Test fun testZeroGridColsRejected() {
        try {
            event(gridCols = 0, gridCol = 0)
            fail("Expected rejection of zero gridCols")
        } catch (e: IllegalArgumentException) { /* expected */ }
    }

    @Test fun testGridRowOutOfBoundsRejected() {
        try {
            event(gridRows = 4, gridRow = 4)
            fail("Expected rejection of gridRow == gridRows")
        } catch (e: IllegalArgumentException) { /* expected */ }
    }

    @Test fun testGridColOutOfBoundsRejected() {
        try {
            event(gridCols = 3, gridCol = 3)
            fail("Expected rejection of gridCol == gridCols")
        } catch (e: IllegalArgumentException) { /* expected */ }
    }

    @Test fun testNegativeGridCellRejected() {
        try {
            event(gridRow = -1)
            fail("Expected rejection of negative gridRow")
        } catch (e: IllegalArgumentException) { /* expected */ }
    }

    @Test fun testEventTypeCardinality() {
        // CLICK, LONG_CLICK, SCROLL, FOCUS, ACCESSIBILITY_FOCUS, SELECT.
        assertEquals(6, InteractionEventType.entries.size)
    }

    @Test fun testFocusAndSelectEventKinds() {
        assertEquals(InteractionEventType.FOCUS, event(eventType = InteractionEventType.FOCUS).eventType)
        assertEquals(InteractionEventType.ACCESSIBILITY_FOCUS, event(eventType = InteractionEventType.ACCESSIBILITY_FOCUS).eventType)
        assertEquals(InteractionEventType.SELECT, event(eventType = InteractionEventType.SELECT).eventType)
    }

    @Test fun testCarriesKinematicsAndContext() {
        val e = event(
            eventType = InteractionEventType.SCROLL,
            eventTimeMillis = 123_456_789L,
            episodeId = "ep-1",
            dwellMillisSincePrev = 420L,
            orientation = 1,
            screenDensityDpi = 420,
            scrollVelocityX = -12.5,
            scrollVelocityY = 880.0,
            scrollReversed = true,
        )
        assertEquals(123_456_789L, e.eventTimeMillis)
        assertEquals("ep-1", e.episodeId)
        assertEquals(420L, e.dwellMillisSincePrev)
        assertEquals(1, e.orientation)
        assertEquals(420, e.screenDensityDpi)
        assertEquals(880.0, e.scrollVelocityY!!, 1e-9)
        assertEquals(true, e.scrollReversed)
    }

    @Test fun testOrientationOutOfRangeRejected() {
        try {
            event(orientation = 4)
            fail("Expected rejection of orientation outside 0..3")
        } catch (e: IllegalArgumentException) { /* expected */ }
    }

    @Test fun testNegativeDwellRejected() {
        try {
            event(dwellMillisSincePrev = -1)
            fail("Expected rejection of negative dwell")
        } catch (e: IllegalArgumentException) { /* expected */ }
    }
}
