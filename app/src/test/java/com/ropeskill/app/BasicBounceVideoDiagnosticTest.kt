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
            shadowRejectedTakeoffCounts = mapOf(BounceDiagnostic.ANKLE_RISE_TOO_SMALL to 2),
            shadowRejectedTakeoffTimeline = listOf(
                BasicBounceVideoRejectedTakeoff(
                    elapsedMillis = 5_500L,
                    diagnostic = BounceDiagnostic.ANKLE_RISE_TOO_SMALL,
                ),
            ),
            shadowCycleEvents = listOf(
                CycleTraceEvidence(
                    sequence = 2,
                    timestampMillis = 25_500L,
                    elapsedMillis = 5_500L,
                    intervalMillis = null,
                    event = CycleTraceEvent.REJECTED_TAKEOFF,
                    ankleRiseRatio = null,
                    hipRiseRatio = null,
                    diagnostic = BounceDiagnostic.ANKLE_RISE_TOO_SMALL,
                ),
            ),
            shadowGateMargins = listOf(
                BasicBounceVideoShadowGateMargin(
                    elapsedMillis = 5_500L,
                    diagnostic = BounceDiagnostic.ANKLE_RISE_TOO_SMALL,
                    standardAnkleMargin = -0.035f,
                    leftIndividualAnkleMargin = -0.001f,
                    rightIndividualAnkleMargin = 0.003f,
                    rescueAnkleMargin = -0.002f,
                    rescueHipMargin = 0.010f,
                    hipToAnkleMargin = 0.10405f,
                ),
            ),
            shadowProposalSnapshot = T735TakeoffGateSnapshot(
                measurementStarted = true,
                rawPulseCount = 15,
                qualifiedPulseCount = 14,
                matchedPulseCount = 13,
                unmatchedPulseCount = 1,
                unmatchedWhileAirborneCount = 0,
                attributedUnmatchedCount = 0,
                noProductionPeakCount = 1,
                pendingEvidenceCount = 0,
                blockingGateCounts = emptyMap(),
                productionTakeoffCount = 13,
                productionLandingCount = 13,
                interruptedPulseCount = 0,
                latestPulses = emptyList(),
                processedFrames = 910L,
                retainedPulses = listOf(
                    T735MotionPulse(
                        sequence = 14,
                        elapsedMillis = 6_200L,
                        durationMillis = 99L,
                        ankleRiseRatio = 0.031f,
                        hipRiseRatio = 0.105f,
                        qualified = true,
                        matchedProductionTakeoff = false,
                        peakTrackingStatus = BounceTrackingStatus.READY,
                        gateAttribution = null,
                    ),
                ),
            ),
            shadowLongAirIntervals = listOf(
                T743AirInterval(
                    sequence = 7,
                    startedAtElapsedMillis = 6_000L,
                    endedAtElapsedMillis = 6_600L,
                    frameSamples = 18,
                    closeReason = T743AirCloseReason.RETURNED_TO_BASELINE,
                    physicalPulsesWhileAirborne = 1,
                ),
            ),
            shadowLongAirFrames = listOf(
                T743LandingFrame(
                    sequence = 100L,
                    elapsedMillis = 6_300L,
                    statusBefore = BounceTrackingStatus.AIRBORNE,
                    statusAfter = BounceTrackingStatus.AIRBORNE,
                    productionTakeoffCount = 13,
                    productionLandingCount = 12,
                    productionEvent = T743ProductionEvent.NONE,
                    evidence = longAirEvidence(),
                ),
            ),
            shadowLongAirPulses = listOf(
                T743AirPulse(
                    sequence = 1,
                    intervalSequence = 7,
                    peakFrameSequence = 100L,
                    elapsedMillis = 6_300L,
                    ankleRiseRatio = 0.030f,
                    hipRiseRatio = 0.080f,
                    landingEvidenceAtPeak = longAirEvidence(),
                ),
            ),
            landingRearmProfileName = T757DetectorProfiles.SHADOW_PROFILE_NAME,
            landingRearmCountedJumps = 15,
            landingRearmRescues = listOf(
                BasicBounceVideoLandingRearmRescue(
                    elapsedMillis = 6_300L,
                    airborneMillis = 330L,
                    ankleBaselineMargin = 0.00334f,
                    hipBaselineMargin = 0.04684f,
                    ankleDescentMargin = -0.03572f,
                    hipDescentMargin = 0.09055f,
                    ankleStartedNextRise = true,
                    hipStartedNextRise = true,
                ),
            ),
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
        assertTrue(csv.contains("shadow_rejected_takeoff_ANKLE_RISE_TOO_SMALL,2"))
        assertTrue(csv.contains("shadow_proposal_qualified_pulses,14"))
        assertTrue(csv.contains("shadow_proposal_unmatched_pulses,1"))
        assertTrue(csv.contains("shadow_proposal_no_peak,1"))
        assertTrue(csv.contains("shadow_long_air_interval_total,1"))
        assertTrue(csv.contains("landing_rearm_profile,T757_LANDING_REARM_RESCUE_SHADOW"))
        assertTrue(csv.contains("landing_rearm_offline_counted_jumps,15"))
        assertTrue(csv.contains("landing_rearm_rescue_total,1"))
        assertTrue(csv.contains("7,6000,6600,600,18,RETURNED_TO_BASELINE,1"))
        assertTrue(
            csv.contains(
                "7,100,6300,AIRBORNE,AIRBORNE,NONE,300,0.05000,0.04000,-0.01000," +
                    "0.04000,0.06000,0.02000,false",
            ),
        )
        assertTrue(csv.contains("1,7,100,6300,0.03000,0.08000,-0.01000,0.02000"))
        assertTrue(
            csv.contains(
                "1,6300,330,0.00334,0.04684,-0.03572,0.09055,true,true",
            ),
        )
        assertTrue(csv.contains("shadow_cycle_sequence,event,elapsed_ms,diagnostic"))
        assertTrue(csv.contains("2,REJECTED_TAKEOFF,5500,ANKLE_RISE_TOO_SMALL"))
        assertTrue(csv.contains("shadow_rejected_index,elapsed_ms,diagnostic"))
        assertTrue(csv.contains("1,5500,ANKLE_RISE_TOO_SMALL"))
        assertTrue(csv.contains("shadow_margin_index,elapsed_ms,diagnostic,standard_ankle_margin"))
        assertTrue(
            csv.contains(
                "1,5500,ANKLE_RISE_TOO_SMALL,-0.03500,-0.00100,0.00300," +
                    "-0.00200,0.01000,0.10405",
            ),
        )
        assertTrue(
            csv.contains(
                "14,6200,99,true,false,READY,,,0.03100,0.10500",
            ),
        )
        assertTrue(csv.contains("rejected_takeoff_FEET_NOT_SYNCHRONIZED,1"))
        assertTrue(csv.contains("1,4500,FEET_NOT_SYNCHRONIZED"))
        assertTrue(result.summaryLine().contains("T/L 13/12"))
        assertTrue(result.rejectedTakeoffSummary().contains("FEET_NOT_SYNCHRONIZED=1"))
        assertTrue(result.shadowRejectedTakeoffSummary().contains("ANKLE_RISE_TOO_SMALL=2"))
        assertTrue(result.shadowProposalSummary().contains("Q/M/U/UA/NP/P 14/13/1/0/1/0"))
        assertTrue(result.shadowLongAirSummary().contains("intervals/frames/pulses 1/1/1"))
        assertTrue(result.landingRearmSummary().contains("15 jumps · rescues 1"))
    }

    @Test
    fun shadowGateMargin_reportsSignedDistanceFromExactDetectorGates() {
        val evidence = TakeoffPeakEvidence(
            outcome = TakeoffPeakOutcome.REJECTED,
            smoothedAnkleRiseRatio = 0.010f,
            rawAnkleRiseRatio = 0.007f,
            rawLeftAnkleRiseRatio = 0.005f,
            rawRightAnkleRiseRatio = 0.009f,
            individualAnkleRiseThreshold = 0.006f,
            leftIndividualAnkleGatePassed = false,
            rightIndividualAnkleGatePassed = true,
            smoothedHipRiseRatio = 0.110f,
            rawHipRiseRatio = 0.115f,
            riseFrameCount = 3,
            riseMillis = 99L,
            peakFrameIntervalMillis = 33L,
            nextFrameIntervalMillis = 33L,
            diagnostic = BounceDiagnostic.ANKLE_RISE_TOO_SMALL,
        )

        val margin = BasicBounceVideoShadowGateMargin.from(
            elapsedMillis = 5_500L,
            evidence = evidence,
            thresholds = T756DetectorProfiles.SHADOW_ONLY,
        )

        assertTrue(kotlin.math.abs(margin.standardAnkleMargin - (-0.035f)) < 0.000001f)
        assertTrue(kotlin.math.abs(margin.leftIndividualAnkleMargin - (-0.001f)) < 0.000001f)
        assertTrue(kotlin.math.abs(margin.rightIndividualAnkleMargin - 0.003f) < 0.000001f)
        assertTrue(kotlin.math.abs(margin.rescueAnkleMargin - (-0.002f)) < 0.000001f)
        assertTrue(kotlin.math.abs(margin.rescueHipMargin - 0.010f) < 0.000001f)
        assertTrue(kotlin.math.abs(margin.hipToAnkleMargin - 0.10405f) < 0.000001f)
    }

    private fun longAirEvidence(): LandingStateEvidence = LandingStateEvidence(
        ankleFromBaselineRatio = 0.050f,
        hipFromBaselineRatio = 0.040f,
        ankleBaselineLimitRatio = 0.040f,
        hipBaselineLimitRatio = 0.060f,
        ankleReturnedToBaseline = false,
        hipReturnedToBaseline = true,
        returnedToBaseline = false,
        ankleDescentFromPeakRatio = 0.030f,
        hipDescentFromPeakRatio = 0.070f,
        ankleDescentLimitRatio = 0.040f,
        hipDescentLimitRatio = 0.060f,
        ankleDescendedFromPeak = false,
        hipDescendedFromPeak = true,
        descendedFromPeak = false,
        ankleStartedNextRise = true,
        hipStartedNextRise = true,
        startedNextRise = true,
        completedVerticalCycle = false,
        airborneMillis = 300L,
        airborneTooLong = false,
        recoveredLandingAfterTimeout = false,
    )
}
