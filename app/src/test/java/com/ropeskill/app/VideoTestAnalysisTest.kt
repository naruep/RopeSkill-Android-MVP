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
        )
    }

    private fun sourceFile(relativePath: String): File {
        val candidates = listOf(File(relativePath), File("app", relativePath))
        return candidates.firstOrNull(File::isFile)
            ?: error("Cannot find source file $relativePath from ${File(".").absolutePath}")
    }
}
