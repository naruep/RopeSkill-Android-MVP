package com.ropeskill.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class T733RaCandidateShadowTest {
    @Test
    fun sameFrame_nearRaBoundary_changesOnlyCandidateMetrics() {
        val runner = T733RaCandidateShadowRunner(shadowEnabled = true)
        calibrate(runner)
        val initial = requireNotNull(runner.startMeasurement())
        assertEquals(T729MeasurementState.MATCHED, initial.measurementState)

        val baselineTakeoff = runner.process(
            frame(hipY = 0.30f, leftAnkleY = 0.7856f, rightAnkleY = 0.7856f),
            timestampMillis = 2_000L,
        )
        runner.takePublishedSnapshot()
        val baselineLanding = runner.process(standingFrame(), timestampMillis = 2_300L)

        assertEquals(BounceEvent.NONE, baselineTakeoff.event)
        assertFalse(baselineLanding.countedJump)
        val snapshot = requireNotNull(runner.takePublishedSnapshot())
        assertEquals(0, snapshot.baseline.jumpCount)
        assertEquals(1, snapshot.ra016.jumpCount)
        assertEquals(1, snapshot.ra015.jumpCount)
        assertEquals(1, snapshot.ra016.takeoffCount)
        assertEquals(1, snapshot.ra016.landingCount)
        assertEquals(1, snapshot.ra015.takeoffCount)
        assertEquals(1, snapshot.ra015.landingCount)
    }

    @Test
    fun baselineResult_matchesStandaloneProductionDetector() {
        val runner = T733RaCandidateShadowRunner(shadowEnabled = true)
        val standalone = BasicBounceDetector(T733RaCandidateProfiles.BASELINE)
        repeat(CALIBRATION_FRAMES) { index ->
            val timestampMillis = index * 33L
            val frame = standingFrame()
            assertEquals(
                standalone.process(frame, timestampMillis),
                runner.process(frame, timestampMillis),
            )
        }
        runner.startMeasurement()

        listOf(
            2_000L to frame(0.30f, 0.7856f, 0.7856f),
            2_300L to standingFrame(),
            3_000L to frame(0.30f, 0.784f, 0.784f),
            3_300L to standingFrame(),
            4_000L to frame(0.32f, 0.76f, 0.76f),
            4_300L to standingFrame(),
        ).forEach { (timestampMillis, poseFrame) ->
            assertEquals(
                standalone.process(poseFrame, timestampMillis),
                runner.process(poseFrame, timestampMillis),
            )
        }
    }

    @Test
    fun movementBeforeMeasurement_invalidatesMatchedWindow() {
        val runner = T733RaCandidateShadowRunner(shadowEnabled = true)
        calibrate(runner)
        runner.process(
            frame(hipY = 0.30f, leftAnkleY = 0.7856f, rightAnkleY = 0.7856f),
            timestampMillis = 1_800L,
        )

        val snapshot = requireNotNull(runner.startMeasurement())

        assertEquals(T729MeasurementState.INVALID, snapshot.measurementState)
        assertEquals(0, snapshot.baseline.jumpCount)
        assertEquals(0, snapshot.ra016.jumpCount)
        assertEquals(0, snapshot.ra015.jumpCount)
    }

    @Test
    fun reset_clearsCandidateMetricsAndRequiresCalibration() {
        val runner = T733RaCandidateShadowRunner(shadowEnabled = true)
        calibrate(runner)
        runner.startMeasurement()
        runner.process(
            frame(hipY = 0.30f, leftAnkleY = 0.7856f, rightAnkleY = 0.7856f),
            timestampMillis = 2_000L,
        )
        runner.process(standingFrame(), timestampMillis = 2_300L)

        runner.reset()
        val snapshot = requireNotNull(runner.startMeasurement())

        assertEquals(T729MeasurementState.WAITING_FOR_ALL_READY, snapshot.measurementState)
        assertFalse(snapshot.baseline.readyForMeasurement)
        assertFalse(snapshot.ra016.readyForMeasurement)
        assertFalse(snapshot.ra015.readyForMeasurement)
        assertEquals(0, snapshot.ra015.jumpCount)
    }

    @Test
    fun shadowsDisabled_returnsOnlyProductionResult() {
        val runner = T733RaCandidateShadowRunner(shadowEnabled = false)
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
    fun formatter_identifiesCandidateFloorsAndDeltas() {
        val text = formatT733RaCandidateSnapshot(
            T733RaCandidateSnapshot(
                baseline = metrics(jumps = 19),
                ra016 = metrics(jumps = 20),
                ra015 = metrics(jumps = 21),
                measurementState = T729MeasurementState.MATCHED,
                processedFrames = 600L,
                averageProcessingMicros = 120.0,
                maxProcessingMicros = 480.0,
            ),
        )

        assertTrue(text.contains("T-733 RA SHADOW V17 MATCHED"))
        assertTrue(text.contains("BASE R0.0200 J19"))
        assertTrue(text.contains("RA16 R0.0160 J20"))
        assertTrue(text.contains("RA15 R0.0150 J21"))
        assertTrue(text.contains("D+1"))
        assertTrue(text.contains("D+2"))
        assertTrue(text.contains("PROC 120/480us F600"))
    }

    private fun metrics(jumps: Int): T729ArmMetrics =
        T729ArmMetrics(
            jumpCount = jumps,
            takeoffCount = jumps,
            landingCount = jumps,
            strongHipRescueCount = jumps,
            readyForMeasurement = true,
        )

    private fun calibrate(runner: T733RaCandidateShadowRunner) {
        repeat(CALIBRATION_FRAMES) { index ->
            runner.process(standingFrame(), timestampMillis = index * 33L)
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
