package com.ropeskill.app

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
