package com.ropeskill.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class T730PassiveGateAttributionTest {
    @Test
    fun realDetectorAcceptedCycle_matchesTimestampAndSequenceGuards() {
        val detector = BasicBounceDetector()
        val collector = T730PassiveGateAttributionCollector(enabled = true)
        calibrate(detector)
        collector.startMeasurement(timestampMillis = 1_800L)

        collector.record(
            detector.process(standingFrame(), timestampMillis = 1_900L),
            timestampMillis = 1_900L,
        )
        val takeoff = detector.process(
            frame(hipY = 0.32f, leftAnkleY = 0.76f, rightAnkleY = 0.76f),
            timestampMillis = 2_000L,
        )
        assertNull(collector.record(takeoff, timestampMillis = 2_000L))
        val landing = detector.process(
            standingFrame(),
            timestampMillis = 2_300L,
        )
        val snapshot = requireNotNull(
            collector.record(landing, timestampMillis = 2_300L),
        )

        assertEquals(BounceEvent.TAKEOFF, takeoff.event)
        assertTrue(landing.countedJump)
        assertFalse(snapshot.measurementInvalid)
        assertEquals(200L, snapshot.window.startedAtElapsedMillis)
        assertEquals(500L, snapshot.window.endedAtElapsedMillis)
        assertEquals(1, snapshot.window.windowCountedPeakCount)
        assertEquals(1, snapshot.traceEvents.single().takeoffCycleSequence)
        assertEquals(2, snapshot.traceEvents.single().completionCycleSequence)
    }

    @Test
    fun detectorNearRescueBoundary_isAttributedWithoutChangingDetectorResult() {
        val detector = BasicBounceDetector()
        val collector = T730PassiveGateAttributionCollector(enabled = true)
        calibrate(detector)
        collector.startMeasurement(timestampMillis = 1_800L)

        collector.record(
            detector.process(
                standingFrame(),
                timestampMillis = 1_900L,
            ),
            timestampMillis = 1_900L,
        )
        val rise = detector.process(
            frame(hipY = 0.30f, leftAnkleY = 0.784f, rightAnkleY = 0.784f),
            timestampMillis = 2_000L,
        )
        collector.record(rise, timestampMillis = 2_000L)
        val reversal = detector.process(
            standingFrame(),
            timestampMillis = 2_300L,
        )
        val snapshot = requireNotNull(
            collector.record(reversal, timestampMillis = 2_300L),
        )

        assertEquals(BounceEvent.NONE, rise.event)
        assertFalse(rise.countedJump)
        assertFalse(reversal.countedJump)
        assertEquals(1, snapshot.completedPeakCount)
        assertEquals(1, snapshot.traceEvents.single().completionCycleSequence)
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
    fun rawTraceCapacityExceeded_invalidatesWithoutDiscardingRetainedTrace() {
        val collector = T730PassiveGateAttributionCollector(
            enabled = true,
            maxTraceEventHistory = 2,
            maxRetainedRejectedPeakHistory = 2,
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
        assertTrue(finalSnapshot.measurementInvalid)
        assertEquals(T730InvalidReason.TRACE_OVERFLOW, finalSnapshot.invalidReason)
        assertEquals(2, finalSnapshot.rejectedPeakCount)
        assertEquals(listOf(1, 2), finalSnapshot.retainedRejectedPeaks.map { it.eventId })
        assertEquals(listOf(1, 2), finalSnapshot.traceEvents.map { it.eventId })
        assertEquals(1, finalSnapshot.overflowCount)
        assertEquals(
            2,
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
        assertEquals(T730InvalidReason.FULL_BODY_LOSS, invalid.invalidReason)
        assertNull(collector.record(peakResult(TakeoffPeakOutcome.COUNTED)))
        assertTrue(formatT730AttributionSnapshot(invalid).contains("INVALID FULL-BODY"))
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
        assertEquals(T730InvalidReason.RECALIBRATING, invalid.invalidReason)
        assertEquals(0, invalid.completedPeakCount)
    }

    @Test
    fun rejectedPeakThatStartedBeforeMeasurement_isSignedLeadEvidence() {
        val collector = T730PassiveGateAttributionCollector(enabled = true)
        collector.startMeasurement(timestampMillis = 1_000L)

        val snapshot = requireNotNull(
            collector.record(
                result = rejectedResult(
                    ankleRise = 0.019f,
                    leftAnkleRise = 0.019f,
                    rightAnkleRise = 0.019f,
                    hipRise = 0.120f,
                    hipToAnkleRatio = 6.0f,
                    feetSynchronized = true,
                    traceTimestampMillis = 1_050L,
                    cycleSequence = 1,
                ),
                timestampMillis = 1_050L,
            ),
        )

        assertFalse(snapshot.measurementInvalid)
        assertTrue(snapshot.measurementActive)
        assertEquals(1, snapshot.completedPeakCount)
        assertEquals(T730TraceRegion.LEAD, snapshot.traceEvents.single().region)
        assertEquals(-49L, snapshot.traceEvents.single().spanStartedAtElapsedMillis)
    }

    @Test
    fun timestampedPeakOnlyRejectedFallback_isUnattributedWithoutSequence() {
        val collector = timestampedCollector()
        val peakOnly = BounceDetectionResult(
            countedJump = false,
            trackingStatus = BounceTrackingStatus.READY,
            diagnostic = BounceDiagnostic.ANKLE_RISE_TOO_SMALL,
            takeoffPeakEvidence = peakEvidence(
                outcome = TakeoffPeakOutcome.REJECTED,
            ),
        )

        val snapshot = requireNotNull(
            collector.record(
                result = peakOnly,
                timestampMillis = 1_200L,
            ),
        )

        assertFalse(snapshot.measurementInvalid)
        assertEquals(
            setOf(T730BlockingGate.UNATTRIBUTED),
            snapshot.retainedRejectedPeaks.single().blockingGates,
        )
        assertNull(snapshot.traceEvents.single().completionCycleSequence)
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
                    traceTimestampMillis = 1_199L,
                    cycleSequence = 1,
                ),
                timestampMillis = 1_199L,
            ),
        )

        assertTrue(snapshot.measurementActive)
        assertFalse(snapshot.measurementInvalid)
        assertEquals(1, snapshot.rejectedPeakCount)
    }

    @Test
    fun countedCycleAnchorsTimestampedWindowFromTakeoffToLanding() {
        val collector = timestampedCollector()

        assertNull(
            collector.record(
                result = acceptedTakeoffResult(
                    timestampMillis = 2_000L,
                    sequence = 1,
                ),
                timestampMillis = 2_000L,
            ),
        )
        val snapshot = requireNotNull(
            collector.record(
                result = acceptedLandingResult(
                    outcome = TakeoffPeakOutcome.COUNTED,
                    timestampMillis = 2_300L,
                    sequence = 2,
                    airborneMillis = 300L,
                ),
                timestampMillis = 2_300L,
            ),
        )

        assertFalse(snapshot.measurementInvalid)
        assertEquals(1_000L, snapshot.window.startedAtElapsedMillis)
        assertEquals(1_300L, snapshot.window.endedAtElapsedMillis)
        assertEquals(1, snapshot.window.windowPeakCount)
        assertEquals(1, snapshot.window.windowCountedPeakCount)
        assertEquals(T730TraceRegion.WINDOW, snapshot.traceEvents.single().region)
        assertEquals(1, snapshot.traceEvents.single().takeoffCycleSequence)
        assertEquals(2, snapshot.traceEvents.single().completionCycleSequence)
    }

    @Test
    fun suppressedCycleAlsoAnchorsTimestampedWindow() {
        val collector = timestampedCollector()
        collector.record(
            acceptedTakeoffResult(timestampMillis = 2_000L, sequence = 1),
            timestampMillis = 2_000L,
        )

        val snapshot = requireNotNull(
            collector.record(
                acceptedLandingResult(
                    outcome = TakeoffPeakOutcome.SUPPRESSED,
                    timestampMillis = 2_250L,
                    sequence = 2,
                    airborneMillis = 250L,
                ),
                timestampMillis = 2_250L,
            ),
        )

        assertEquals(1, snapshot.window.windowPeakCount)
        assertEquals(0, snapshot.window.windowCountedPeakCount)
        assertEquals(1, snapshot.window.windowSuppressedPeakCount)
        assertEquals(T730TraceRegion.WINDOW, snapshot.traceEvents.single().region)
    }

    @Test
    fun laterAcceptedAnchorPromotesBoundaryAndTailRejectsIntoWindow() {
        val collector = timestampedCollector()
        collector.record(
            rejectedResult(
                ankleRise = 0.019f,
                leftAnkleRise = 0.019f,
                rightAnkleRise = 0.019f,
                hipRise = 0.120f,
                hipToAnkleRatio = 6.0f,
                feetSynchronized = true,
                traceTimestampMillis = 1_300L,
                cycleSequence = 1,
            ),
            timestampMillis = 1_300L,
        )
        collector.record(
            acceptedTakeoffResult(timestampMillis = 2_000L, sequence = 2),
            timestampMillis = 2_000L,
        )
        collector.record(
            acceptedLandingResult(
                outcome = TakeoffPeakOutcome.COUNTED,
                timestampMillis = 2_300L,
                sequence = 3,
                airborneMillis = 300L,
            ),
            timestampMillis = 2_300L,
        )
        val boundary = requireNotNull(
            collector.record(
                rejectedResult(
                    ankleRise = 0.019f,
                    leftAnkleRise = 0.019f,
                    rightAnkleRise = 0.019f,
                    hipRise = 0.120f,
                    hipToAnkleRatio = 6.0f,
                    feetSynchronized = true,
                    traceTimestampMillis = 2_350L,
                    cycleSequence = 4,
                ),
                timestampMillis = 2_350L,
            ),
        )
        val tail = requireNotNull(
            collector.record(
                rejectedResult(
                    ankleRise = 0.019f,
                    leftAnkleRise = 0.019f,
                    rightAnkleRise = 0.019f,
                    hipRise = 0.120f,
                    hipToAnkleRatio = 6.0f,
                    feetSynchronized = true,
                    traceTimestampMillis = 2_600L,
                    cycleSequence = 5,
                ),
                timestampMillis = 2_600L,
            ),
        )

        assertEquals(T730TraceRegion.LEAD, boundary.traceEvents[0].region)
        assertEquals(T730TraceRegion.BOUNDARY, boundary.traceEvents[2].region)
        assertEquals(T730TraceRegion.TAIL, tail.traceEvents[3].region)
        assertEquals(0, tail.window.windowRejectedPeakCount)

        collector.record(
            acceptedTakeoffResult(timestampMillis = 3_000L, sequence = 6),
            timestampMillis = 3_000L,
        )
        val promoted = requireNotNull(
            collector.record(
                acceptedLandingResult(
                    outcome = TakeoffPeakOutcome.COUNTED,
                    timestampMillis = 3_300L,
                    sequence = 7,
                    airborneMillis = 300L,
                ),
                timestampMillis = 3_300L,
            ),
        )

        assertEquals(
            listOf(
                T730TraceRegion.LEAD,
                T730TraceRegion.WINDOW,
                T730TraceRegion.WINDOW,
                T730TraceRegion.WINDOW,
                T730TraceRegion.WINDOW,
            ),
            promoted.traceEvents.map { it.region },
        )
        assertEquals(4, promoted.window.windowPeakCount)
        assertEquals(2, promoted.window.windowCountedPeakCount)
        assertEquals(2, promoted.window.windowRejectedPeakCount)
        assertEquals(0, promoted.window.boundaryPeakCount)
        assertEquals(0, promoted.window.tailPeakCount)
        assertEquals(
            3,
            promoted.blockingGateCounts[T730BlockingGate.RESCUE_ANKLE_RISE],
        )
        assertEquals(
            2,
            promoted.window.windowBlockingGateCounts[
                T730BlockingGate.RESCUE_ANKLE_RISE
            ],
        )
    }

    @Test
    fun timestampGuardsExposeFirstSpecificInvalidReason() {
        val missingCollector = timestampedCollector()
        val missing = requireNotNull(
            missingCollector.record(
                BounceDetectionResult(
                    countedJump = false,
                    trackingStatus = BounceTrackingStatus.READY,
                    diagnostic = BounceDiagnostic.READY,
                ),
            ),
        )
        assertEquals(T730InvalidReason.TIMESTAMP_MISSING, missing.invalidReason)
        assertNull(missing.invalidAtElapsedMillis)
        assertTrue(
            formatT730AttributionSnapshot(missing)
                .contains("INVALID TIME-MISSING@---"),
        )

        val reversedCollector = timestampedCollector()
        assertNull(
            reversedCollector.record(
                BounceDetectionResult(
                    countedJump = false,
                    trackingStatus = BounceTrackingStatus.READY,
                    diagnostic = BounceDiagnostic.READY,
                ),
                timestampMillis = 1_100L,
            ),
        )
        val reversed = requireNotNull(
            reversedCollector.record(
                BounceDetectionResult(
                    countedJump = false,
                    trackingStatus = BounceTrackingStatus.READY,
                    diagnostic = BounceDiagnostic.READY,
                ),
                timestampMillis = 1_099L,
            ),
        )
        assertEquals(T730InvalidReason.TIMESTAMP_REVERSED, reversed.invalidReason)
        assertEquals(99L, reversed.invalidAtElapsedMillis)
        assertNull(
            reversedCollector.record(
                peakResult(TakeoffPeakOutcome.COUNTED),
                timestampMillis = 1_200L,
            ),
        )
        assertEquals(T730InvalidReason.TIMESTAMP_REVERSED, reversed.invalidReason)
    }

    @Test
    fun timestampMismatchAndCycleGapInvalidateTrace() {
        val mismatchCollector = timestampedCollector()
        val mismatch = requireNotNull(
            mismatchCollector.record(
                acceptedTakeoffResult(timestampMillis = 2_001L, sequence = 1),
                timestampMillis = 2_000L,
            ),
        )
        assertEquals(T730InvalidReason.TIMESTAMP_MISMATCH, mismatch.invalidReason)

        val gapCollector = timestampedCollector()
        gapCollector.record(
            acceptedTakeoffResult(timestampMillis = 2_000L, sequence = 1),
            timestampMillis = 2_000L,
        )
        val gap = requireNotNull(
            gapCollector.record(
                acceptedLandingResult(
                    outcome = TakeoffPeakOutcome.COUNTED,
                    timestampMillis = 2_300L,
                    sequence = 3,
                    airborneMillis = 300L,
                ),
                timestampMillis = 2_300L,
            ),
        )
        assertEquals(T730InvalidReason.CYCLE_SEQUENCE_GAP, gap.invalidReason)
        assertEquals(0, gap.completedPeakCount)
    }

    @Test
    fun trackingLossDuringAcceptedCycleHasDedicatedReason() {
        val collector = timestampedCollector()
        collector.record(
            acceptedTakeoffResult(timestampMillis = 2_000L, sequence = 1),
            timestampMillis = 2_000L,
        )

        val invalid = requireNotNull(
            collector.record(
                BounceDetectionResult(
                    countedJump = false,
                    trackingStatus = BounceTrackingStatus.WAITING,
                    diagnostic = BounceDiagnostic.FULL_BODY_REQUIRED,
                ),
                timestampMillis = 2_100L,
            ),
        )

        assertEquals(
            T730InvalidReason.TRACKING_LOSS_DURING_CYCLE,
            invalid.invalidReason,
        )
        assertEquals(1_100L, invalid.invalidAtElapsedMillis)
    }

    @Test
    fun postWindowFullBodyExitSealsAtTwoSecondBoundary() {
        val earlyCollector = timestampedCollector()
        earlyCollector.record(
            acceptedTakeoffResult(timestampMillis = 2_000L, sequence = 1),
            timestampMillis = 2_000L,
        )
        earlyCollector.record(
            acceptedLandingResult(
                outcome = TakeoffPeakOutcome.COUNTED,
                timestampMillis = 2_300L,
                sequence = 2,
                airborneMillis = 300L,
            ),
            timestampMillis = 2_300L,
        )
        val early = requireNotNull(
            earlyCollector.record(
                fullBodyLossResult(),
                timestampMillis = 4_299L,
            ),
        )
        assertTrue(early.measurementInvalid)
        assertFalse(early.measurementSealed)
        assertEquals(T730InvalidReason.FULL_BODY_LOSS, early.invalidReason)

        val sealedCollector = timestampedCollector()
        sealedCollector.record(
            acceptedTakeoffResult(timestampMillis = 2_000L, sequence = 1),
            timestampMillis = 2_000L,
        )
        sealedCollector.record(
            acceptedLandingResult(
                outcome = TakeoffPeakOutcome.COUNTED,
                timestampMillis = 2_300L,
                sequence = 2,
                airborneMillis = 300L,
            ),
            timestampMillis = 2_300L,
        )
        val sealed = requireNotNull(
            sealedCollector.record(
                fullBodyLossResult(),
                timestampMillis = 4_300L,
            ),
        )

        assertFalse(sealed.measurementInvalid)
        assertFalse(sealed.measurementActive)
        assertTrue(sealed.measurementSealed)
        assertEquals(T730StopReason.POST_WINDOW_FULL_BODY_EXIT, sealed.stopReason)
        assertEquals(3_300L, sealed.stoppedAtElapsedMillis)
        assertEquals(1, sealed.traceEvents.size)
        assertTrue(
            formatT730AttributionSnapshot(sealed)
                .contains("SEALED POST-EXIT@+3.300"),
        )
        assertNull(
            sealedCollector.record(
                peakResult(TakeoffPeakOutcome.COUNTED),
                timestampMillis = 4_400L,
            ),
        )
    }

    @Test
    fun acceptedCycleIntegrityBranchesExposeSpecificReasons() {
        val orphanCollector = timestampedCollector()
        val orphan = requireNotNull(
            orphanCollector.record(
                acceptedLandingResult(
                    outcome = TakeoffPeakOutcome.COUNTED,
                    timestampMillis = 2_300L,
                    sequence = 1,
                    airborneMillis = 300L,
                ),
                timestampMillis = 2_300L,
            ),
        )
        assertEquals(T730InvalidReason.ORPHAN_ACCEPTED_LANDING, orphan.invalidReason)

        val mismatchCollector = timestampedCollector()
        mismatchCollector.record(
            acceptedTakeoffResult(timestampMillis = 2_000L, sequence = 1),
            timestampMillis = 2_000L,
        )
        val outcomeMismatch = requireNotNull(
            mismatchCollector.record(
                acceptedLandingResult(
                    outcome = TakeoffPeakOutcome.COUNTED,
                    timestampMillis = 2_300L,
                    sequence = 2,
                    airborneMillis = 300L,
                ).copy(countedJump = false),
                timestampMillis = 2_300L,
            ),
        )
        assertEquals(
            T730InvalidReason.OUTCOME_EVENT_MISMATCH,
            outcomeMismatch.invalidReason,
        )

        val airborneCollector = timestampedCollector()
        airborneCollector.record(
            acceptedTakeoffResult(timestampMillis = 2_000L, sequence = 1),
            timestampMillis = 2_000L,
        )
        val airborneMismatch = requireNotNull(
            airborneCollector.record(
                acceptedLandingResult(
                    outcome = TakeoffPeakOutcome.COUNTED,
                    timestampMillis = 2_300L,
                    sequence = 2,
                    airborneMillis = 299L,
                ),
                timestampMillis = 2_300L,
            ),
        )
        assertEquals(
            T730InvalidReason.AIRBORNE_INTERVAL_MISMATCH,
            airborneMismatch.invalidReason,
        )
    }

    @Test
    fun retainedRejectedLimitDoesNotDiscardRawTraceOrReportOverflow() {
        val collector = T730PassiveGateAttributionCollector(
            enabled = true,
            maxTraceEventHistory = 10,
            maxRetainedRejectedPeakHistory = 2,
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
        assertEquals(3, finalSnapshot.traceEvents.size)
        assertEquals(listOf(2, 3), finalSnapshot.retainedRejectedPeaks.map { it.eventId })
        assertEquals(0, finalSnapshot.overflowCount)
        assertFalse(finalSnapshot.measurementInvalid)
    }

    @Test
    fun defaultTraceCapacityRetainsFormalSizedEvidence() {
        val collector = startedCollector()

        var snapshot: T730AttributionSnapshot? = null
        repeat(100) {
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
        assertEquals(100, finalSnapshot.traceEvents.size)
        assertEquals(0, finalSnapshot.overflowCount)
        assertFalse(finalSnapshot.measurementInvalid)
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

        assertTrue(text.contains("T-730 TRACE V14 WAIT-ANCHOR"))
        assertTrue(text.contains("ALL P1 C0 R1 S0 TR1 OV0"))
        assertTrue(text.contains("SEG L1 W0 B0 T0"))
        assertTrue(text.contains("WIN NONE P0 C0 R0 S0 U0"))
        assertTrue(
            text.contains(
                "CYC N0 AIR M--- X--- GAP M--- X--- T2T M--- X---",
            ),
        )
        assertTrue(text.contains("RA0"))
        assertTrue(
            text.contains("TH SA.0450 B.0100 SH.0600 RA.0200 RH.1000 Q.8500"),
        )
        assertTrue(text.contains("#001 L/R @---..---"))
        assertTrue(text.contains("A0.0190 L0.0230 R0.0170 H0.1210"))
        assertTrue(text.contains("RES[RA] Q6.0500 SY+"))
    }

    @Test
    fun formatterShowsAnchoredWindowBoundsAndTimestampedAcceptedSpan() {
        val collector = timestampedCollector()
        collector.record(
            acceptedTakeoffResult(timestampMillis = 2_000L, sequence = 1),
            timestampMillis = 2_000L,
        )
        val snapshot = requireNotNull(
            collector.record(
                acceptedLandingResult(
                    outcome = TakeoffPeakOutcome.COUNTED,
                    timestampMillis = 2_300L,
                    sequence = 2,
                    airborneMillis = 300L,
                ),
                timestampMillis = 2_300L,
            ),
        )

        val text = formatT730AttributionSnapshot(snapshot)

        assertTrue(text.contains("T-730 TRACE V14 WINDOW"))
        assertTrue(text.contains("SEG L0 W1 B0 T0"))
        assertTrue(text.contains("WIN +1.000..+1.300 P1 C1 R0 S0 U0"))
        assertTrue(
            text.contains(
                "CYC N1 AIR M300 X300#001 GAP M--- X--- T2T M--- X---",
            ),
        )
        assertTrue(text.contains("#001 W/C @+1.000..+1.300"))
        assertTrue(text.contains("CY A300/F1 G---/0 T--- LR? CI---"))
    }

    @Test
    fun cycleSeparationTraceCapturesAirborneRearmAndTakeoffTiming() {
        val collector = timestampedCollector()
        collector.record(
            acceptedTakeoffResult(timestampMillis = 2_000L, sequence = 1),
            timestampMillis = 2_000L,
        )
        collector.record(airborneResult(), timestampMillis = 2_100L)
        collector.record(airborneResult(), timestampMillis = 2_200L)
        collector.record(
            acceptedLandingResult(
                outcome = TakeoffPeakOutcome.COUNTED,
                timestampMillis = 2_300L,
                sequence = 2,
                airborneMillis = 300L,
                landingReason = LandingDetectionReason.RETURNED_TO_BASELINE,
                countIntervalMillis = 300L,
            ),
            timestampMillis = 2_300L,
        )
        collector.record(readyResult(), timestampMillis = 2_333L)
        collector.record(readyResult(), timestampMillis = 2_366L)
        collector.record(
            acceptedTakeoffResult(timestampMillis = 2_400L, sequence = 3),
            timestampMillis = 2_400L,
        )
        val snapshot = requireNotNull(
            collector.record(
                acceptedLandingResult(
                    outcome = TakeoffPeakOutcome.COUNTED,
                    timestampMillis = 3_100L,
                    sequence = 4,
                    airborneMillis = 700L,
                    landingReason =
                        LandingDetectionReason.COMPLETED_VERTICAL_CYCLE,
                    countIntervalMillis = 800L,
                ),
                timestampMillis = 3_100L,
            ),
        )

        assertEquals(2, snapshot.cycleSeparation.acceptedCycleCount)
        assertEquals(500L, snapshot.cycleSeparation.medianObservedAirborneMillis)
        assertEquals(700L, snapshot.cycleSeparation.maximumObservedAirborneMillis)
        assertEquals(2, snapshot.cycleSeparation.maximumObservedAirborneEventId)
        assertEquals(100L, snapshot.cycleSeparation.medianRearmMillis)
        assertEquals(2, snapshot.cycleSeparation.maximumRearmEventId)
        assertEquals(400L, snapshot.cycleSeparation.medianTakeoffIntervalMillis)
        assertEquals(2, snapshot.cycleSeparation.maximumTakeoffIntervalEventId)

        val first = snapshot.traceEvents[0].cycleSeparationEvidence
        assertEquals(300L, first?.observedAirborneMillis)
        assertEquals(3, first?.airborneFrameSamples)
        assertNull(first?.rearmMillisBeforeTakeoff)

        val second = snapshot.traceEvents[1].cycleSeparationEvidence
        assertEquals(700L, second?.observedAirborneMillis)
        assertEquals(100L, second?.rearmMillisBeforeTakeoff)
        assertEquals(2, second?.readyFrameSamplesBeforeTakeoff)
        assertEquals(400L, second?.takeoffIntervalMillis)
        assertEquals(
            LandingDetectionReason.COMPLETED_VERTICAL_CYCLE,
            second?.landingReason,
        )
        assertEquals(800L, second?.countIntervalMillis)

        val text = formatT730AttributionSnapshot(snapshot)
        assertTrue(
            text.contains(
                "CYC N2 AIR M500 X700#002 GAP M100 X100#002 " +
                    "T2T M400 X400#002",
            ),
        )
        assertTrue(text.contains("CY A700/F1 G100/2 T400 LRC CI800"))
    }

    private fun startedCollector(): T730PassiveGateAttributionCollector =
        T730PassiveGateAttributionCollector(enabled = true).also {
            it.startMeasurement()
        }

    private fun timestampedCollector(): T730PassiveGateAttributionCollector =
        T730PassiveGateAttributionCollector(enabled = true).also {
            it.startMeasurement(timestampMillis = 1_000L)
        }

    private fun acceptedTakeoffResult(
        timestampMillis: Long,
        sequence: Int,
    ): BounceDetectionResult =
        BounceDetectionResult(
            countedJump = false,
            trackingStatus = BounceTrackingStatus.AIRBORNE,
            event = BounceEvent.TAKEOFF,
            diagnostic = BounceDiagnostic.AIRBORNE,
            cycleTraceEvidence = cycleTrace(
                sequence = sequence,
                timestampMillis = timestampMillis,
                event = CycleTraceEvent.TAKEOFF,
            ),
        )

    private fun fullBodyLossResult(): BounceDetectionResult =
        BounceDetectionResult(
            countedJump = false,
            trackingStatus = BounceTrackingStatus.WAITING,
            diagnostic = BounceDiagnostic.FULL_BODY_REQUIRED,
        )

    private fun airborneResult(): BounceDetectionResult =
        BounceDetectionResult(
            countedJump = false,
            trackingStatus = BounceTrackingStatus.AIRBORNE,
            diagnostic = BounceDiagnostic.AIRBORNE,
        )

    private fun readyResult(): BounceDetectionResult =
        BounceDetectionResult(
            countedJump = false,
            trackingStatus = BounceTrackingStatus.READY,
            diagnostic = BounceDiagnostic.READY,
        )

    private fun acceptedLandingResult(
        outcome: TakeoffPeakOutcome,
        timestampMillis: Long,
        sequence: Int,
        airborneMillis: Long,
        landingReason: LandingDetectionReason? = null,
        countIntervalMillis: Long? = null,
    ): BounceDetectionResult {
        require(outcome != TakeoffPeakOutcome.REJECTED)
        val counted = outcome == TakeoffPeakOutcome.COUNTED
        return BounceDetectionResult(
            countedJump = counted,
            trackingStatus = BounceTrackingStatus.READY,
            event = BounceEvent.LANDING,
            diagnostic = BounceDiagnostic.LANDED,
            cycleTraceEvidence = cycleTrace(
                sequence = sequence,
                timestampMillis = timestampMillis,
                event = if (counted) {
                    CycleTraceEvent.LANDING_COUNTED
                } else {
                    CycleTraceEvent.LANDING_SUPPRESSED
                },
                airborneMillis = airborneMillis,
                landingReason = landingReason,
                countIntervalMillis = countIntervalMillis,
            ),
            takeoffPeakEvidence = peakEvidence(outcome = outcome),
        )
    }

    private fun cycleTrace(
        sequence: Int,
        timestampMillis: Long,
        event: CycleTraceEvent,
        airborneMillis: Long? = null,
        landingReason: LandingDetectionReason? = null,
        countIntervalMillis: Long? = null,
    ): CycleTraceEvidence =
        CycleTraceEvidence(
            sequence = sequence,
            timestampMillis = timestampMillis,
            elapsedMillis = 0L,
            intervalMillis = null,
            event = event,
            ankleRiseRatio = null,
            hipRiseRatio = null,
            diagnostic = when (event) {
                CycleTraceEvent.TAKEOFF -> BounceDiagnostic.AIRBORNE
                CycleTraceEvent.LANDING_COUNTED,
                CycleTraceEvent.LANDING_SUPPRESSED,
                -> BounceDiagnostic.LANDED
                CycleTraceEvent.REJECTED_TAKEOFF ->
                    BounceDiagnostic.ANKLE_RISE_TOO_SMALL
            },
            airborneMillis = airborneMillis,
            landingReason = landingReason,
            countIntervalMillis = countIntervalMillis,
        )

    private fun rejectedResult(
        ankleRise: Float,
        leftAnkleRise: Float,
        rightAnkleRise: Float,
        hipRise: Float,
        hipToAnkleRatio: Float,
        feetSynchronized: Boolean,
        diagnostic: BounceDiagnostic = BounceDiagnostic.ANKLE_RISE_TOO_SMALL,
        peakAnkleRise: Float = ankleRise,
        traceTimestampMillis: Long? = null,
        cycleSequence: Int? = null,
    ): BounceDetectionResult {
        require((traceTimestampMillis == null) == (cycleSequence == null))
        return BounceDetectionResult(
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
            cycleTraceEvidence = traceTimestampMillis?.let { timestamp ->
                cycleTrace(
                    sequence = requireNotNull(cycleSequence),
                    timestampMillis = timestamp,
                    event = CycleTraceEvent.REJECTED_TAKEOFF,
                )
            },
            takeoffPeakEvidence = peakEvidence(
                outcome = TakeoffPeakOutcome.REJECTED,
                ankleRise = peakAnkleRise,
                leftAnkleRise = leftAnkleRise,
                rightAnkleRise = rightAnkleRise,
                hipRise = hipRise,
                diagnostic = diagnostic,
            ),
        )
    }

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
