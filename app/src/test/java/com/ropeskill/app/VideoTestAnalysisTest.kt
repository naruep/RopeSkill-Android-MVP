package com.ropeskill.app

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoTestAnalysisTest {
    @Test
    fun framePlan_includesTwoSecondCalibrationAndThirtySecondWindow() {
        val plan = VideoTestFramePlan.create(
            videoDurationMillis = 60_000L,
            goTimestampMillis = 10_000L,
        )

        assertEquals(8_000L, plan.analysisStartMillis)
        assertEquals(40_000L, plan.analysisEndExclusiveMillis)
        assertEquals(33L, plan.frameIntervalMillis)
        assertEquals(970, plan.plannedFrameCount)
    }

    @Test
    fun framePlan_clampsToSourceVideoBoundaries() {
        val plan = VideoTestFramePlan.create(
            videoDurationMillis = 20_000L,
            goTimestampMillis = 1_000L,
        )

        assertEquals(0L, plan.analysisStartMillis)
        assertEquals(20_000L, plan.analysisEndExclusiveMillis)
    }

    @Test
    fun result_reportsProductionVersusFixedReferenceAndExportsCsv() {
        val result = sampleResult()

        assertEquals(27, result.productionRawLandings)
        assertEquals(30, result.fixedReferenceRawLandings)
        assertEquals(3, result.fixedReferenceDifference)
        assertEquals(-14, result.rightPrimaryError)
        assertEquals(14, result.rightPrimaryAbsoluteError)
        assertFalse(result.rightPrimaryMatchesGroundTruth)
        assertEquals(6.7, result.rightPrimaryAgreementPercent!!, 0.05)
        assertTrue("production_raw_landings,27" in result.toCsv())
        assertTrue("fixed_reference_raw_landings,30" in result.toCsv())
        assertTrue("counted_right_steps,13" in result.toCsv())
        assertTrue("fixed_go_pilot_left_landings,15" in result.toCsv())
        assertTrue("fixed_go_pilot_right_landings,16" in result.toCsv())
        assertTrue("fixed_go_pilot_counted_right_steps,15" in result.toCsv())
        assertTrue("fixed_candidate_counted_right_steps,1" in result.toCsv())
        assertTrue("right_primary_counted_right_steps,1" in result.toCsv())
        assertTrue("right_primary_refractory_ms,300" in result.toCsv())
        assertTrue("ground_truth_right_steps,15" in result.toCsv())
        assertTrue("right_primary_error,-14" in result.toCsv())
        assertTrue("right_primary_absolute_error,14" in result.toCsv())
        assertTrue("right_primary_agreement_percent,6.7" in result.toCsv())
        assertTrue("right_primary_ground_truth_match,FAIL" in result.toCsv())
        assertTrue("alternation_guard_counted_right_steps,0" in result.toCsv())
        assertTrue("alternation_guard_ground_truth_match,FAIL" in result.toCsv())
        assertTrue("minimum_transition_guard_min_gap_ms,100" in result.toCsv())
        assertTrue("minimum_transition_guard_counted_right_steps,0" in result.toCsv())
        assertTrue("minimum_transition_guard_ground_truth_match,FAIL" in result.toCsv())
        assertTrue("event_index,detector_source,video_timestamp_ms" in result.toCsv())
        assertTrue(
            "FIXED_REFERENCE_SHADOW,5333,333,RIGHT,CONSERVATIVE_REARM,false,NONE" in
                result.toCsv(),
        )
        assertTrue("candidate_event_index,video_timestamp_ms" in result.toCsv())
        assertTrue(
            "1,5333,333,RIGHT,CONSERVATIVE_REARM,true,1,RIGHT_ELIGIBLE,NEED_LEFT,NONE" in
                result.toCsv(),
        )
        assertTrue("right_primary_event_index,video_timestamp_ms" in result.toCsv())
        assertTrue(
            "1,5333,333,CONSERVATIVE_REARM,true,1,,NONE" in result.toCsv(),
        )
        assertTrue("alternation_guard_event_index,video_timestamp_ms" in result.toCsv())
        assertTrue("minimum_transition_guard_event_index,video_timestamp_ms" in result.toCsv())
        assertTrue("frame_index,video_timestamp_ms,relative_to_go_ms" in result.toCsv())
        assertTrue(
            "production_left_candidate_method,production_left_candidate_outcome," +
                "production_left_candidate_reject_reason" in result.toCsv(),
        )
        assertTrue(
            "CONSERVATIVE_REARM,REJECTED,CONSERVATIVE_REARM_FRAMES_PENDING" in
                result.toCsv(),
        )
        assertTrue(
            "\"5033:RIGHT:STRICT:COUNTED_RIGHT_STEP:NONE|" +
                "5066:RIGHT:STRICT:REJECTED:REPEATED_RIGHT\"" in result.toCsv(),
        )
    }

    @Test
    fun poseEvidence_capturesRawFootInputsAndClassifierDerivedLegLengths() {
        val points = MutableList(33) { NormalizedPoint(0f, 0f, false) }
        points[23] = NormalizedPoint(0f, 0.50f, true)
        points[24] = NormalizedPoint(0f, 0.51f, true)
        points[27] = NormalizedPoint(0f, 0.90f, true)
        points[28] = NormalizedPoint(0f, 0.91f, true)
        points[29] = NormalizedPoint(0f, 0.92f, true)
        points[30] = NormalizedPoint(0f, 0.93f, false)
        points[31] = NormalizedPoint(0f, 0.94f, true)
        points[32] = NormalizedPoint(0f, 0.95f, true)

        val evidence = VideoTestPoseEvidence.capture(
            frame = PoseFrame(points, 100, 200, 1L),
            leftLegLength = 0.42f,
        )

        assertEquals(0.90f, evidence.leftAnkleY!!, 0f)
        assertTrue(evidence.leftAnkleVisible)
        assertEquals(0.93f, evidence.rightHeelY!!, 0f)
        assertFalse(evidence.rightHeelVisible)
        assertEquals(0.42f, evidence.leftLegLength!!, 0.0001f)
        assertEquals(null, evidence.rightLegLength)
    }

    @Test
    fun result_comparesPositiveAndNegativeGroundTruthWithoutInflatingAgreement() {
        val matching = sampleResult().copy(groundTruthRightSteps = 1)
        assertTrue(matching.rightPrimaryMatchesGroundTruth)
        assertEquals(100.0, matching.rightPrimaryAgreementPercent!!, 0.0)

        val overcount = sampleResult().copy(groundTruthRightSteps = 0)
        assertFalse(overcount.rightPrimaryMatchesGroundTruth)
        assertEquals(1, overcount.rightPrimaryError)
        assertEquals(null, overcount.rightPrimaryAgreementPercent)

        val severeOvercount = sampleResult().copy(groundTruthRightSteps = 1).let {
            it.copy(
                rightPrimaryCandidate = it.rightPrimaryCandidate.copy(
                    countedRightSteps = 3,
                    acceptedRightEvents = 3,
                ),
            )
        }
        assertEquals(0.0, severeOvercount.rightPrimaryAgreementPercent!!, 0.0)
    }

    @Test
    fun debugRoute_isGuardedAndVideoModeDoesNotReferenceHistoryStorage() {
        val mainActivity = sourceFile("src/main/java/com/ropeskill/app/MainActivity.kt").readText()
        val viewModel = sourceFile("src/main/java/com/ropeskill/app/VideoTestViewModel.kt").readText()

        assertTrue("if (BuildConfig.DEBUG)" in mainActivity)
        assertTrue("composable(VIDEO_TEST_ROUTE)" in mainActivity)
        assertTrue("ActivityResultContracts.OpenDocument()" in sourceFile(
            "src/main/java/com/ropeskill/app/VideoTestScreen.kt",
        ).readText())
        assertFalse("SessionRepository" in viewModel)
        assertFalse("TrainingSession" in viewModel)
        assertFalse("BasicBounceDetector" in viewModel)
    }

    private fun sampleResult(): VideoTestAnalysisResult {
        val landingEvents = listOf(
            VideoTestLandingEvent(
                detectorSource = VideoTestDetectorSource.PRODUCTION,
                videoTimestampMillis = 5_300L,
                foot = SpeedLanding.RIGHT,
                landingMethod = SpeedLandingDetectionMethod.STRICT,
                countedRightStep = true,
            ),
            VideoTestLandingEvent(
                detectorSource = VideoTestDetectorSource.FIXED_REFERENCE_SHADOW,
                videoTimestampMillis = 5_333L,
                foot = SpeedLanding.RIGHT,
                landingMethod = SpeedLandingDetectionMethod.CONSERVATIVE_REARM,
            ),
            VideoTestLandingEvent(
                detectorSource = VideoTestDetectorSource.FIXED_GO_PILOT,
                videoTimestampMillis = 5_366L,
                foot = SpeedLanding.RIGHT,
                landingMethod = SpeedLandingDetectionMethod.STRICT,
                countedRightStep = true,
            ),
        )
        return VideoTestAnalysisResult(
            videoName = "speed,test.mp4",
            videoDurationMillis = 40_000L,
            goTimestampMillis = 5_000L,
            groundTruthRightSteps = 15,
            sampledFrames = 970,
            framesWithValidPose = 950,
            lowVisibilityFrames = 20,
            productionLeftLandings = 14,
            productionRightLandings = 13,
            productionLeftConservativeRearms = 2,
            productionRightConservativeRearms = 3,
            countedRightSteps = 13,
            fixedGoPilotLeftLandings = 15,
            fixedGoPilotRightLandings = 16,
            fixedGoPilotCountedRightSteps = 15,
            fixedGoPilotRepeatedLeftRejects = 1,
            fixedGoPilotRepeatedRightRejects = 2,
            fixedGoPilotBothFeetRejects = 3,
            fixedGoPilotUnclearLandingRejects = 4,
            fixedReferenceLeftLandings = 15,
            fixedReferenceRightLandings = 15,
            fixedReferenceLeftStrictLandings = 11,
            fixedReferenceRightStrictLandings = 10,
            fixedReferenceLeftConservativeRearms = 4,
            fixedReferenceRightConservativeRearms = 5,
            repeatedLeftRejects = 1,
            repeatedRightRejects = 0,
            bothFeetRejects = 0,
            unclearLandingRejects = 0,
            landingEvents = landingEvents,
            frameTrace = listOf(
                VideoTestFrameTrace(
                    frameIndex = 1,
                    videoTimestampMillis = 5_033L,
                    classifierDiagnostic = SpeedClassifierDiagnostic.READY,
                    trackingValid = true,
                    pose = VideoTestPoseEvidence(
                        leftAnkleY = 0.90f,
                        leftAnkleVisible = true,
                        rightAnkleY = 0.91f,
                        rightAnkleVisible = true,
                        leftHeelY = 0.92f,
                        leftHeelVisible = true,
                        rightHeelY = 0.93f,
                        rightHeelVisible = true,
                        leftToeY = 0.94f,
                        leftToeVisible = true,
                        rightToeY = 0.95f,
                        rightToeVisible = true,
                        leftLegLength = 0.40f,
                        rightLegLength = 0.41f,
                    ),
                    fixedLeftGroundReference = 0.96f,
                    fixedRightGroundReference = 0.97f,
                    productionLeftPhaseBefore = SpeedFootPhase.GROUNDED,
                    productionLeftPhaseAfter = SpeedFootPhase.AIRBORNE,
                    productionLeftRiseRatio = 0.1234567f,
                    productionLeftRearmFrames = 1,
                    productionLeftCandidateMethod =
                        SpeedLandingDetectionMethod.CONSERVATIVE_REARM,
                    productionLeftCandidateOutcome = SpeedCandidateOutcome.REJECTED,
                    productionLeftCandidateRejectReason =
                        SpeedCandidateRejectReason.CONSERVATIVE_REARM_FRAMES_PENDING,
                    productionRightPhaseBefore = SpeedFootPhase.GROUNDED,
                    productionRightPhaseAfter = SpeedFootPhase.GROUNDED,
                    productionRightRiseRatio = 0.02f,
                    productionRightRearmFrames = 0,
                    productionRightCandidateMethod = null,
                    productionRightCandidateOutcome = SpeedCandidateOutcome.NO_CANDIDATE,
                    productionRightCandidateRejectReason =
                        SpeedCandidateRejectReason.GROUNDED_BELOW_LIFT_THRESHOLD,
                    fixedLeftPhase = SpeedFootPhase.AIRBORNE,
                    fixedLeftRiseRatio = 0.111111f,
                    fixedLeftRearmFrames = 1,
                    fixedRightPhase = SpeedFootPhase.GROUNDED,
                    fixedRightRiseRatio = 0.01f,
                    fixedRightRearmFrames = 0,
                    fixedLeftCandidateMethod = SpeedLandingDetectionMethod.CONSERVATIVE_REARM,
                    fixedLeftCandidateOutcome = SpeedCandidateOutcome.REJECTED,
                    fixedLeftCandidateRejectReason =
                        SpeedCandidateRejectReason.CONSERVATIVE_REARM_FRAMES_PENDING,
                    fixedRightCandidateMethod = null,
                    fixedRightCandidateOutcome = SpeedCandidateOutcome.NO_CANDIDATE,
                    fixedRightCandidateRejectReason =
                        SpeedCandidateRejectReason.GROUNDED_BELOW_LIFT_THRESHOLD,
                    productionEvents = listOf(
                        VideoTestFrameLandingEvent(
                            videoTimestampMillis = 5_033L,
                            foot = SpeedLanding.RIGHT,
                            landingMethod = SpeedLandingDetectionMethod.STRICT,
                            counterOutcome = VideoTestSpeedStepOutcome.COUNTED_RIGHT_STEP,
                            counterRejectReason = SpeedRejectReason.NONE,
                        ),
                        VideoTestFrameLandingEvent(
                            videoTimestampMillis = 5_066L,
                            foot = SpeedLanding.RIGHT,
                            landingMethod = SpeedLandingDetectionMethod.STRICT,
                            counterOutcome = VideoTestSpeedStepOutcome.REJECTED,
                            counterRejectReason = SpeedRejectReason.REPEATED_RIGHT,
                        ),
                    ),
                    fixedReferenceEvents = listOf(
                        VideoTestFrameLandingEvent(
                            videoTimestampMillis = 5_033L,
                            foot = SpeedLanding.RIGHT,
                            landingMethod = SpeedLandingDetectionMethod.CONSERVATIVE_REARM,
                        ),
                    ),
                ),
            ),
            fixedReferenceCandidate = VideoTestCandidateCounter.evaluate(
                fixedReferenceEvents = landingEvents,
                goTimestampMillis = 5_000L,
            ),
            rightPrimaryCandidate = VideoTestRightPrimaryCounter.evaluate(
                fixedReferenceEvents = landingEvents,
                goTimestampMillis = 5_000L,
            ),
            alternationGuardCandidate = VideoTestAlternationGuardCounter.evaluate(
                fixedReferenceEvents = landingEvents,
                goTimestampMillis = 5_000L,
            ),
            minimumTransitionGapCandidate = VideoTestMinimumTransitionGapCounter.evaluate(
                fixedReferenceEvents = landingEvents,
                goTimestampMillis = 5_000L,
            ),
        )
    }

    private fun sourceFile(relativePath: String): File {
        val candidates = listOf(File(relativePath), File("app", relativePath))
        return candidates.firstOrNull(File::isFile)
            ?: error("Cannot find source file $relativePath from ${File(".").absolutePath}")
    }
}
