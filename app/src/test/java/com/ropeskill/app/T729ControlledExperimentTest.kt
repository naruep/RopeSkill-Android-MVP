package com.ropeskill.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class T729ControlledExperimentTest {
    @Test
    fun sameFrame_nearRescueBoundary_changesOnlyRescueShadowMetrics() {
        var nowNanos = 0L
        val runner = T729ControlledExperimentRunner(
            shadowEnabled = true,
            nanoTime = {
                nowNanos += 100_000L
                nowNanos
            },
        )
        calibrate(runner)

        val initial = requireNotNull(runner.startMeasurement())
        assertEquals(T729MeasurementState.MATCHED, initial.measurementState)
        assertTrue(initial.baseline.readyForMeasurement)
        assertTrue(initial.bilateralOnly.readyForMeasurement)
        assertTrue(initial.rescueOnly.readyForMeasurement)

        val baselineTakeoff = runner.process(
            frame(hipY = 0.30f, leftAnkleY = 0.784f, rightAnkleY = 0.784f),
            timestampMillis = 2_000L,
        )
        runner.takePublishedSnapshot()
        val baselineLanding = runner.process(
            frame(hipY = 0.40f, leftAnkleY = 0.80f, rightAnkleY = 0.80f),
            timestampMillis = 2_300L,
        )

        assertEquals(BounceEvent.NONE, baselineTakeoff.event)
        assertFalse(baselineLanding.countedJump)
        val snapshot = requireNotNull(runner.takePublishedSnapshot())
        assertEquals(0, snapshot.baseline.jumpCount)
        assertEquals(0, snapshot.bilateralOnly.jumpCount)
        assertEquals(1, snapshot.rescueOnly.jumpCount)
        assertEquals(1, snapshot.rescueOnly.takeoffCount)
        assertEquals(1, snapshot.rescueOnly.landingCount)
        assertEquals(0, snapshot.rescueOnly.cooldownSuppressedCount)
        assertEquals(2L, snapshot.processedFrames)
        assertEquals(100.0, snapshot.averageProcessingMicros, 0.0)
        assertEquals(100.0, snapshot.maxProcessingMicros, 0.0)
    }

    @Test
    fun sameFrame_asymmetricBoundary_changesOnlyBilateralShadowMetrics() {
        val runner = T729ControlledExperimentRunner(shadowEnabled = true)
        calibrate(runner)
        runner.startMeasurement()

        val baselineTakeoff = runner.process(
            frame(hipY = 0.30f, leftAnkleY = 0.7968f, rightAnkleY = 0.766f),
            timestampMillis = 2_000L,
        )
        runner.takePublishedSnapshot()
        val baselineLanding = runner.process(
            standingFrame(),
            timestampMillis = 2_300L,
        )

        assertEquals(BounceEvent.NONE, baselineTakeoff.event)
        assertFalse(baselineLanding.countedJump)
        val snapshot = requireNotNull(runner.takePublishedSnapshot())
        assertEquals(T729MeasurementState.MATCHED, snapshot.measurementState)
        assertEquals(0, snapshot.baseline.jumpCount)
        assertEquals(1, snapshot.bilateralOnly.jumpCount)
        assertEquals(0, snapshot.rescueOnly.jumpCount)
        assertEquals(1, snapshot.bilateralOnly.takeoffCount)
        assertEquals(1, snapshot.bilateralOnly.landingCount)
    }

    @Test
    fun baselineResult_matchesFullStandaloneResultAcrossStateSequences() {
        val runner = T729ControlledExperimentRunner(shadowEnabled = true)
        val standalone = BasicBounceDetector(T729DetectorProfiles.BASELINE)
        repeat(CALIBRATION_FRAMES) { index ->
            val timestampMillis = index * 33L
            val standing = standingFrame()
            assertEquals(
                standalone.process(standing, timestampMillis),
                runner.process(standing, timestampMillis),
            )
        }
        runner.startMeasurement()

        val stateSequence = listOf(
            2_000L to frame(
                hipY = 0.32f,
                leftAnkleY = 0.76f,
                rightAnkleY = 0.76f,
            ),
            2_300L to standingFrame(),
            2_350L to frame(
                hipY = 0.28f,
                leftAnkleY = 0.72f,
                rightAnkleY = 0.72f,
            ),
            2_400L to standingFrame(),
            2_450L to standingFrame(),
            3_000L to frame(
                hipY = 0.30f,
                leftAnkleY = 0.7968f,
                rightAnkleY = 0.766f,
            ),
            3_100L to standingFrame(),
            4_000L to frame(
                hipY = 0.30f,
                leftAnkleY = 0.7795f,
                rightAnkleY = 0.7795f,
            ),
            4_300L to standingFrame(),
        )
        stateSequence.forEach { (timestampMillis, poseFrame) ->
            assertEquals(
                standalone.process(poseFrame, timestampMillis),
                runner.process(poseFrame, timestampMillis),
            )
        }

        runner.reset()
        standalone.reset()
        repeat(6) { index ->
            val timestampMillis = 5_000L + index * 33L
            assertEquals(
                standalone.process(missingFrame(), timestampMillis),
                runner.process(missingFrame(), timestampMillis),
            )
        }
        repeat(CALIBRATION_FRAMES) { index ->
            val timestampMillis = 6_000L + index * 33L
            assertEquals(
                standalone.process(standingFrame(), timestampMillis),
                runner.process(standingFrame(), timestampMillis),
            )
        }
    }

    @Test
    fun reset_clearsShadowMetricsAndRequiresFreshCalibration() {
        val runner = T729ControlledExperimentRunner(shadowEnabled = true)
        calibrate(runner)
        runner.startMeasurement()
        runner.process(
            frame(hipY = 0.30f, leftAnkleY = 0.784f, rightAnkleY = 0.784f),
            timestampMillis = 2_000L,
        )
        runner.process(standingFrame(), timestampMillis = 2_300L)

        runner.reset()

        val beforeCalibration = requireNotNull(runner.startMeasurement())
        assertEquals(0, beforeCalibration.rescueOnly.jumpCount)
        assertEquals(
            T729MeasurementState.WAITING_FOR_ALL_READY,
            beforeCalibration.measurementState,
        )
        assertFalse(beforeCalibration.baseline.readyForMeasurement)
        assertFalse(beforeCalibration.bilateralOnly.readyForMeasurement)
        assertFalse(beforeCalibration.rescueOnly.readyForMeasurement)
    }

    @Test
    fun measurementWaitsForAllArmsAndStartsMatchedAfterFreshCalibration() {
        val runner = T729ControlledExperimentRunner(shadowEnabled = true)

        val initial = requireNotNull(runner.startMeasurement())
        assertEquals(
            T729MeasurementState.WAITING_FOR_ALL_READY,
            initial.measurementState,
        )

        var latest = initial
        repeat(CALIBRATION_FRAMES) { index ->
            runner.process(standingFrame(), timestampMillis = index * 33L)
            latest = requireNotNull(runner.takePublishedSnapshot())
        }

        assertEquals(T729MeasurementState.MATCHED, latest.measurementState)
        assertTrue(latest.baseline.readyForMeasurement)
        assertTrue(latest.bilateralOnly.readyForMeasurement)
        assertTrue(latest.rescueOnly.readyForMeasurement)
        assertEquals(0, latest.baseline.jumpCount)
        assertEquals(0L, latest.processedFrames)
    }

    @Test
    fun standingCountdownFrames_remainReadyForMatchedStart() {
        val runner = T729ControlledExperimentRunner(shadowEnabled = true)
        calibrate(runner)

        repeat(5) { index ->
            val result = runner.process(
                standingFrame(),
                timestampMillis = 1_500L + index * 33L,
            )
            assertEquals(BounceTrackingStatus.READY, result.trackingStatus)
            assertEquals(BounceDiagnostic.ANKLE_RISE_TOO_SMALL, result.diagnostic)
        }

        val snapshot = requireNotNull(runner.startMeasurement())
        assertEquals(T729MeasurementState.MATCHED, snapshot.measurementState)
        assertTrue(snapshot.baseline.readyForMeasurement)
        assertTrue(snapshot.bilateralOnly.readyForMeasurement)
        assertTrue(snapshot.rescueOnly.readyForMeasurement)
    }

    @Test
    fun movementBeforeGo_invalidatesFormalMatchedWindowImmediately() {
        val runner = T729ControlledExperimentRunner(shadowEnabled = true)
        calibrate(runner)
        runner.process(
            frame(hipY = 0.30f, leftAnkleY = 0.7968f, rightAnkleY = 0.766f),
            timestampMillis = 1_800L,
        )

        val initial = requireNotNull(runner.startMeasurement())
        assertEquals(T729MeasurementState.INVALID, initial.measurementState)
        runner.process(standingFrame(), timestampMillis = 2_100L)

        val invalid = requireNotNull(runner.takePublishedSnapshot())
        assertEquals(T729MeasurementState.INVALID, invalid.measurementState)
        assertEquals(0, invalid.baseline.jumpCount)
        assertEquals(0, invalid.bilateralOnly.jumpCount)
        assertEquals(0, invalid.rescueOnly.jumpCount)
        assertEquals(0L, invalid.processedFrames)
    }

    @Test
    fun completedShadowOnlyCycleBeforeGo_invalidatesLaterMatchedStart() {
        val runner = T729ControlledExperimentRunner(shadowEnabled = true)
        calibrate(runner)
        runner.process(
            frame(hipY = 0.30f, leftAnkleY = 0.7968f, rightAnkleY = 0.766f),
            timestampMillis = 1_800L,
        )
        runner.process(standingFrame(), timestampMillis = 2_100L)
        repeat(3) { index ->
            runner.process(
                standingFrame(),
                timestampMillis = 2_200L + index * 33L,
            )
        }

        val invalid = requireNotNull(runner.startMeasurement())
        assertEquals(T729MeasurementState.INVALID, invalid.measurementState)
        assertEquals(0, invalid.baseline.jumpCount)
        assertEquals(0, invalid.bilateralOnly.jumpCount)
        assertEquals(0, invalid.rescueOnly.jumpCount)
        assertEquals(0L, invalid.processedFrames)
    }

    @Test
    fun rejectedMicroMotionBeforeGo_doesNotInvalidateDetectorState() {
        val runner = T729ControlledExperimentRunner(shadowEnabled = true)
        calibrate(runner)
        runner.process(
            frame(hipY = 0.30f, leftAnkleY = 0.7856f, rightAnkleY = 0.7856f),
            timestampMillis = 1_800L,
        )
        runner.process(standingFrame(), timestampMillis = 2_100L)

        val snapshot = requireNotNull(runner.startMeasurement())
        assertEquals(T729MeasurementState.MATCHED, snapshot.measurementState)
        assertTrue(snapshot.baseline.readyForMeasurement)
        assertTrue(snapshot.bilateralOnly.readyForMeasurement)
        assertTrue(snapshot.rescueOnly.readyForMeasurement)
    }

    @Test
    fun shadowsDisabled_returnsOnlyProductionBaselineResult() {
        val runner = T729ControlledExperimentRunner(shadowEnabled = false)
        calibrate(runner)

        assertNull(runner.startMeasurement())
        val takeoff = runner.process(
            frame(hipY = 0.32f, leftAnkleY = 0.76f, rightAnkleY = 0.76f),
            timestampMillis = 2_000L,
        )
        val landing = runner.process(standingFrame(), timestampMillis = 2_300L)

        assertNull(runner.takePublishedSnapshot())
        assertEquals(BounceEvent.TAKEOFF, takeoff.event)
        assertTrue(landing.countedJump)
    }

    @Test
    fun formatter_identifiesIsolatedProfilesAndMatchedDeltas() {
        val text = formatT729ExperimentSnapshot(
            T729ExperimentSnapshot(
                baseline = T729ArmMetrics(
                    jumpCount = 83,
                    takeoffCount = 83,
                    landingCount = 83,
                    strongHipRescueCount = 79,
                    readyForMeasurement = true,
                ),
                bilateralOnly = T729ArmMetrics(
                    jumpCount = 88,
                    takeoffCount = 88,
                    landingCount = 88,
                    strongHipRescueCount = 84,
                    readyForMeasurement = true,
                ),
                rescueOnly = T729ArmMetrics(
                    jumpCount = 90,
                    takeoffCount = 90,
                    landingCount = 90,
                    strongHipRescueCount = 86,
                    readyForMeasurement = true,
                ),
                measurementState = T729MeasurementState.MATCHED,
                processedFrames = 2_572L,
                averageProcessingMicros = 125.0,
                maxProcessingMicros = 460.0,
            ),
        )

        assertTrue(text.contains("T-729 SHADOW V1 MATCHED"))
        assertTrue(text.contains("BASE B0.0100 R0.0200 J83"))
        assertTrue(text.contains("BIL B0.0060 R0.0200 J88"))
        assertTrue(text.contains("RES B0.0100 R0.0180 J90"))
        assertTrue(text.contains("RES79"))
        assertTrue(text.contains("D+5"))
        assertTrue(text.contains("D+7"))
        assertTrue(text.contains("PROC 125/460us F2572"))
    }

    private fun calibrate(runner: T729ControlledExperimentRunner) {
        repeat(CALIBRATION_FRAMES) { index ->
            runner.process(standingFrame(), timestampMillis = index * 33L)
        }
    }

    private fun standingFrame(): PoseFrame =
        frame(hipY = 0.40f, leftAnkleY = 0.80f, rightAnkleY = 0.80f)

    private fun missingFrame(): PoseFrame =
        PoseFrame(landmarks = emptyList(), imageWidth = 1080, imageHeight = 1920)

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
