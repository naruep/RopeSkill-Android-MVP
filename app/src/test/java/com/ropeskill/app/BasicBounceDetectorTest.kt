package com.ropeskill.app

import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    private fun calibratedDetector(): BasicBounceDetector =
        BasicBounceDetector().also { detector ->
            repeat(45) { index ->
                detector.process(
                    frame(hipY = 0.40f, leftAnkleY = 0.80f, rightAnkleY = 0.80f),
                    timestampMillis = index * 33L,
                )
            }
        }

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
}
