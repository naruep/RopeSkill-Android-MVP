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
        assertTrue("production_raw_landings,27" in result.toCsv())
        assertTrue("fixed_reference_raw_landings,30" in result.toCsv())
        assertTrue("counted_right_steps,13" in result.toCsv())
        assertTrue("event_index,detector_source,video_timestamp_ms" in result.toCsv())
        assertTrue(
            "FIXED_REFERENCE_SHADOW,5333,333,RIGHT,CONSERVATIVE_REARM,false,NONE" in
                result.toCsv(),
        )
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

    private fun sampleResult() = VideoTestAnalysisResult(
        videoName = "speed,test.mp4",
        videoDurationMillis = 40_000L,
        goTimestampMillis = 5_000L,
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
        landingEvents = listOf(
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
        ),
    )

    private fun sourceFile(relativePath: String): File {
        val candidates = listOf(File(relativePath), File("app", relativePath))
        return candidates.firstOrNull(File::isFile)
            ?: error("Cannot find source file $relativePath from ${File(".").absolutePath}")
    }
}
