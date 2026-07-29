package com.ropeskill.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class T730PassiveGateAttributionTest {
    @Test
    fun detectorNearRescueBoundary_isAttributedWithoutChangingDetectorResult() {
        val detector = BasicBounceDetector()
        val collector = T730PassiveGateAttributionCollector(enabled = true)
        calibrate(detector)
        collector.startMeasurement()

        collector.record(
            detector.process(
                standingFrame(),
                timestampMillis = 1_900L,
            ),
        )
        val rise = detector.process(
            frame(hipY = 0.30f, leftAnkleY = 0.784f, rightAnkleY = 0.784f),
            timestampMillis = 2_000L,
        )
        collector.record(rise)
        val reversal = detector.process(
            standingFrame(),
            timestampMillis = 2_300L,
        )
        val snapshot = requireNotNull(collector.record(reversal))

        assertEquals(BounceEvent.NONE, rise.event)
        assertFalse(rise.countedJump)
        assertFalse(reversal.countedJump)
        assertEquals(1, snapshot.completedPeakCount)
        val attribution = snapshot.retainedRejectedPeaks.single()
        assertEquals(T730PeakRoute.STRONG_HIP_RESCUE, attribution.route)
        assertEquals(
            setOf(T730BlockingGate.RESCUE_ANKLE_RISE),
            attribution.blockingGates,
        )
        assertTrue(requireNotNull(attribution.gateEvaluation).leftBilateralAnklePassed)
        assertTrue(attribution.gateEvaluation.rightBilateralAnklePassed)
        assertFalse(attribution.gateEvaluation.rescueAnkleRisePassed)
        assertTrue(attribution.gateEvaluation.rescueHipRisePassed)
        assertTrue(attribution.gateEvaluation.hipToAnkleRatioPassed)
    }

    @Test
    fun detectorAsymmetricPeak_reportsLeftBilateralBlocker() {
        val detector = BasicBounceDetector()
        val collector = T730PassiveGateAttributionCollector(enabled = true)
        calibrate(detector)
        collector.startMeasurement()
        collector.record(detector.process(standingFrame(), timestampMillis = 1_900L))
        collector.record(
            detector.process(
                frame(hipY = 0.30f, leftAnkleY = 0.7961f, rightAnkleY = 0.765f),
                timestampMillis = 2_000L,
            ),
        )

        val snapshot = requireNotNull(
            collector.record(
                detector.process(standingFrame(), timestampMillis = 2_100L),
            ),
        )

        val attribution = snapshot.retainedRejectedPeaks.single()
        assertTrue(
            T730BlockingGate.LEFT_BILATERAL_ANKLE in attribution.blockingGates,
        )
        assertFalse(
            requireNotNull(attribution.gateEvaluation).leftBilateralAnklePassed,
        )
        assertTrue(attribution.gateEvaluation.rightBilateralAnklePassed)
    }

    @Test
    fun detectorLowHipAsymmetricPeak_preservesMultipleBlockers() {
        val detector = BasicBounceDetector()
        val collector = T730PassiveGateAttributionCollector(enabled = true)
        calibrate(detector)
        collector.startMeasurement()
        collector.record(detector.process(standingFrame(), timestampMillis = 1_900L))
        collector.record(
            detector.process(
                frame(hipY = 0.39f, leftAnkleY = 0.7961f, rightAnkleY = 0.765f),
                timestampMillis = 2_000L,
            ),
        )

        val snapshot = requireNotNull(
            collector.record(
                detector.process(standingFrame(), timestampMillis = 2_100L),
            ),
        )

        val blockers = snapshot.retainedRejectedPeaks.single().blockingGates
        assertTrue(T730BlockingGate.LEFT_BILATERAL_ANKLE in blockers)
        assertTrue(T730BlockingGate.HIP_TO_ANKLE_RATIO in blockers)
        assertTrue(T730BlockingGate.RESCUE_HIP_RISE in blockers)
        assertTrue(blockers.size >= 3)
    }

    @Test
    fun standardRoute_reportsEveryActiveBlockerFromExactEvidencePair() {
        val collector = startedCollector()

        val snapshot = requireNotNull(
            collector.record(
                rejectedResult(
                    ankleRise = 0.050f,
                    leftAnkleRise = 0.005f,
                    rightAnkleRise = 0.095f,
                    hipRise = 0.040f,
                    hipToAnkleRatio = 0.50f,
                    feetSynchronized = false,
                    diagnostic = BounceDiagnostic.FEET_NOT_SYNCHRONIZED,
                ),
            ),
        )

        val attribution = snapshot.retainedRejectedPeaks.single()
        assertEquals(T730PeakRoute.STANDARD, attribution.route)
        assertEquals(
            setOf(
                T730BlockingGate.FEET_SYNCHRONIZED,
                T730BlockingGate.LEFT_BILATERAL_ANKLE,
                T730BlockingGate.HIP_TO_ANKLE_RATIO,
                T730BlockingGate.STANDARD_HIP_RISE,
            ),
            attribution.blockingGates,
        )
        val evaluation = requireNotNull(attribution.gateEvaluation)
        assertTrue(evaluation.standardAnkleRisePassed)
        assertFalse(evaluation.standardPathPassed)
        assertFalse(evaluation.rescuePathPassed)
    }

    @Test
    fun rescueRoute_reportsHipAndRatioFailuresWithoutDiagnosticPrecedenceLoss() {
        val collector = startedCollector()

        val snapshot = requireNotNull(
            collector.record(
                rejectedResult(
                    ankleRise = 0.030f,
                    leftAnkleRise = 0.120f,
                    rightAnkleRise = 0.120f,
                    hipRise = 0.080f,
                    hipToAnkleRatio = 0.70f,
                    feetSynchronized = true,
                    diagnostic = BounceDiagnostic.ANKLE_RISE_TOO_SMALL,
                ),
            ),
        )

        val attribution = snapshot.retainedRejectedPeaks.single()
        assertEquals(T730PeakRoute.STRONG_HIP_RESCUE, attribution.route)
        assertEquals(
            setOf(
                T730BlockingGate.HIP_TO_ANKLE_RATIO,
                T730BlockingGate.RESCUE_HIP_RISE,
            ),
            attribution.blockingGates,
        )
        val evaluation = requireNotNull(attribution.gateEvaluation)
        assertFalse(evaluation.standardAnkleRisePassed)
        assertTrue(evaluation.standardHipRisePassed)
        assertTrue(evaluation.rescueAnkleRisePassed)
        assertFalse(evaluation.rescueHipRisePassed)
        assertFalse(evaluation.hipToAnkleRatioPassed)
    }

    @Test
    fun everyBlockingGate_canBeAttributedInIsolation() {
        data class Case(
            val expected: T730BlockingGate,
            val result: BounceDetectionResult,
        )

        val cases = listOf(
            Case(
                expected = T730BlockingGate.FEET_SYNCHRONIZED,
                result = rejectedResult(
                    ankleRise = 0.030f,
                    leftAnkleRise = 0.030f,
                    rightAnkleRise = 0.030f,
                    hipRise = 0.120f,
                    hipToAnkleRatio = 4.0f,
                    feetSynchronized = false,
                    diagnostic = BounceDiagnostic.FEET_NOT_SYNCHRONIZED,
                ),
            ),
            Case(
                expected = T730BlockingGate.LEFT_BILATERAL_ANKLE,
                result = rejectedResult(
                    ankleRise = 0.030f,
                    leftAnkleRise = 0.005f,
                    rightAnkleRise = 0.055f,
                    hipRise = 0.120f,
                    hipToAnkleRatio = 4.0f,
                    feetSynchronized = true,
                ),
            ),
            Case(
                expected = T730BlockingGate.RIGHT_BILATERAL_ANKLE,
                result = rejectedResult(
                    ankleRise = 0.030f,
                    leftAnkleRise = 0.055f,
                    rightAnkleRise = 0.005f,
                    hipRise = 0.120f,
                    hipToAnkleRatio = 4.0f,
                    feetSynchronized = true,
                ),
            ),
            Case(
                expected = T730BlockingGate.HIP_TO_ANKLE_RATIO,
                result = rejectedResult(
                    ankleRise = 0.030f,
                    leftAnkleRise = 0.142f,
                    rightAnkleRise = 0.142f,
                    hipRise = 0.120f,
                    hipToAnkleRatio = 0.849f,
                    feetSynchronized = true,
                ),
            ),
            Case(
                expected = T730BlockingGate.STANDARD_HIP_RISE,
                result = rejectedResult(
                    ankleRise = 0.050f,
                    leftAnkleRise = 0.050f,
                    rightAnkleRise = 0.050f,
                    hipRise = 0.059f,
                    hipToAnkleRatio = 1.18f,
                    feetSynchronized = true,
                    diagnostic = BounceDiagnostic.HIP_RISE_TOO_SMALL,
                ),
            ),
            Case(
                expected = T730BlockingGate.RESCUE_ANKLE_RISE,
                result = rejectedResult(
                    ankleRise = 0.019f,
                    leftAnkleRise = 0.019f,
                    rightAnkleRise = 0.019f,
                    hipRise = 0.120f,
                    hipToAnkleRatio = 6.0f,
                    feetSynchronized = true,
                ),
            ),
            Case(
                expected = T730BlockingGate.RESCUE_HIP_RISE,
                result = rejectedResult(
                    ankleRise = 0.030f,
                    leftAnkleRise = 0.030f,
                    rightAnkleRise = 0.030f,
                    hipRise = 0.099f,
                    hipToAnkleRatio = 3.3f,
                    feetSynchronized = true,
                ),
            ),
        )

        cases.forEach { case ->
            val snapshot = requireNotNull(startedCollector().record(case.result))
            assertEquals(
                case.expected,
                snapshot.retainedRejectedPeaks.single().blockingGates.single(),
            )
        }
    }

    @Test
    fun exactThresholds_areInclusiveForBothProductionPaths() {
        val standard = requireNotNull(
            startedCollector().record(
                rejectedResult(
                    ankleRise = 0.045f,
                    leftAnkleRise = 0.010f,
                    rightAnkleRise = 0.010f,
                    hipRise = 0.060f,
                    hipToAnkleRatio = 0.85f,
                    feetSynchronized = true,
                    diagnostic = BounceDiagnostic.READY,
                ),
            ),
        ).retainedRejectedPeaks.single()
        val rescue = requireNotNull(
            startedCollector().record(
                rejectedResult(
                    ankleRise = 0.020f,
                    leftAnkleRise = 0.010f,
                    rightAnkleRise = 0.010f,
                    hipRise = 0.100f,
                    hipToAnkleRatio = 0.85f,
                    feetSynchronized = true,
                    diagnostic = BounceDiagnostic.ANKLE_RISE_TOO_SMALL,
                ),
            ),
        ).retainedRejectedPeaks.single()

        assertTrue(requireNotNull(standard.gateEvaluation).standardPathPassed)
        assertTrue(requireNotNull(rescue.gateEvaluation).rescuePathPassed)
        assertEquals(
            setOf(T730BlockingGate.UNATTRIBUTED),
            standard.blockingGates,
        )
        assertEquals(
            setOf(T730BlockingGate.UNATTRIBUTED),
            rescue.blockingGates,
        )
    }

    @Test
    fun directHipToAnkleComparison_matchesProductionAtZeroNegativeAndBoundary() {
        val boundary = 0.100f * 0.85f
        data class Case(
            val rawAnkleRise: Float,
            val hipRise: Float,
            val expectedPassed: Boolean,
            val expectsDisplayRatio: Boolean,
        )

        val cases = listOf(
            Case(0f, 0f, expectedPassed = true, expectsDisplayRatio = false),
            Case(-0.010f, 0f, expectedPassed = true, expectsDisplayRatio = false),
            Case(0.100f, boundary, expectedPassed = true, expectsDisplayRatio = true),
            Case(
                0.100f,
                Math.nextDown(boundary),
                expectedPassed = false,
                expectsDisplayRatio = true,
            ),
        )

        cases.forEach { case ->
            val attribution = requireNotNull(
                startedCollector().record(
                    rejectedResult(
                        ankleRise = 0.050f,
                        leftAnkleRise = case.rawAnkleRise,
                        rightAnkleRise = case.rawAnkleRise,
                        hipRise = case.hipRise,
                        hipToAnkleRatio = 0f,
                        feetSynchronized = true,
                        diagnostic = BounceDiagnostic.HIP_RISE_TOO_SMALL,
                    ),
                ),
            ).retainedRejectedPeaks.single()

            assertEquals(
                case.expectedPassed,
                requireNotNull(attribution.gateEvaluation).hipToAnkleRatioPassed,
            )
            assertEquals(
                case.expectsDisplayRatio,
                attribution.hipToAnkleRiseRatio != null,
            )
        }
    }

    @Test
    fun peakIds_includeCountedRejectedAndSuppressedOutcomesInOrder() {
        val collector = startedCollector()

        collector.record(peakResult(TakeoffPeakOutcome.COUNTED))
        val firstRejected = requireNotNull(
            collector.record(
                rejectedResult(
                    ankleRise = 0.019f,
                    leftAnkleRise = 0.023f,
                    rightAnkleRise = 0.017f,
                    hipRise = 0.121f,
                    hipToAnkleRatio = 6.0f,
                    feetSynchronized = true,
                ),
            ),
        )
        collector.record(peakResult(TakeoffPeakOutcome.SUPPRESSED))
        val finalSnapshot = requireNotNull(
            collector.record(
                rejectedResult(
                    ankleRise = 0.050f,
                    leftAnkleRise = 0.007f,
                    rightAnkleRise = 0.051f,
                    hipRise = 0.006f,
                    hipToAnkleRatio = 0.12f,
                    feetSynchronized = true,
                ),
            ),
        )

        assertEquals(2, firstRejected.retainedRejectedPeaks.single().eventId)
        assertEquals(listOf(2, 4), finalSnapshot.retainedRejectedPeaks.map { it.eventId })
        assertEquals(4, finalSnapshot.completedPeakCount)
        assertEquals(1, finalSnapshot.countedPeakCount)
        assertEquals(2, finalSnapshot.rejectedPeakCount)
        assertEquals(1, finalSnapshot.suppressedPeakCount)
        assertEquals(
            finalSnapshot.completedPeakCount,
            finalSnapshot.countedPeakCount +
                finalSnapshot.rejectedPeakCount +
                finalSnapshot.suppressedPeakCount,
        )
    }

    @Test
    fun oneRejectedPeak_incrementsEveryOverlappingGateCounterOnce() {
        val collector = startedCollector()

        val snapshot = requireNotNull(
            collector.record(
                rejectedResult(
                    ankleRise = 0.015f,
                    leftAnkleRise = 0.005f,
                    rightAnkleRise = 0.004f,
                    hipRise = -0.010f,
                    hipToAnkleRatio = 0.30f,
                    feetSynchronized = false,
                    diagnostic = BounceDiagnostic.FEET_NOT_SYNCHRONIZED,
                ),
            ),
        )

        assertEquals(1, snapshot.rejectedPeakCount)
        assertEquals(
            1,
            snapshot.blockingGateCounts[T730BlockingGate.FEET_SYNCHRONIZED],
        )
        assertEquals(
            1,
            snapshot.blockingGateCounts[T730BlockingGate.LEFT_BILATERAL_ANKLE],
        )
        assertEquals(
            1,
            snapshot.blockingGateCounts[T730BlockingGate.RIGHT_BILATERAL_ANKLE],
        )
        assertEquals(
            1,
            snapshot.blockingGateCounts[T730BlockingGate.HIP_TO_ANKLE_RATIO],
        )
        assertEquals(
            1,
            snapshot.blockingGateCounts[T730BlockingGate.RESCUE_ANKLE_RISE],
        )
        assertEquals(
            1,
            snapshot.blockingGateCounts[T730BlockingGate.RESCUE_HIP_RISE],
        )
    }

    @Test
    fun missingOrMismatchedCompanionEvidence_isMarkedUnattributed() {
        val collector = startedCollector()

        val missing = requireNotNull(
            collector.record(
                peakResult(TakeoffPeakOutcome.REJECTED),
            ),
        )
        val mismatched = requireNotNull(
            collector.record(
                rejectedResult(
                    ankleRise = 0.019f,
                    leftAnkleRise = 0.020f,
                    rightAnkleRise = 0.020f,
                    hipRise = 0.120f,
                    hipToAnkleRatio = 6.0f,
                    feetSynchronized = true,
                    peakAnkleRise = 0.030f,
                ),
            ),
        )

        assertEquals(
            setOf(T730BlockingGate.UNATTRIBUTED),
            missing.retainedRejectedPeaks.single().blockingGates,
        )
        assertEquals(
            listOf(false, false),
            mismatched.retainedRejectedPeaks.map {
                it.evidenceOperandsExactlyMatched
            },
        )
        assertEquals(
            2,
            mismatched.blockingGateCounts[T730BlockingGate.UNATTRIBUTED],
        )
    }

    @Test
    fun boundedHistory_keepsLatestIdsAndReportsOverflow() {
        val collector = T730PassiveGateAttributionCollector(
            enabled = true,
            maxRejectedPeakHistory = 2,
        )
        collector.startMeasurement()

        var snapshot: T730AttributionSnapshot? = null
        repeat(3) {
            snapshot = collector.record(
                rejectedResult(
                    ankleRise = 0.019f,
                    leftAnkleRise = 0.019f,
                    rightAnkleRise = 0.019f,
                    hipRise = 0.120f,
                    hipToAnkleRatio = 6.0f,
                    feetSynchronized = true,
                ),
            )
        }

        val finalSnapshot = requireNotNull(snapshot)
        assertEquals(3, finalSnapshot.rejectedPeakCount)
        assertEquals(listOf(2, 3), finalSnapshot.retainedRejectedPeaks.map { it.eventId })
        assertEquals(1, finalSnapshot.overflowCount)
        assertEquals(
            3,
            finalSnapshot.blockingGateCounts[T730BlockingGate.RESCUE_ANKLE_RISE],
        )
    }

    @Test
    fun resetClearsMeasurementAndNextStartRestartsEventIds() {
        val collector = startedCollector()
        collector.record(peakResult(TakeoffPeakOutcome.COUNTED))
        collector.reset()

        assertNull(collector.record(peakResult(TakeoffPeakOutcome.COUNTED)))
        val restarted = requireNotNull(collector.startMeasurement())
        assertEquals(0, restarted.completedPeakCount)
        val first = requireNotNull(
            collector.record(
                rejectedResult(
                    ankleRise = 0.019f,
                    leftAnkleRise = 0.019f,
                    rightAnkleRise = 0.019f,
                    hipRise = 0.120f,
                    hipToAnkleRatio = 6.0f,
                    feetSynchronized = true,
                ),
            ),
        )
        assertEquals(1, first.retainedRejectedPeaks.single().eventId)
    }

    @Test
    fun waitingAfterMeasurementStart_invalidatesWindowAndStopsAttribution() {
        val collector = startedCollector()

        val invalid = requireNotNull(
            collector.record(
                BounceDetectionResult(
                    countedJump = false,
                    trackingStatus = BounceTrackingStatus.WAITING,
                    diagnostic = BounceDiagnostic.FULL_BODY_REQUIRED,
                ),
            ),
        )

        assertTrue(invalid.measurementInvalid)
        assertFalse(invalid.measurementActive)
        assertNull(collector.record(peakResult(TakeoffPeakOutcome.COUNTED)))
        assertTrue(formatT730AttributionSnapshot(invalid).contains("INVALID-RESTART"))
    }

    @Test
    fun calibratingAfterMeasurementStart_invalidatesAirborneTimeoutEpoch() {
        val collector = startedCollector()

        val invalid = requireNotNull(
            collector.record(
                BounceDetectionResult(
                    countedJump = false,
                    trackingStatus = BounceTrackingStatus.CALIBRATING,
                    diagnostic = BounceDiagnostic.CALIBRATING,
                ),
            ),
        )

        assertTrue(invalid.measurementInvalid)
        assertFalse(invalid.measurementActive)
        assertEquals(0, invalid.completedPeakCount)
    }

    @Test
    fun rejectedPeakThatStartedBeforeMeasurement_invalidatesWindow() {
        val collector = T730PassiveGateAttributionCollector(enabled = true)
        collector.startMeasurement(timestampMillis = 1_000L)

        val invalid = requireNotNull(
            collector.record(
                result = rejectedResult(
                    ankleRise = 0.019f,
                    leftAnkleRise = 0.019f,
                    rightAnkleRise = 0.019f,
                    hipRise = 0.120f,
                    hipToAnkleRatio = 6.0f,
                    feetSynchronized = true,
                ),
                timestampMillis = 1_050L,
            ),
        )

        assertTrue(invalid.measurementInvalid)
        assertFalse(invalid.measurementActive)
        assertEquals(0, invalid.completedPeakCount)
        assertTrue(invalid.retainedRejectedPeaks.isEmpty())
    }

    @Test
    fun rejectedPeakAfterPostGoStationaryFrame_remainsActive() {
        val collector = T730PassiveGateAttributionCollector(enabled = true)
        collector.startMeasurement(timestampMillis = 1_000L)
        assertNull(
            collector.record(
                result = BounceDetectionResult(
                    countedJump = false,
                    trackingStatus = BounceTrackingStatus.READY,
                    diagnostic = BounceDiagnostic.ANKLE_RISE_TOO_SMALL,
                ),
                timestampMillis = 1_050L,
            ),
        )

        val snapshot = requireNotNull(
            collector.record(
                result = rejectedResult(
                    ankleRise = 0.019f,
                    leftAnkleRise = 0.019f,
                    rightAnkleRise = 0.019f,
                    hipRise = 0.120f,
                    hipToAnkleRatio = 6.0f,
                    feetSynchronized = true,
                ),
                timestampMillis = 1_199L,
            ),
        )

        assertTrue(snapshot.measurementActive)
        assertFalse(snapshot.measurementInvalid)
        assertEquals(1, snapshot.rejectedPeakCount)
    }

    @Test
    fun samePeakObjectDeliveredTwice_isRecordedOnlyOnce() {
        val collector = startedCollector()
        val result = peakResult(TakeoffPeakOutcome.COUNTED)

        val first = requireNotNull(collector.record(result))

        assertEquals(1, first.completedPeakCount)
        assertNull(collector.record(result))
    }

    @Test
    fun disabledCollector_hasNoSnapshotOrHistory() {
        val collector = T730PassiveGateAttributionCollector(enabled = false)

        assertNull(collector.startMeasurement())
        assertNull(collector.record(peakResult(TakeoffPeakOutcome.COUNTED)))
    }

    @Test
    fun formatterShowsTotalsThresholdsGateCountsAndEventEvidence() {
        val collector = startedCollector()
        val snapshot = requireNotNull(
            collector.record(
                rejectedResult(
                    ankleRise = 0.019f,
                    leftAnkleRise = 0.023f,
                    rightAnkleRise = 0.017f,
                    hipRise = 0.121f,
                    hipToAnkleRatio = 6.0f,
                    feetSynchronized = true,
                ),
            ),
        )

        val text = formatT730AttributionSnapshot(snapshot)

        assertTrue(text.contains("T-730 GATE V12 ACTIVE"))
        assertTrue(text.contains("P1 C0 R1 S0 U0 OV0"))
        assertTrue(text.contains("RA1"))
        assertTrue(
            text.contains("TH SA.0450 B.0100 SH.0600 RA.0200 RH.1000 Q.8500"),
        )
        assertTrue(text.contains("#01 RES[RA]"))
        assertTrue(text.contains("A0.0190 L0.0230 R0.0170 H0.1210 Q6.0500 SY+"))
    }

    private fun startedCollector(): T730PassiveGateAttributionCollector =
        T730PassiveGateAttributionCollector(enabled = true).also {
            it.startMeasurement()
        }

    private fun rejectedResult(
        ankleRise: Float,
        leftAnkleRise: Float,
        rightAnkleRise: Float,
        hipRise: Float,
        hipToAnkleRatio: Float,
        feetSynchronized: Boolean,
        diagnostic: BounceDiagnostic = BounceDiagnostic.ANKLE_RISE_TOO_SMALL,
        peakAnkleRise: Float = ankleRise,
    ): BounceDetectionResult =
        BounceDetectionResult(
            countedJump = false,
            trackingStatus = BounceTrackingStatus.READY,
            diagnostic = diagnostic,
            rejectedTakeoffEvidence = RejectedTakeoffEvidence(
                ankleRiseRatio = ankleRise,
                hipRiseRatio = hipRise,
                hipToAnkleRiseRatio = hipToAnkleRatio,
                ankleRiseThreshold = 0.045f,
                hipRiseThreshold = 0.060f,
                hipToAnkleRiseThreshold = 0.85f,
                feetSynchronized = feetSynchronized,
                diagnostic = diagnostic,
            ),
            takeoffPeakEvidence = peakEvidence(
                outcome = TakeoffPeakOutcome.REJECTED,
                ankleRise = peakAnkleRise,
                leftAnkleRise = leftAnkleRise,
                rightAnkleRise = rightAnkleRise,
                hipRise = hipRise,
                diagnostic = diagnostic,
            ),
        )

    private fun peakResult(outcome: TakeoffPeakOutcome): BounceDetectionResult =
        BounceDetectionResult(
            countedJump = outcome == TakeoffPeakOutcome.COUNTED,
            trackingStatus = BounceTrackingStatus.READY,
            diagnostic = BounceDiagnostic.LANDED,
            takeoffPeakEvidence = peakEvidence(outcome = outcome),
        )

    private fun peakEvidence(
        outcome: TakeoffPeakOutcome,
        ankleRise: Float = 0.050f,
        leftAnkleRise: Float = 0.050f,
        rightAnkleRise: Float = 0.050f,
        hipRise: Float = 0.100f,
        diagnostic: BounceDiagnostic = BounceDiagnostic.LANDED,
    ): TakeoffPeakEvidence =
        TakeoffPeakEvidence(
            outcome = outcome,
            smoothedAnkleRiseRatio = ankleRise,
            rawAnkleRiseRatio = (leftAnkleRise + rightAnkleRise) / 2f,
            rawLeftAnkleRiseRatio = leftAnkleRise,
            rawRightAnkleRiseRatio = rightAnkleRise,
            individualAnkleRiseThreshold = 0.010f,
            leftIndividualAnkleGatePassed = leftAnkleRise >= 0.010f,
            rightIndividualAnkleGatePassed = rightAnkleRise >= 0.010f,
            smoothedHipRiseRatio = hipRise,
            rawHipRiseRatio = hipRise,
            riseFrameCount = 3,
            riseMillis = 66L,
            peakFrameIntervalMillis = 33L,
            nextFrameIntervalMillis = 33L,
            diagnostic = diagnostic,
        )

    private fun calibrate(detector: BasicBounceDetector) {
        repeat(CALIBRATION_FRAMES) { index ->
            detector.process(standingFrame(), timestampMillis = index * 33L)
        }
    }

    private fun standingFrame(): PoseFrame =
        frame(hipY = 0.40f, leftAnkleY = 0.80f, rightAnkleY = 0.80f)

    private fun frame(
        hipY: Float,
        leftAnkleY: Float,
        rightAnkleY: Float,
    ): PoseFrame {
        val points = MutableList(33) {
            NormalizedPoint(x = 0.5f, y = 0.5f, isVisible = false)
        }
        points[23] = NormalizedPoint(x = 0.45f, y = hipY, isVisible = true)
        points[24] = NormalizedPoint(x = 0.55f, y = hipY, isVisible = true)
        points[27] = NormalizedPoint(x = 0.45f, y = leftAnkleY, isVisible = true)
        points[28] = NormalizedPoint(x = 0.55f, y = rightAnkleY, isVisible = true)
        return PoseFrame(landmarks = points, imageWidth = 1080, imageHeight = 1920)
    }

    private companion object {
        const val CALIBRATION_FRAMES = 45
    }
}
