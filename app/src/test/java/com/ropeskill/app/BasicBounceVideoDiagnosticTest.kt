package com.ropeskill.app

import org.junit.Assert.assertTrue
import org.junit.Test

class BasicBounceVideoDiagnosticTest {
    @Test
    fun framePlan_keepsStillCalibrationAndBoundsLongAnalysisWindow() {
        val plan = BasicBounceVideoFramePlan.create(
            videoDurationMillis = 180_000L,
            goTimestampMillis = 2_000L,
        )

        assertTrue(plan.analysisStartMillis == 0L)
        assertTrue(plan.analysisEndExclusiveMillis == 122_000L)
        assertTrue(plan.plannedFrameCount > 3_000)
    }

    @Test
    fun csv_isBoundedSummaryAndEscapesTheSelectedVideoName() {
        val result = BasicBounceVideoDiagnosticResult(
            videoName = "stall,\"screen\".mp4",
            videoDurationMillis = 90_000L,
            goTimestampMillis = 20_000L,
            sampledFrames = 910,
            countedJumps = 12,
            finalTrackingStatus = BounceTrackingStatus.AIRBORNE,
            finalDiagnostic = BounceDiagnostic.AIRBORNE,
            landingSnapshot = T743LandingStateSnapshot(
                measurementStarted = true,
                productionTakeoffCount = 13,
                productionLandingCount = 12,
                productionSuppressedLandingCount = 0,
                airIntervalCount = 13,
                openAirInterval = true,
                closedByBaselineCount = 12,
                closedByVerticalCycleCount = 0,
                closedByBothCount = 0,
                closedByTimeoutRecoveryCount = 0,
                resetOrCalibrateCount = 0,
                evidenceGapCount = 0,
                physicalPulsesWhileAirborne = 2,
                recentFrames = emptyList(),
                recentIntervals = emptyList(),
                recentAirPulses = emptyList(),
                processedFrames = 910L,
            ),
            shadowProfileName = T756DetectorProfiles.SHADOW_PROFILE_NAME,
            shadowCountedJumps = 14,
            rejectedTakeoffCounts = mapOf(BounceDiagnostic.FEET_NOT_SYNCHRONIZED to 1),
            rejectedTakeoffTimeline = listOf(
                BasicBounceVideoRejectedTakeoff(
                    elapsedMillis = 4_500L,
                    diagnostic = BounceDiagnostic.FEET_NOT_SYNCHRONIZED,
                ),
            ),
            cycleEvents = emptyList(),
        )

        val csv = result.toCsv()

        assertTrue(csv.contains("video_name,\"stall,\"\"screen\"\".mp4\""))
        assertTrue(csv.contains("open_airborne_interval,true"))
        assertTrue(csv.contains("shadow_profile,T756_BILATERAL_HIP_RESCUE_SHADOW"))
        assertTrue(csv.contains("shadow_offline_counted_jumps,14"))
        assertTrue(csv.contains("rejected_takeoff_FEET_NOT_SYNCHRONIZED,1"))
        assertTrue(csv.contains("1,4500,FEET_NOT_SYNCHRONIZED"))
        assertTrue(result.summaryLine().contains("T/L 13/12"))
        assertTrue(result.rejectedTakeoffSummary().contains("FEET_NOT_SYNCHRONIZED=1"))
    }
}
