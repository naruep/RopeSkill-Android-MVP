package com.ropeskill.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class T735PassiveTakeoffGateTraceTest {
    @Test
    fun matchedAndAirborneUnmatchedPulses_areSeparatedWithoutChangingResult() {
        val collector = T735PassiveTakeoffGateCollector(enabled = true)
        collector.startMeasurement(timestampMillis = 1_000L)

        collector.record(frame(0.40f, 0.80f), result(), 1_000L)
        collector.record(
            frame(0.37f, 0.77f),
            result(
                event = BounceEvent.TAKEOFF,
                status = BounceTrackingStatus.AIRBORNE,
            ),
            1_033L,
        )
        collector.record(
            frame(0.34f, 0.74f),
            result(status = BounceTrackingStatus.AIRBORNE),
            1_066L,
        )
        collector.record(
            frame(0.36f, 0.76f),
            result(status = BounceTrackingStatus.AIRBORNE),
            1_099L,
        )

        collector.record(
            frame(0.32f, 0.72f),
            result(status = BounceTrackingStatus.AIRBORNE),
            1_132L,
        )
        collector.record(
            frame(0.29f, 0.69f),
            result(status = BounceTrackingStatus.AIRBORNE),
            1_165L,
        )
        collector.record(
            frame(0.31f, 0.71f),
            result(status = BounceTrackingStatus.AIRBORNE),
            1_198L,
        )
        val snapshot = requireNotNull(
            collector.record(
                frame(0.31f, 0.71f),
                result(status = BounceTrackingStatus.AIRBORNE),
                1_330L,
            ),
        )

        assertEquals(2, snapshot.rawPulseCount)
        assertEquals(2, snapshot.qualifiedPulseCount)
        assertEquals(1, snapshot.matchedPulseCount)
        assertEquals(1, snapshot.unmatchedPulseCount)
        assertEquals(1, snapshot.unmatchedWhileAirborneCount)
        assertEquals(1, snapshot.productionTakeoffCount)
        assertEquals(0, snapshot.productionLandingCount)
        assertEquals("QM", snapshot.latestPulses[0].marker())
        assertEquals("QU", snapshot.latestPulses[1].marker())
    }

    @Test
    fun lowAmplitudeJitter_isRetainedAsRawButNotQualified() {
        val collector = T735PassiveTakeoffGateCollector(enabled = true)
        collector.startMeasurement(timestampMillis = 2_000L)
        collector.record(frame(0.40f, 0.80f), result(), 2_000L)
        collector.record(frame(0.399f, 0.799f), result(), 2_033L)
        val snapshot = requireNotNull(
            collector.record(frame(0.400f, 0.800f), result(), 2_066L),
        )

        assertEquals(1, snapshot.rawPulseCount)
        assertEquals(0, snapshot.qualifiedPulseCount)
        assertEquals(0, snapshot.unmatchedPulseCount)
        assertEquals("rU", snapshot.latestPulses.single().marker())
    }

    @Test
    fun unmatchedPulse_withRejectedRescuePeak_reportsExactBlockingGates() {
        val collector = T735PassiveTakeoffGateCollector(enabled = true)
        collector.startMeasurement(timestampMillis = 2_500L)
        collector.record(frame(0.40f, 0.80f), result(), 2_500L)
        collector.record(frame(0.36f, 0.76f), result(), 2_533L)
        val snapshot = requireNotNull(
            collector.record(
                frame(0.38f, 0.78f),
                result(
                    peakEvidence = rejectedPeak(
                        smoothedAnkle = 0.015f,
                        rawAnkle = 0.025f,
                        rawLeft = 0.023f,
                        rawRight = 0.017f,
                        smoothedHip = 0.119f,
                        leftPassed = true,
                        rightPassed = true,
                    ),
                ),
                2_566L,
            ),
        )

        assertEquals(1, snapshot.attributedUnmatchedCount)
        assertEquals(0, snapshot.noProductionPeakCount)
        assertEquals(1, snapshot.blockingGateCounts[T735BlockingGate.RESCUE_ANKLE_RISE])
        assertEquals(
            setOf(T735BlockingGate.RESCUE_ANKLE_RISE),
            snapshot.latestPulses.single().gateAttribution?.blockingGates,
        )
    }

    @Test
    fun rejectedPeakOneFrameAfterRawPulse_isStillMatched() {
        val collector = T735PassiveTakeoffGateCollector(enabled = true)
        collector.startMeasurement(timestampMillis = 2_600L)
        collector.record(frame(0.40f, 0.80f), result(), 2_600L)
        collector.record(frame(0.36f, 0.76f), result(), 2_633L)
        collector.record(frame(0.38f, 0.78f), result(), 2_666L)
        val snapshot = requireNotNull(
            collector.record(
                frame(0.38f, 0.78f),
                result(
                    peakEvidence = rejectedPeak(
                        smoothedAnkle = 0.019f,
                        rawAnkle = 0.025f,
                        rawLeft = 0.023f,
                        rawRight = 0.017f,
                        smoothedHip = 0.119f,
                        leftPassed = true,
                        rightPassed = true,
                    ),
                ),
                2_699L,
            ),
        )

        assertEquals(1, snapshot.attributedUnmatchedCount)
        assertEquals(0, snapshot.noProductionPeakCount)
        assertEquals(0, snapshot.pendingEvidenceCount)
    }

    @Test
    fun unmatchedPulse_withRejectedStandardPeak_reportsAllFailedProductionGates() {
        val collector = T735PassiveTakeoffGateCollector(enabled = true)
        collector.startMeasurement(timestampMillis = 2_700L)
        collector.record(frame(0.40f, 0.80f), result(), 2_700L)
        collector.record(frame(0.36f, 0.76f), result(), 2_733L)
        val snapshot = requireNotNull(
            collector.record(
                frame(0.38f, 0.78f),
                result(
                    peakEvidence = rejectedPeak(
                        smoothedAnkle = 0.050f,
                        rawAnkle = 0.080f,
                        rawLeft = 0.008f,
                        rawRight = 0.050f,
                        smoothedHip = 0.050f,
                        leftPassed = false,
                        rightPassed = true,
                        diagnostic = BounceDiagnostic.FEET_NOT_SYNCHRONIZED,
                    ),
                ),
                2_766L,
            ),
        )

        assertEquals(
            setOf(
                T735BlockingGate.FEET_SYNCHRONIZED,
                T735BlockingGate.LEFT_BILATERAL_ANKLE,
                T735BlockingGate.HIP_TO_ANKLE_RATIO,
                T735BlockingGate.STANDARD_HIP_RISE,
            ),
            snapshot.latestPulses.single().gateAttribution?.blockingGates,
        )
    }

    @Test
    fun missingLandmarks_interruptActivePulse_andResetDirectionAnchor() {
        val collector = T735PassiveTakeoffGateCollector(enabled = true)
        collector.startMeasurement(timestampMillis = 3_000L)
        collector.record(frame(0.40f, 0.80f), result(), 3_000L)
        collector.record(frame(0.35f, 0.75f), result(), 3_033L)

        val snapshot = requireNotNull(
            collector.record(PoseFrame.Empty, result(), 3_066L),
        )

        assertEquals(1, snapshot.interruptedPulseCount)
        assertEquals(0, snapshot.rawPulseCount)
    }

    @Test
    fun disabledCollector_neverPublishes() {
        val collector = T735PassiveTakeoffGateCollector(enabled = false)

        assertNull(collector.startMeasurement(4_000L))
        assertNull(collector.record(frame(0.40f, 0.80f), result(), 4_000L))
    }

    @Test
    fun formatter_exposesTraceTotalsAndRecentRows() {
        val text = formatT735TakeoffGateSnapshot(
            T735TakeoffGateSnapshot(
                measurementStarted = true,
                rawPulseCount = 22,
                qualifiedPulseCount = 22,
                matchedPulseCount = 21,
                unmatchedPulseCount = 1,
                unmatchedWhileAirborneCount = 1,
                attributedUnmatchedCount = 0,
                noProductionPeakCount = 1,
                pendingEvidenceCount = 0,
                blockingGateCounts = emptyMap(),
                productionTakeoffCount = 21,
                productionLandingCount = 21,
                interruptedPulseCount = 0,
                latestPulses = listOf(
                    T735MotionPulse(
                        sequence = 12,
                        elapsedMillis = 6_250L,
                        durationMillis = 132L,
                        ankleRiseRatio = 0.032f,
                        hipRiseRatio = 0.118f,
                        qualified = true,
                        matchedProductionTakeoff = false,
                        peakTrackingStatus = BounceTrackingStatus.AIRBORNE,
                        gateAttribution = null,
                    ),
                ),
                processedFrames = 720L,
            ),
        )

        assertTrue(text.contains("T-736 TUNED GATE V20 MATCHED"))
        assertTrue(text.contains("RAW22 Q22 M21 U1 UA1 G0 NP1 P0 T/L21/21 X0 F720"))
        assertTrue(text.contains("G SY0 BL0 BR0 Q0 SH0 RA0 RH0"))
        assertTrue(text.contains("#12 +6.250 QU A0.032 H0.118 D132 AIR NP"))
    }

    private fun T735MotionPulse.marker(): String =
        (if (qualified) "Q" else "r") +
            if (matchedProductionTakeoff) "M" else "U"

    private fun result(
        event: BounceEvent = BounceEvent.NONE,
        status: BounceTrackingStatus = BounceTrackingStatus.READY,
        peakEvidence: TakeoffPeakEvidence? = null,
    ): BounceDetectionResult =
        BounceDetectionResult(
            countedJump = false,
            trackingStatus = status,
            event = event,
            diagnostic = if (status == BounceTrackingStatus.AIRBORNE) {
                BounceDiagnostic.AIRBORNE
            } else {
                BounceDiagnostic.READY
            },
            takeoffPeakEvidence = peakEvidence,
        )

    private fun rejectedPeak(
        smoothedAnkle: Float,
        rawAnkle: Float,
        rawLeft: Float,
        rawRight: Float,
        smoothedHip: Float,
        leftPassed: Boolean,
        rightPassed: Boolean,
        diagnostic: BounceDiagnostic = BounceDiagnostic.ANKLE_RISE_TOO_SMALL,
    ): TakeoffPeakEvidence =
        TakeoffPeakEvidence(
            outcome = TakeoffPeakOutcome.REJECTED,
            smoothedAnkleRiseRatio = smoothedAnkle,
            rawAnkleRiseRatio = rawAnkle,
            rawLeftAnkleRiseRatio = rawLeft,
            rawRightAnkleRiseRatio = rawRight,
            individualAnkleRiseThreshold = 0.010f,
            leftIndividualAnkleGatePassed = leftPassed,
            rightIndividualAnkleGatePassed = rightPassed,
            smoothedHipRiseRatio = smoothedHip,
            rawHipRiseRatio = smoothedHip,
            riseFrameCount = 3,
            riseMillis = 66L,
            peakFrameIntervalMillis = 33L,
            nextFrameIntervalMillis = 33L,
            diagnostic = diagnostic,
        )

    private fun frame(
        hipY: Float,
        ankleY: Float,
    ): PoseFrame {
        val points = MutableList(33) {
            NormalizedPoint(x = 0.5f, y = 0.5f, isVisible = false)
        }
        points[23] = NormalizedPoint(x = 0.45f, y = hipY, isVisible = true)
        points[24] = NormalizedPoint(x = 0.55f, y = hipY, isVisible = true)
        points[27] = NormalizedPoint(x = 0.45f, y = ankleY, isVisible = true)
        points[28] = NormalizedPoint(x = 0.55f, y = ankleY, isVisible = true)
        return PoseFrame(points, 1_080, 1_920)
    }
}
