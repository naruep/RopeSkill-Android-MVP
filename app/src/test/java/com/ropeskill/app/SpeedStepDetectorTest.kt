package com.ropeskill.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeedStepDetectorTest {
    @Test
    fun firstRightLanding_counts() {
        val detector = startedDetector()

        val result = detector.processLanding(SpeedLanding.RIGHT, 1_100L)

        assertTrue(result.countedRightStep)
        assertEquals(1, result.count)
        assertEquals(SpeedAlternationState.NEED_LEFT, result.stateAfter)
    }

    @Test
    fun alternatingLandings_countEveryRight() {
        val detector = startedDetector()

        val counts = listOf(
            detector.processLanding(SpeedLanding.RIGHT, 1_100L),
            detector.processLanding(SpeedLanding.LEFT, 1_200L),
            detector.processLanding(SpeedLanding.RIGHT, 1_300L),
        )

        assertEquals(2, counts.last().count)
        assertEquals(listOf(1_100L, 1_300L), detector.diagnostics().countedRightTimestampsMillis)
    }

    @Test
    fun repeatedRight_doesNotCountUntilLeftUnlocks() {
        val detector = startedDetector()

        detector.processLanding(SpeedLanding.RIGHT, 1_100L)
        val repeated = detector.processLanding(SpeedLanding.RIGHT, 1_200L)
        detector.processLanding(SpeedLanding.LEFT, 1_300L)
        val unlocked = detector.processLanding(SpeedLanding.RIGHT, 1_400L)

        assertFalse(repeated.countedRightStep)
        assertEquals(SpeedRejectReason.REPEATED_RIGHT, repeated.rejectReason)
        assertEquals(2, unlocked.count)
        assertEquals(1, detector.diagnostics().repeatedRightRejects)
    }

    @Test
    fun repeatedLeft_keepsRightEligible() {
        val detector = startedDetector()

        detector.processLanding(SpeedLanding.LEFT, 1_100L)
        val repeated = detector.processLanding(SpeedLanding.LEFT, 1_200L)
        val right = detector.processLanding(SpeedLanding.RIGHT, 1_300L)

        assertEquals(SpeedRejectReason.REPEATED_LEFT, repeated.rejectReason)
        assertTrue(right.countedRightStep)
    }

    @Test
    fun bothFeet_doesNotUnlockRepeatedRight() {
        val detector = startedDetector()

        detector.processLanding(SpeedLanding.RIGHT, 1_100L)
        detector.processLanding(SpeedLanding.BOTH, 1_200L)
        val result = detector.processLanding(SpeedLanding.RIGHT, 1_300L)

        assertEquals(1, result.count)
        assertEquals(SpeedRejectReason.REPEATED_RIGHT, result.rejectReason)
        assertEquals(1, detector.diagnostics().bothFeetRejects)
    }

    @Test
    fun unclearLanding_doesNotUnlockRepeatedRight() {
        val detector = startedDetector()

        detector.processLanding(SpeedLanding.RIGHT, 1_100L)
        detector.processLanding(SpeedLanding.UNCLEAR, 1_200L)
        val result = detector.processLanding(SpeedLanding.RIGHT, 1_300L)

        assertEquals(1, result.count)
        assertEquals(SpeedRejectReason.REPEATED_RIGHT, result.rejectReason)
        assertEquals(1, detector.diagnostics().unclearLandingRejects)
    }

    @Test
    fun trackingLoss_doesNotUnlockRepeatedRight() {
        val detector = startedDetector()

        detector.processLanding(SpeedLanding.RIGHT, 1_100L)
        detector.onTrackingLost(1_150L)
        detector.onTrackingRestored(1_350L)
        val result = detector.processLanding(SpeedLanding.RIGHT, 1_400L)

        assertEquals(1, result.count)
        assertEquals(SpeedRejectReason.REPEATED_RIGHT, result.rejectReason)
        assertEquals(1, detector.diagnostics().trackingLossEvents)
        assertEquals(200L, detector.diagnostics().trackingLossDurationMillis)
    }

    @Test
    fun landingAtGo_isIncluded() {
        val detector = startedDetector()

        assertTrue(detector.processLanding(SpeedLanding.RIGHT, GO).countedRightStep)
    }

    @Test
    fun landingAtEndBoundary_isExcluded() {
        val detector = startedDetector()

        val result = detector.processLanding(SpeedLanding.RIGHT, GO + 30_000L)

        assertFalse(result.countedRightStep)
        assertEquals(SpeedRejectReason.AFTER_TIME_LIMIT, result.rejectReason)
    }

    @Test
    fun landingBeforeGo_isExcluded() {
        val detector = startedDetector()

        val result = detector.processLanding(SpeedLanding.RIGHT, GO - 1L)

        assertEquals(0, result.count)
        assertEquals(SpeedRejectReason.BEFORE_GO, result.rejectReason)
    }

    @Test
    fun outOfOrderTimestamp_isRejectedWithoutChangingGate() {
        val detector = startedDetector()

        detector.processLanding(SpeedLanding.RIGHT, 1_200L)
        val outOfOrder = detector.processLanding(SpeedLanding.LEFT, 1_100L)
        val repeatedRight = detector.processLanding(SpeedLanding.RIGHT, 1_300L)

        assertEquals(SpeedRejectReason.OUT_OF_ORDER_TIMESTAMP, outOfOrder.rejectReason)
        assertEquals(SpeedRejectReason.REPEATED_RIGHT, repeatedRight.rejectReason)
        assertEquals(1, detector.diagnostics().outOfOrderTimestampRejects)
    }

    @Test
    fun reset_clearsCountGateAndDiagnostics() {
        val detector = startedDetector()
        detector.processLanding(SpeedLanding.RIGHT, 1_100L)

        detector.start(5_000L)
        val result = detector.processLanding(SpeedLanding.RIGHT, 5_100L)

        assertEquals(1, result.count)
        assertEquals(listOf(5_100L), detector.diagnostics().rightLandingTimestampsMillis)
    }

    @Test
    fun processingBeforeStart_isSafe() {
        val detector = SpeedStepDetector()

        val result = detector.processLanding(SpeedLanding.RIGHT, 1_000L)

        assertEquals(0, result.count)
        assertEquals(SpeedRejectReason.NOT_STARTED, result.rejectReason)
    }

    private fun startedDetector() = SpeedStepDetector().also { it.start(GO) }

    companion object {
        private const val GO = 1_000L
    }
}
