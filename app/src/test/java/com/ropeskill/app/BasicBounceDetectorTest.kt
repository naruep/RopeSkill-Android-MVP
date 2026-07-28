package com.ropeskill.app

import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BasicBounceDetectorTest {
    @Test
    fun basicBounce_withHipRiseAboveAnkleRatio_countsOnLanding() {
        val detector = calibratedDetector()

        val takeoff = detector.process(
            frame(hipY = 0.32f, leftAnkleY = 0.76f, rightAnkleY = 0.76f),
            timestampMillis = 1_000L,
        )
        val landing = detector.process(
            frame(hipY = 0.40f, leftAnkleY = 0.80f, rightAnkleY = 0.80f),
            timestampMillis = 1_500L,
        )

        assertEquals(BounceEvent.TAKEOFF, takeoff.event)
        assertEquals(BounceEvent.LANDING, landing.event)
        assertTrue(landing.countedJump)
    }

    @Test
    fun kneeLiftLikeMotion_withInsufficientRelativeHipRise_doesNotTakeOff() {
        val detector = calibratedDetector()

        val result = detector.process(
            frame(hipY = 0.37f, leftAnkleY = 0.76f, rightAnkleY = 0.76f),
            timestampMillis = 1_000L,
        )

        assertEquals(BounceEvent.NONE, result.event)
        assertFalse(result.countedJump)
        assertEquals(BounceTrackingStatus.READY, result.trackingStatus)
        assertEquals(BounceDiagnostic.HIP_RISE_TOO_SMALL, result.diagnostic)
    }

    @Test
    fun rejectedSubThresholdBounce_onDownwardReversal_emitsPeakEvidenceWithoutCount() {
        val detector = calibratedDetector()

        detector.process(
            frame(hipY = 0.38f, leftAnkleY = 0.79f, rightAnkleY = 0.79f),
            timestampMillis = 1_000L,
        )
        detector.process(
            frame(hipY = 0.36f, leftAnkleY = 0.78f, rightAnkleY = 0.78f),
            timestampMillis = 1_033L,
        )
        val reversal = detector.process(
            frame(hipY = 0.38f, leftAnkleY = 0.79f, rightAnkleY = 0.79f),
            timestampMillis = 1_066L,
        )

        val evidence = requireNotNull(reversal.rejectedTakeoffEvidence)
        assertFalse(reversal.countedJump)
        assertEquals(BounceDiagnostic.ANKLE_RISE_TOO_SMALL, evidence.diagnostic)
        assertTrue(evidence.ankleRiseRatio < evidence.ankleRiseThreshold)
        assertEquals(0.045f, evidence.ankleRiseThreshold, 0f)
        assertEquals(0.060f, evidence.hipRiseThreshold, 0f)
        assertEquals(0.85f, evidence.hipToAnkleRiseThreshold, 0f)
        val peak = requireNotNull(reversal.takeoffPeakEvidence)
        assertEquals(TakeoffPeakOutcome.REJECTED, peak.outcome)
        assertTrue(peak.rawAnkleRiseRatio > peak.smoothedAnkleRiseRatio)
        assertEquals(2, peak.riseFrameCount)
        assertEquals(33L, peak.riseMillis)
        assertEquals(33L, peak.peakFrameIntervalMillis)
        assertEquals(33L, peak.nextFrameIntervalMillis)
        assertEquals(BounceDiagnostic.ANKLE_RISE_TOO_SMALL, peak.diagnostic)
    }

    @Test
    fun acceptedBounce_emitsPassivePeakAndFrameTimingWithoutChangingCount() {
        val detector = calibratedDetector()

        detector.process(
            frame(hipY = 0.39f, leftAnkleY = 0.79f, rightAnkleY = 0.79f),
            timestampMillis = 900L,
        )
        val takeoff = detector.process(
            frame(hipY = 0.32f, leftAnkleY = 0.76f, rightAnkleY = 0.76f),
            timestampMillis = 1_000L,
        )
        val peak = detector.process(
            frame(hipY = 0.28f, leftAnkleY = 0.72f, rightAnkleY = 0.72f),
            timestampMillis = 1_033L,
        )
        val descending = detector.process(
            frame(hipY = 0.40f, leftAnkleY = 0.80f, rightAnkleY = 0.80f),
            timestampMillis = 1_100L,
        )
        val landing = detector.process(
            frame(hipY = 0.40f, leftAnkleY = 0.80f, rightAnkleY = 0.80f),
            timestampMillis = 1_133L,
        )

        assertEquals(BounceEvent.TAKEOFF, takeoff.event)
        assertFalse(peak.countedJump)
        assertFalse(descending.countedJump)
        assertEquals(BounceEvent.LANDING, landing.event)
        assertTrue(landing.countedJump)
        val evidence = requireNotNull(landing.takeoffPeakEvidence)
        assertEquals(TakeoffPeakOutcome.COUNTED, evidence.outcome)
        assertTrue(evidence.rawAnkleRiseRatio > evidence.smoothedAnkleRiseRatio)
        assertEquals(3, evidence.riseFrameCount)
        assertEquals(133L, evidence.riseMillis)
        assertEquals(33L, evidence.peakFrameIntervalMillis)
        assertEquals(67L, evidence.nextFrameIntervalMillis)
    }

    @Test
    fun rejectedTakeoff_withVisibleFootLandmarks_recordsHeelAndToeEvidence() {
        val detector = calibratedDetector(includeFootLandmarks = true)

        detector.process(
            frame(
                hipY = 0.39f,
                leftAnkleY = 0.79f,
                rightAnkleY = 0.79f,
                leftHeelY = 0.79f,
                rightHeelY = 0.79f,
                leftToeY = 0.81f,
                rightToeY = 0.81f,
            ),
            timestampMillis = 2_000L,
        )
        detector.process(
            frame(
                hipY = 0.37f,
                leftAnkleY = 0.77f,
                rightAnkleY = 0.77f,
                leftHeelY = 0.77f,
                rightHeelY = 0.77f,
                leftToeY = 0.79f,
                rightToeY = 0.79f,
            ),
            timestampMillis = 2_033L,
        )
        val reversal = detector.process(
            frame(
                hipY = 0.39f,
                leftAnkleY = 0.79f,
                rightAnkleY = 0.79f,
                leftHeelY = 0.79f,
                rightHeelY = 0.79f,
                leftToeY = 0.81f,
                rightToeY = 0.81f,
            ),
            timestampMillis = 2_066L,
        )

        val foot = requireNotNull(
            requireNotNull(reversal.rejectedTakeoffEvidence).footContactEvidence,
        )
        assertFalse(reversal.countedJump)
        assertTrue(foot.leftHeelRiseRatio > 0f)
        assertTrue(foot.rightHeelRiseRatio > 0f)
        assertTrue(foot.leftToeRiseRatio > 0f)
        assertTrue(foot.rightToeRiseRatio > 0f)
    }

    @Test
    fun acceptedTakeoff_doesNotEmitRejectedEvidence() {
        val detector = calibratedDetector()

        val takeoff = detector.process(
            frame(hipY = 0.32f, leftAnkleY = 0.76f, rightAnkleY = 0.76f),
            timestampMillis = 1_000L,
        )

        assertEquals(BounceEvent.TAKEOFF, takeoff.event)
        assertNull(takeoff.rejectedTakeoffEvidence)
    }

    @Test
    fun genuineBounce_withObservedHipToAnkleRatioAbovePointEightFive_takesOff() {
        val detector = calibratedDetector()

        val takeoff = detector.process(
            frame(hipY = 0.325f, leftAnkleY = 0.75f, rightAnkleY = 0.75f),
            timestampMillis = 1_000L,
        )

        assertEquals(BounceEvent.TAKEOFF, takeoff.event)
        assertEquals(BounceTrackingStatus.AIRBORNE, takeoff.trackingStatus)
    }

    @Test
    fun motionWithHipToAnkleRatioBelowPointEightFive_remainsRejected() {
        val detector = calibratedDetector()

        val result = detector.process(
            frame(hipY = 0.335f, leftAnkleY = 0.75f, rightAnkleY = 0.75f),
            timestampMillis = 1_000L,
        )

        assertEquals(BounceEvent.NONE, result.event)
        assertEquals(BounceTrackingStatus.READY, result.trackingStatus)
        assertEquals(BounceDiagnostic.HIP_RISE_TOO_SMALL, result.diagnostic)
    }

    @Test
    fun leftKneeLift_returningTowardSupportFoot_doesNotTakeOffOrCount() {
        val detector = calibratedDetector()

        detector.process(
            frame(hipY = 0.32f, leftAnkleY = 0.66f, rightAnkleY = 0.80f),
            timestampMillis = 1_000L,
        )
        val falseTakeoffWindow = detector.process(
            frame(hipY = 0.36f, leftAnkleY = 0.78f, rightAnkleY = 0.80f),
            timestampMillis = 1_033L,
        )
        val returnedToStanding = detector.process(
            frame(hipY = 0.40f, leftAnkleY = 0.80f, rightAnkleY = 0.80f),
            timestampMillis = 1_066L,
        )

        assertEquals(BounceEvent.NONE, falseTakeoffWindow.event)
        assertFalse(falseTakeoffWindow.countedJump)
        assertEquals(BounceTrackingStatus.READY, falseTakeoffWindow.trackingStatus)
        assertEquals(BounceDiagnostic.ANKLE_RISE_TOO_SMALL, falseTakeoffWindow.diagnostic)
        assertFalse(returnedToStanding.countedJump)
    }

    @Test
    fun rightKneeLift_returningTowardSupportFoot_doesNotTakeOffOrCount() {
        val detector = calibratedDetector()

        detector.process(
            frame(hipY = 0.32f, leftAnkleY = 0.80f, rightAnkleY = 0.66f),
            timestampMillis = 1_000L,
        )
        val falseTakeoffWindow = detector.process(
            frame(hipY = 0.36f, leftAnkleY = 0.80f, rightAnkleY = 0.78f),
            timestampMillis = 1_033L,
        )
        val returnedToStanding = detector.process(
            frame(hipY = 0.40f, leftAnkleY = 0.80f, rightAnkleY = 0.80f),
            timestampMillis = 1_066L,
        )

        assertEquals(BounceEvent.NONE, falseTakeoffWindow.event)
        assertFalse(falseTakeoffWindow.countedJump)
        assertEquals(BounceTrackingStatus.READY, falseTakeoffWindow.trackingStatus)
        assertEquals(BounceDiagnostic.ANKLE_RISE_TOO_SMALL, falseTakeoffWindow.diagnostic)
        assertFalse(returnedToStanding.countedJump)
    }

    @Test
    fun genuineBounce_withObservedBilateralAnkleRise_countsOnLanding() {
        val detector = calibratedDetector()

        val takeoff = detector.process(
            frame(hipY = 0.32f, leftAnkleY = 0.74f, rightAnkleY = 0.76f),
            timestampMillis = 1_000L,
        )
        val landing = detector.process(
            frame(hipY = 0.40f, leftAnkleY = 0.80f, rightAnkleY = 0.80f),
            timestampMillis = 1_300L,
        )

        assertEquals(BounceEvent.TAKEOFF, takeoff.event)
        assertEquals(BounceEvent.LANDING, landing.event)
        assertTrue(landing.countedJump)
        assertTrue(requireNotNull(landing.lastCountEvidence).hipRiseRatio >= 0.060f)
    }

    @Test
    fun heelRaise_withObservedHipRiseBelowPointZeroSix_doesNotTakeOffOrCount() {
        val detector = calibratedDetector()

        detector.process(
            frame(hipY = 0.39f, leftAnkleY = 0.74f, rightAnkleY = 0.74f),
            timestampMillis = 1_000L,
        )
        val falseTakeoffWindow = detector.process(
            frame(hipY = 0.363f, leftAnkleY = 0.78f, rightAnkleY = 0.78f),
            timestampMillis = 1_033L,
        )
        val returnedToStanding = detector.process(
            frame(hipY = 0.40f, leftAnkleY = 0.80f, rightAnkleY = 0.80f),
            timestampMillis = 1_066L,
        )

        assertEquals(BounceEvent.NONE, falseTakeoffWindow.event)
        assertFalse(falseTakeoffWindow.countedJump)
        assertEquals(BounceTrackingStatus.READY, falseTakeoffWindow.trackingStatus)
        assertEquals(BounceDiagnostic.HIP_RISE_TOO_SMALL, falseTakeoffWindow.diagnostic)
        assertFalse(returnedToStanding.countedJump)
    }

    @Test
    fun subthresholdAnkleRise_withStrongHipRise_usesRescueAndCountsOnLanding() {
        val detector = calibratedDetector()

        val takeoff = detector.process(
            frame(hipY = 0.30f, leftAnkleY = 0.7795f, rightAnkleY = 0.7795f),
            timestampMillis = 1_000L,
        )
        val landing = detector.process(
            frame(hipY = 0.40f, leftAnkleY = 0.80f, rightAnkleY = 0.80f),
            timestampMillis = 1_300L,
        )

        assertEquals(BounceEvent.TAKEOFF, takeoff.event)
        assertEquals(BounceEvent.LANDING, landing.event)
        assertTrue(landing.countedJump)
        val takeoffTrace = requireNotNull(takeoff.cycleTraceEvidence)
        assertEquals(1, takeoffTrace.sequence)
        assertEquals(0L, takeoffTrace.elapsedMillis)
        assertNull(takeoffTrace.intervalMillis)
        assertEquals(CycleTraceEvent.TAKEOFF, takeoffTrace.event)
        assertTrue(takeoffTrace.usedStrongHipRescue)
        val landingTrace = requireNotNull(landing.cycleTraceEvidence)
        assertEquals(2, landingTrace.sequence)
        assertEquals(300L, landingTrace.elapsedMillis)
        assertEquals(300L, landingTrace.intervalMillis)
        assertEquals(CycleTraceEvent.LANDING_COUNTED, landingTrace.event)
        assertEquals(LandingDetectionReason.RETURNED_TO_BASELINE, landingTrace.landingReason)
        assertEquals(300L, landingTrace.airborneMillis)
        val evidence = requireNotNull(landing.lastCountEvidence)
        assertTrue(evidence.usedStrongHipRescue)
        assertTrue(evidence.hipRiseRatio >= 0.100f)
    }

    @Test
    fun rescueTakeoff_insideAnkleLandingBand_waitsForHipReturnBeforeLanding() {
        val detector = calibratedDetector()

        val takeoff = detector.process(
            frame(hipY = 0.30f, leftAnkleY = 0.7795f, rightAnkleY = 0.7795f),
            timestampMillis = 1_000L,
        )
        val firstOverlappingFrame = detector.process(
            frame(hipY = 0.30f, leftAnkleY = 0.7795f, rightAnkleY = 0.7795f),
            timestampMillis = 1_033L,
        )
        val secondOverlappingFrame = detector.process(
            frame(hipY = 0.30f, leftAnkleY = 0.7795f, rightAnkleY = 0.7795f),
            timestampMillis = 1_066L,
        )
        val descending = detector.process(
            frame(hipY = 0.40f, leftAnkleY = 0.80f, rightAnkleY = 0.80f),
            timestampMillis = 1_099L,
        )
        val landing = detector.process(
            frame(hipY = 0.40f, leftAnkleY = 0.80f, rightAnkleY = 0.80f),
            timestampMillis = 1_132L,
        )

        assertEquals(BounceEvent.TAKEOFF, takeoff.event)
        listOf(firstOverlappingFrame, secondOverlappingFrame, descending).forEach {
            assertEquals(BounceEvent.NONE, it.event)
            assertFalse(it.countedJump)
            assertEquals(BounceTrackingStatus.AIRBORNE, it.trackingStatus)
            assertNull(it.cycleTraceEvidence)
        }
        assertEquals(BounceEvent.LANDING, landing.event)
        assertTrue(landing.countedJump)
        assertNull(landing.cooldownSuppressedEvidence)
        val trace = requireNotNull(landing.cycleTraceEvidence)
        assertEquals(2, trace.sequence)
        assertEquals(CycleTraceEvent.LANDING_COUNTED, trace.event)
        assertEquals(LandingDetectionReason.RETURNED_TO_BASELINE, trace.landingReason)
        assertEquals(132L, trace.airborneMillis)
    }

    @Test
    fun ankleRiseBelowPointZeroTwoFive_withStrongHipRise_remainsRejected() {
        val detector = calibratedDetector()

        val result = detector.process(
            frame(hipY = 0.30f, leftAnkleY = 0.7805f, rightAnkleY = 0.7805f),
            timestampMillis = 1_000L,
        )

        assertEquals(BounceEvent.NONE, result.event)
        assertFalse(result.countedJump)
        assertEquals(BounceTrackingStatus.READY, result.trackingStatus)
        assertEquals(BounceDiagnostic.ANKLE_RISE_TOO_SMALL, result.diagnostic)
    }

    @Test
    fun rejectedTakeoffTrace_recordsCompletedSubthresholdCycle() {
        val detector = calibratedDetector()
        detector.process(
            frame(hipY = 0.40f, leftAnkleY = 0.80f, rightAnkleY = 0.80f),
            timestampMillis = 900L,
        )
        detector.process(
            frame(hipY = 0.30f, leftAnkleY = 0.7805f, rightAnkleY = 0.7805f),
            timestampMillis = 1_000L,
        )
        val rejected = detector.process(
            frame(hipY = 0.40f, leftAnkleY = 0.80f, rightAnkleY = 0.80f),
            timestampMillis = 1_100L,
        )

        assertFalse(rejected.countedJump)
        assertEquals(BounceEvent.NONE, rejected.event)
        val trace = requireNotNull(rejected.cycleTraceEvidence)
        assertEquals(1, trace.sequence)
        assertEquals(CycleTraceEvent.REJECTED_TAKEOFF, trace.event)
        assertEquals(BounceDiagnostic.ANKLE_RISE_TOO_SMALL, trace.diagnostic)
        assertTrue(requireNotNull(trace.ankleRiseRatio) < 0.025f)
        assertTrue(requireNotNull(trace.hipRiseRatio) >= 0.100f)
    }

    @Test
    fun subthresholdAnkleRise_withoutStrongHipRise_remainsRejected() {
        val detector = calibratedDetector()

        val result = detector.process(
            frame(hipY = 0.36f, leftAnkleY = 0.774f, rightAnkleY = 0.774f),
            timestampMillis = 1_000L,
        )

        assertEquals(BounceEvent.NONE, result.event)
        assertFalse(result.countedJump)
        assertEquals(BounceTrackingStatus.READY, result.trackingStatus)
        assertEquals(BounceDiagnostic.ANKLE_RISE_TOO_SMALL, result.diagnostic)
    }

    @Test
    fun acceptedTakeoff_withoutFootLandmarks_countsWithUnavailableFootEvidence() {
        val detector = calibratedDetector()

        detector.process(
            frame(hipY = 0.32f, leftAnkleY = 0.76f, rightAnkleY = 0.76f),
            timestampMillis = 1_000L,
        )
        val landing = detector.process(
            frame(hipY = 0.40f, leftAnkleY = 0.80f, rightAnkleY = 0.80f),
            timestampMillis = 1_300L,
        )

        assertTrue(landing.countedJump)
        assertNull(requireNotNull(landing.lastCountEvidence).footContactEvidence)
    }

    @Test
    fun acceptedHeelRaiseLikeMotion_recordsStationaryToesWithoutChangingCount() {
        val detector = calibratedDetector(includeFootLandmarks = true)

        detector.process(
            frame(
                hipY = 0.32f,
                leftAnkleY = 0.76f,
                rightAnkleY = 0.76f,
                leftHeelY = 0.75f,
                rightHeelY = 0.75f,
                leftToeY = 0.82f,
                rightToeY = 0.82f,
            ),
            timestampMillis = 1_000L,
        )
        val landing = detector.process(
            frame(
                hipY = 0.40f,
                leftAnkleY = 0.80f,
                rightAnkleY = 0.80f,
                leftHeelY = 0.80f,
                rightHeelY = 0.80f,
                leftToeY = 0.82f,
                rightToeY = 0.82f,
            ),
            timestampMillis = 1_300L,
        )

        val foot = requireNotNull(
            requireNotNull(landing.lastCountEvidence).footContactEvidence,
        )
        assertTrue(landing.countedJump)
        assertTrue(foot.leftHeelRiseRatio > 0.05f)
        assertTrue(foot.rightHeelRiseRatio > 0.05f)
        assertEquals(0f, foot.leftToeRiseRatio, 0.001f)
        assertEquals(0f, foot.rightToeRiseRatio, 0.001f)
    }

    @Test
    fun acceptedBasicBounce_recordsHeelAndToeRiseWithoutChangingCount() {
        val detector = calibratedDetector(includeFootLandmarks = true)

        detector.process(
            frame(
                hipY = 0.32f,
                leftAnkleY = 0.76f,
                rightAnkleY = 0.76f,
                leftHeelY = 0.76f,
                rightHeelY = 0.76f,
                leftToeY = 0.78f,
                rightToeY = 0.78f,
            ),
            timestampMillis = 1_000L,
        )
        val landing = detector.process(
            frame(
                hipY = 0.40f,
                leftAnkleY = 0.80f,
                rightAnkleY = 0.80f,
                leftHeelY = 0.80f,
                rightHeelY = 0.80f,
                leftToeY = 0.82f,
                rightToeY = 0.82f,
            ),
            timestampMillis = 1_300L,
        )

        val foot = requireNotNull(
            requireNotNull(landing.lastCountEvidence).footContactEvidence,
        )
        assertTrue(landing.countedJump)
        assertTrue(foot.leftHeelRiseRatio > 0.05f)
        assertTrue(foot.rightHeelRiseRatio > 0.05f)
        assertTrue(foot.leftToeRiseRatio > 0.05f)
        assertTrue(foot.rightToeRiseRatio > 0.05f)
    }

    @Test
    fun descentFromAirbornePeak_withoutReturningToOldBaseline_countsLanding() {
        val detector = calibratedDetector()

        val takeoff = detector.process(
            frame(hipY = 0.30f, leftAnkleY = 0.75f, rightAnkleY = 0.75f),
            timestampMillis = 1_000L,
        )
        detector.process(
            frame(hipY = 0.24f, leftAnkleY = 0.70f, rightAnkleY = 0.70f),
            timestampMillis = 1_100L,
        )
        val descending = detector.process(
            frame(hipY = 0.36f, leftAnkleY = 0.77f, rightAnkleY = 0.77f),
            timestampMillis = 1_250L,
        )
        val landing = detector.process(
            frame(hipY = 0.30f, leftAnkleY = 0.745f, rightAnkleY = 0.745f),
            timestampMillis = 1_300L,
        )

        assertEquals(BounceEvent.TAKEOFF, takeoff.event)
        assertFalse(descending.countedJump)
        assertEquals(BounceTrackingStatus.AIRBORNE, descending.trackingStatus)
        assertEquals(BounceEvent.LANDING, landing.event)
        assertTrue(landing.countedJump)
        assertEquals(
            LandingDetectionReason.COMPLETED_VERTICAL_CYCLE,
            requireNotNull(landing.cycleTraceEvidence).landingReason,
        )
    }

    @Test
    fun continuousBasicBounce_atTargetRopeCadence_countsAtLeastEighteenOfTwenty() {
        val detector = calibratedDetector()
        var timestampMillis = 2_000L
        var countedJumps = 0

        repeat(20) {
            for (frameIndex in 1..14) {
                val lift = sin(Math.PI * frameIndex / 14.0).toFloat()
                val result = detector.process(
                    frame(
                        hipY = 0.37f - 0.105f * lift,
                        leftAnkleY = 0.77f - 0.07f * lift,
                        rightAnkleY = 0.77f - 0.07f * lift,
                    ),
                    timestampMillis = timestampMillis,
                )
                if (result.countedJump) countedJumps += 1
                timestampMillis += 33L
            }
        }

        assertTrue(
            "Expected at least 18 counts at approximately 130 jumps/min, got $countedJumps",
            countedJumps >= 18,
        )
    }

    @Test
    fun airborneWithoutLanding_afterTimeout_recalibratesWithoutCounting() {
        val detector = calibratedDetector()

        detector.process(
            frame(hipY = 0.32f, leftAnkleY = 0.76f, rightAnkleY = 0.76f),
            timestampMillis = 1_000L,
        )
        val recovery = detector.process(
            frame(hipY = 0.32f, leftAnkleY = 0.76f, rightAnkleY = 0.76f),
            timestampMillis = 2_500L,
        )

        assertFalse(recovery.countedJump)
        assertEquals(BounceEvent.NONE, recovery.event)
        assertEquals(BounceTrackingStatus.CALIBRATING, recovery.trackingStatus)
        assertEquals(BounceDiagnostic.CALIBRATING, recovery.diagnostic)
    }

    @Test
    fun landingAtTimeoutBoundary_stillCountsNormally() {
        val detector = calibratedDetector()

        detector.process(
            frame(hipY = 0.32f, leftAnkleY = 0.76f, rightAnkleY = 0.76f),
            timestampMillis = 1_000L,
        )
        val landing = detector.process(
            frame(hipY = 0.40f, leftAnkleY = 0.80f, rightAnkleY = 0.80f),
            timestampMillis = 2_500L,
        )

        assertEquals(BounceEvent.LANDING, landing.event)
        assertTrue(landing.countedJump)
        assertEquals(BounceTrackingStatus.READY, landing.trackingStatus)
    }

    @Test
    fun landingInsideCooldown_emitsSuppressedEvidenceWithoutCounting() {
        val detector = calibratedDetector()

        detector.process(
            frame(hipY = 0.32f, leftAnkleY = 0.76f, rightAnkleY = 0.76f),
            timestampMillis = 2_000L,
        )
        val firstLanding = detector.process(
            frame(hipY = 0.40f, leftAnkleY = 0.80f, rightAnkleY = 0.80f),
            timestampMillis = 2_300L,
        )
        detector.process(
            frame(hipY = 0.28f, leftAnkleY = 0.72f, rightAnkleY = 0.72f),
            timestampMillis = 2_350L,
        )
        detector.process(
            frame(hipY = 0.40f, leftAnkleY = 0.80f, rightAnkleY = 0.80f),
            timestampMillis = 2_400L,
        )
        val suppressedLanding = detector.process(
            frame(hipY = 0.40f, leftAnkleY = 0.80f, rightAnkleY = 0.80f),
            timestampMillis = 2_450L,
        )

        val evidence = requireNotNull(suppressedLanding.cooldownSuppressedEvidence)
        assertTrue(firstLanding.countedJump)
        assertEquals(BounceEvent.LANDING, suppressedLanding.event)
        assertFalse(suppressedLanding.countedJump)
        assertEquals(150L, evidence.intervalMillis)
        assertEquals(250L, evidence.cooldownMillis)
        val trace = requireNotNull(suppressedLanding.cycleTraceEvidence)
        assertEquals(CycleTraceEvent.LANDING_SUPPRESSED, trace.event)
        assertEquals(150L, trace.countIntervalMillis)
        assertEquals(
            TakeoffPeakOutcome.SUPPRESSED,
            requireNotNull(suppressedLanding.takeoffPeakEvidence).outcome,
        )
    }

    @Test
    fun airborneTimeout_afterRecalibration_canCountNextJump() {
        val detector = calibratedDetector()

        detector.process(
            frame(hipY = 0.32f, leftAnkleY = 0.76f, rightAnkleY = 0.76f),
            timestampMillis = 1_000L,
        )
        detector.process(
            frame(hipY = 0.32f, leftAnkleY = 0.76f, rightAnkleY = 0.76f),
            timestampMillis = 2_500L,
        )
        repeat(44) { index ->
            detector.process(
                frame(hipY = 0.32f, leftAnkleY = 0.76f, rightAnkleY = 0.76f),
                timestampMillis = 2_533L + index * 33L,
            )
        }

        val takeoff = detector.process(
            frame(hipY = 0.24f, leftAnkleY = 0.72f, rightAnkleY = 0.72f),
            timestampMillis = 5_000L,
        )
        val landing = detector.process(
            frame(hipY = 0.32f, leftAnkleY = 0.76f, rightAnkleY = 0.76f),
            timestampMillis = 5_500L,
        )

        assertEquals(BounceEvent.TAKEOFF, takeoff.event)
        assertEquals(BounceEvent.LANDING, landing.event)
        assertTrue(landing.countedJump)
    }

    @Test
    fun diagnosticTransitionSummary_countsOnlyExperimentReasons() {
        var counts = emptyMap<BounceDiagnostic, Int>()

        counts = recordDiagnosticTransition(counts, BounceDiagnostic.ANKLE_RISE_TOO_SMALL)
        counts = recordDiagnosticTransition(counts, BounceDiagnostic.ANKLE_RISE_TOO_SMALL)
        counts = recordDiagnosticTransition(counts, BounceDiagnostic.HIP_RISE_TOO_SMALL)
        counts = recordDiagnosticTransition(counts, BounceDiagnostic.READY)

        assertEquals(2, counts[BounceDiagnostic.ANKLE_RISE_TOO_SMALL])
        assertEquals(1, counts[BounceDiagnostic.HIP_RISE_TOO_SMALL])
        assertFalse(counts.containsKey(BounceDiagnostic.READY))
    }

    private fun calibratedDetector(
        includeFootLandmarks: Boolean = false,
    ): BasicBounceDetector =
        BasicBounceDetector().also { detector ->
            repeat(45) { index ->
                detector.process(
                    frame(
                        hipY = 0.40f,
                        leftAnkleY = 0.80f,
                        rightAnkleY = 0.80f,
                        leftHeelY = if (includeFootLandmarks) 0.80f else null,
                        rightHeelY = if (includeFootLandmarks) 0.80f else null,
                        leftToeY = if (includeFootLandmarks) 0.82f else null,
                        rightToeY = if (includeFootLandmarks) 0.82f else null,
                    ),
                    timestampMillis = index * 33L,
                )
            }
        }

    private fun frame(
        hipY: Float,
        leftAnkleY: Float,
        rightAnkleY: Float,
        leftHeelY: Float? = null,
        rightHeelY: Float? = null,
        leftToeY: Float? = null,
        rightToeY: Float? = null,
    ): PoseFrame {
        val points = MutableList(33) {
            NormalizedPoint(x = 0.5f, y = 0.5f, isVisible = false)
        }
        points[23] = NormalizedPoint(x = 0.45f, y = hipY, isVisible = true)
        points[24] = NormalizedPoint(x = 0.55f, y = hipY, isVisible = true)
        points[27] = NormalizedPoint(x = 0.45f, y = leftAnkleY, isVisible = true)
        points[28] = NormalizedPoint(x = 0.55f, y = rightAnkleY, isVisible = true)
        leftHeelY?.let {
            points[29] = NormalizedPoint(x = 0.45f, y = it, isVisible = true)
        }
        rightHeelY?.let {
            points[30] = NormalizedPoint(x = 0.55f, y = it, isVisible = true)
        }
        leftToeY?.let {
            points[31] = NormalizedPoint(x = 0.45f, y = it, isVisible = true)
        }
        rightToeY?.let {
            points[32] = NormalizedPoint(x = 0.55f, y = it, isVisible = true)
        }
        return PoseFrame(landmarks = points, imageWidth = 1080, imageHeight = 1920)
    }
}
