package com.ropeskill.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class T743PassiveLandingStateTraceTest {
    @Test
    fun enabledAndDisabledObserver_produceIdenticalDetectorResultsForEveryFrame() {
        val enabledDetector = BasicBounceDetector(landingStateEvidenceEnabled = true)
        val disabledDetector = BasicBounceDetector()
        val enabledObserver = T743PassiveLandingStateCollector(enabled = true)
        val disabledObserver = T743PassiveLandingStateCollector(enabled = false)
        enabledObserver.startMeasurement(0L)
        assertNull(disabledObserver.startMeasurement(0L))

        val samples = buildList {
            repeat(45) { add(frame(hipY = 0.40f, ankleY = 0.80f)) }
            add(frame(hipY = 0.30f, ankleY = 0.74f))
            add(frame(hipY = 0.24f, ankleY = 0.69f))
            add(frame(hipY = 0.40f, ankleY = 0.80f))
            add(frame(hipY = 0.40f, ankleY = 0.80f))
            add(frame(hipY = 0.29f, ankleY = 0.73f))
            add(frame(hipY = 0.23f, ankleY = 0.68f))
            add(frame(hipY = 0.39f, ankleY = 0.79f))
        }

        samples.forEachIndexed { index, poseFrame ->
            val timestampMillis = index * 33L
            val enabledResult = enabledDetector.process(poseFrame, timestampMillis)
            val disabledResult = disabledDetector.process(poseFrame, timestampMillis)

            assertEquals(
                disabledResult,
                enabledResult.copy(landingStateEvidence = null),
            )
            enabledObserver.record(poseFrame, enabledResult, timestampMillis)
            assertNull(
                disabledObserver.record(poseFrame, disabledResult, timestampMillis),
            )
        }
    }

    @Test
    fun airborneEvidence_exposesExactProductionLandingOperands() {
        val detector = calibratedDetector()

        val takeoff = detector.process(
            frame(hipY = 0.30f, ankleY = 0.74f),
            timestampMillis = 2_000L,
        )
        val airborne = detector.process(
            frame(hipY = 0.24f, ankleY = 0.69f),
            timestampMillis = 2_100L,
        )
        detector.process(
            frame(hipY = 0.40f, ankleY = 0.80f),
            timestampMillis = 2_200L,
        )
        val landing = detector.process(
            frame(hipY = 0.40f, ankleY = 0.80f),
            timestampMillis = 2_300L,
        )

        assertEquals(BounceEvent.TAKEOFF, takeoff.event)
        val airborneEvidence = requireNotNull(airborne.landingStateEvidence)
        assertFalse(airborneEvidence.returnedToBaseline)
        assertFalse(airborneEvidence.completedVerticalCycle)
        assertEquals(0.04f, airborneEvidence.ankleBaselineLimitRatio, 0f)
        assertEquals(0.06f, airborneEvidence.hipBaselineLimitRatio, 0f)
        assertEquals(100L, airborneEvidence.airborneMillis)

        val landingEvidence = requireNotNull(landing.landingStateEvidence)
        assertTrue(landingEvidence.ankleReturnedToBaseline)
        assertTrue(landingEvidence.hipReturnedToBaseline)
        assertTrue(landingEvidence.returnedToBaseline)
        assertEquals(BounceEvent.LANDING, landing.event)
        assertTrue(landing.countedJump)
    }

    @Test
    fun observer_closesProductionAirIntervalWithLandingReason() {
        val detector = calibratedDetector()
        val observer = T743PassiveLandingStateCollector(enabled = true)
        observer.startMeasurement(1_900L)

        val takeoffFrame = frame(hipY = 0.30f, ankleY = 0.74f)
        val takeoff = detector.process(takeoffFrame, timestampMillis = 2_000L)
        observer.record(takeoffFrame, takeoff, timestampMillis = 2_000L)
        val peakFrame = frame(hipY = 0.24f, ankleY = 0.69f)
        val peak = detector.process(peakFrame, timestampMillis = 2_100L)
        observer.record(peakFrame, peak, timestampMillis = 2_100L)
        val descentFrame = frame(hipY = 0.40f, ankleY = 0.80f)
        val descent = detector.process(descentFrame, timestampMillis = 2_200L)
        observer.record(descentFrame, descent, timestampMillis = 2_200L)
        val landingFrame = frame(hipY = 0.40f, ankleY = 0.80f)
        val landing = detector.process(landingFrame, timestampMillis = 2_300L)
        val snapshot = requireNotNull(
            observer.record(landingFrame, landing, timestampMillis = 2_300L),
        )

        assertEquals(1, snapshot.productionTakeoffCount)
        assertEquals(1, snapshot.productionLandingCount)
        assertEquals(0, snapshot.productionSuppressedLandingCount)
        assertEquals(1, snapshot.airIntervalCount)
        assertFalse(snapshot.openAirInterval)
        assertEquals(1, snapshot.closedByBaselineCount)
        assertEquals(0, snapshot.evidenceGapCount)
        assertEquals(
            T743AirCloseReason.RETURNED_TO_BASELINE,
            snapshot.recentIntervals.single().closeReason,
        )
        assertEquals(
            T743ProductionEvent.LANDING_COUNTED,
            snapshot.recentFrames.last().productionEvent,
        )
    }

    @Test
    fun observer_historiesRemainBoundedAcrossLongSession() {
        val observer = T743PassiveLandingStateCollector(
            enabled = true,
            maxFrameHistory = 3,
            maxIntervalHistory = 2,
        )
        observer.startMeasurement(0L)
        val poseFrame = frame(hipY = 0.40f, ankleY = 0.80f)
        var timestampMillis = 0L
        var snapshot: T743LandingStateSnapshot? = null

        repeat(100) { cycle ->
            timestampMillis += 300L
            observer.record(
                poseFrame,
                result(
                    event = BounceEvent.TAKEOFF,
                    status = BounceTrackingStatus.AIRBORNE,
                    counted = false,
                    trace = trace(
                        sequence = cycle * 2 + 1,
                        event = CycleTraceEvent.TAKEOFF,
                        timestampMillis = timestampMillis,
                    ),
                ),
                timestampMillis,
            )
            timestampMillis += 150L
            snapshot = observer.record(
                poseFrame,
                result(
                    event = BounceEvent.LANDING,
                    status = BounceTrackingStatus.READY,
                    counted = true,
                    trace = trace(
                        sequence = cycle * 2 + 2,
                        event = CycleTraceEvent.LANDING_COUNTED,
                        timestampMillis = timestampMillis,
                        landingReason = LandingDetectionReason.RETURNED_TO_BASELINE,
                    ),
                    landingEvidence = returnedEvidence(airborneMillis = 150L),
                ),
                timestampMillis,
            )
        }

        val finalSnapshot = requireNotNull(snapshot)
        assertEquals(100, finalSnapshot.productionTakeoffCount)
        assertEquals(100, finalSnapshot.productionLandingCount)
        assertEquals(100, finalSnapshot.airIntervalCount)
        assertTrue(finalSnapshot.recentFrames.size <= 3)
        assertTrue(finalSnapshot.recentIntervals.size <= 2)
        assertEquals(0, finalSnapshot.evidenceGapCount)
    }

    @Test
    fun observer_recordsQualifiedUnmatchedPulseWhileProductionRemainsAirborne() {
        val observer = T743PassiveLandingStateCollector(enabled = true)
        observer.startMeasurement(0L)
        observer.record(
            frame(hipY = 0.40f, ankleY = 0.80f),
            result(
                event = BounceEvent.TAKEOFF,
                status = BounceTrackingStatus.AIRBORNE,
                trace = trace(
                    sequence = 1,
                    event = CycleTraceEvent.TAKEOFF,
                    timestampMillis = 100L,
                ),
            ),
            100L,
        )
        observer.record(
            frame(hipY = 0.40f, ankleY = 0.80f),
            result(status = BounceTrackingStatus.AIRBORNE),
            133L,
        )
        observer.record(
            frame(hipY = 0.35f, ankleY = 0.77f),
            result(
                status = BounceTrackingStatus.AIRBORNE,
                landingEvidence = airborneEvidence(airborneMillis = 66L),
            ),
            166L,
        )
        observer.record(
            frame(hipY = 0.31f, ankleY = 0.75f),
            result(
                status = BounceTrackingStatus.AIRBORNE,
                landingEvidence = airborneEvidence(airborneMillis = 99L),
            ),
            199L,
        )
        val snapshot = requireNotNull(
            observer.record(
                frame(hipY = 0.36f, ankleY = 0.78f),
                result(
                    status = BounceTrackingStatus.AIRBORNE,
                    landingEvidence = airborneEvidence(airborneMillis = 132L),
                ),
                232L,
            ),
        )

        assertEquals(1, snapshot.physicalPulsesWhileAirborne)
        val pulse = snapshot.recentAirPulses.single()
        assertEquals(1, pulse.sequence)
        assertEquals(1, pulse.intervalSequence)
        assertEquals(4L, pulse.peakFrameSequence)
        assertEquals(199L, pulse.elapsedMillis)
        assertNotNull(pulse.landingEvidenceAtPeak)
    }

    @Test
    fun resetAndDisabledMode_clearOrPublishNoDiagnosticState() {
        val disabled = T743PassiveLandingStateCollector(enabled = false)
        assertNull(disabled.startMeasurement(0L))
        assertNull(
            disabled.record(
                frame(hipY = 0.40f, ankleY = 0.80f),
                result(),
                33L,
            ),
        )

        val enabled = T743PassiveLandingStateCollector(enabled = true)
        assertNotNull(enabled.startMeasurement(0L))
        enabled.reset()
        assertNull(
            enabled.record(
                frame(hipY = 0.40f, ankleY = 0.80f),
                result(),
                33L,
            ),
        )
    }

    @Test
    fun formattedSnapshot_isCompactAndIncludesOperandsAndCloseReason() {
        val text = formatT743LandingStateSnapshot(
            T743LandingStateSnapshot(
                measurementStarted = true,
                productionTakeoffCount = 3,
                productionLandingCount = 3,
                productionSuppressedLandingCount = 0,
                airIntervalCount = 3,
                openAirInterval = false,
                closedByBaselineCount = 2,
                closedByVerticalCycleCount = 1,
                closedByBothCount = 0,
                closedByTimeoutRecoveryCount = 0,
                resetOrCalibrateCount = 0,
                evidenceGapCount = 0,
                physicalPulsesWhileAirborne = 1,
                recentFrames = listOf(
                    T743LandingFrame(
                        sequence = 20,
                        elapsedMillis = 3_000L,
                        statusBefore = BounceTrackingStatus.AIRBORNE,
                        statusAfter = BounceTrackingStatus.READY,
                        productionTakeoffCount = 3,
                        productionLandingCount = 3,
                        productionEvent = T743ProductionEvent.LANDING_COUNTED,
                        evidence = returnedEvidence(airborneMillis = 180L),
                    ),
                ),
                recentIntervals = listOf(
                    T743AirInterval(
                        sequence = 3,
                        startedAtElapsedMillis = 2_820L,
                        endedAtElapsedMillis = 3_000L,
                        frameSamples = 6,
                        closeReason = T743AirCloseReason.RETURNED_TO_BASELINE,
                        physicalPulsesWhileAirborne = 1,
                    ),
                ),
                recentAirPulses = listOf(
                    T743AirPulse(
                        sequence = 1,
                        intervalSequence = 3,
                        peakFrameSequence = 18,
                        elapsedMillis = 2_940L,
                        ankleRiseRatio = 0.020f,
                        hipRiseRatio = 0.080f,
                        landingEvidenceAtPeak = returnedEvidence(airborneMillis = 120L),
                    ),
                ),
                processedFrames = 90,
            ),
        )

        assertTrue(text.contains("T-743 LANDING STATE V23 PASSIVE"))
        assertTrue(text.contains("T/L3/3 SUP0 PA1 U0"))
        assertTrue(text.contains("I#03"))
        assertTrue(text.contains("BASE"))
        assertTrue(text.contains("B A"))
        assertTrue(text.contains("D A"))
        assertTrue(text.contains("P#01 I3 F18"))
        assertTrue(text.contains("PB A"))
    }

    private fun calibratedDetector(): BasicBounceDetector =
        BasicBounceDetector(landingStateEvidenceEnabled = true).also { detector ->
            repeat(45) { index ->
                detector.process(
                    frame(hipY = 0.40f, ankleY = 0.80f),
                    timestampMillis = index * 33L,
                )
            }
        }

    private fun frame(hipY: Float, ankleY: Float): PoseFrame {
        val points = MutableList(33) {
            NormalizedPoint(x = 0.5f, y = 0.5f, isVisible = false)
        }
        points[23] = NormalizedPoint(x = 0.45f, y = hipY, isVisible = true)
        points[24] = NormalizedPoint(x = 0.55f, y = hipY, isVisible = true)
        points[27] = NormalizedPoint(x = 0.45f, y = ankleY, isVisible = true)
        points[28] = NormalizedPoint(x = 0.55f, y = ankleY, isVisible = true)
        return PoseFrame(points, imageWidth = 1080, imageHeight = 1920)
    }

    private fun result(
        event: BounceEvent = BounceEvent.NONE,
        status: BounceTrackingStatus = BounceTrackingStatus.READY,
        counted: Boolean = false,
        trace: CycleTraceEvidence? = null,
        landingEvidence: LandingStateEvidence? = null,
    ): BounceDetectionResult =
        BounceDetectionResult(
            countedJump = counted,
            trackingStatus = status,
            event = event,
            diagnostic = when (status) {
                BounceTrackingStatus.WAITING -> BounceDiagnostic.FULL_BODY_REQUIRED
                BounceTrackingStatus.CALIBRATING -> BounceDiagnostic.CALIBRATING
                BounceTrackingStatus.READY -> BounceDiagnostic.READY
                BounceTrackingStatus.AIRBORNE -> BounceDiagnostic.AIRBORNE
            },
            cycleTraceEvidence = trace,
            landingStateEvidence = landingEvidence,
        )

    private fun trace(
        sequence: Int,
        event: CycleTraceEvent,
        timestampMillis: Long,
        landingReason: LandingDetectionReason? = null,
    ): CycleTraceEvidence =
        CycleTraceEvidence(
            sequence = sequence,
            timestampMillis = timestampMillis,
            elapsedMillis = timestampMillis,
            intervalMillis = null,
            event = event,
            ankleRiseRatio = null,
            hipRiseRatio = null,
            diagnostic = if (event == CycleTraceEvent.TAKEOFF) {
                BounceDiagnostic.AIRBORNE
            } else {
                BounceDiagnostic.LANDED
            },
            landingReason = landingReason,
        )

    private companion object {
        fun airborneEvidence(airborneMillis: Long): LandingStateEvidence =
            returnedEvidence(airborneMillis).copy(
                ankleFromBaselineRatio = 0.080f,
                hipFromBaselineRatio = 0.100f,
                ankleReturnedToBaseline = false,
                hipReturnedToBaseline = false,
                returnedToBaseline = false,
                ankleDescendedFromPeak = false,
                hipDescendedFromPeak = false,
                descendedFromPeak = false,
            )

        fun returnedEvidence(airborneMillis: Long): LandingStateEvidence =
            LandingStateEvidence(
                ankleFromBaselineRatio = 0.010f,
                hipFromBaselineRatio = 0.020f,
                ankleBaselineLimitRatio = 0.040f,
                hipBaselineLimitRatio = 0.060f,
                ankleReturnedToBaseline = true,
                hipReturnedToBaseline = true,
                returnedToBaseline = true,
                ankleDescentFromPeakRatio = 0.070f,
                hipDescentFromPeakRatio = 0.090f,
                ankleDescentLimitRatio = 0.040f,
                hipDescentLimitRatio = 0.060f,
                ankleDescendedFromPeak = true,
                hipDescendedFromPeak = true,
                descendedFromPeak = true,
                ankleStartedNextRise = false,
                hipStartedNextRise = false,
                startedNextRise = false,
                completedVerticalCycle = false,
                airborneMillis = airborneMillis,
                airborneTooLong = false,
                recoveredLandingAfterTimeout = false,
            )
    }
}
