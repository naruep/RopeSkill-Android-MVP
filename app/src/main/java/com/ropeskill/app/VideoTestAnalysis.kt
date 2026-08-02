package com.ropeskill.app

import java.util.Locale
import kotlin.math.max
import kotlin.math.min

data class VideoTestFramePlan(
    val videoDurationMillis: Long,
    val goTimestampMillis: Long,
    val analysisStartMillis: Long,
    val analysisEndExclusiveMillis: Long,
    val frameIntervalMillis: Long,
) {
    val plannedFrameCount: Int
        get() = ((analysisEndExclusiveMillis - analysisStartMillis + frameIntervalMillis - 1L) /
            frameIntervalMillis).toInt()

    companion object {
        const val PRE_GO_CALIBRATION_MILLIS = 2_000L
        const val ANALYSIS_DURATION_MILLIS = 30_000L
        const val FRAME_INTERVAL_MILLIS = 33L

        fun create(videoDurationMillis: Long, goTimestampMillis: Long): VideoTestFramePlan {
            require(videoDurationMillis > 0L)
            require(goTimestampMillis in 0 until videoDurationMillis)
            return VideoTestFramePlan(
                videoDurationMillis = videoDurationMillis,
                goTimestampMillis = goTimestampMillis,
                analysisStartMillis = max(0L, goTimestampMillis - PRE_GO_CALIBRATION_MILLIS),
                analysisEndExclusiveMillis = min(
                    videoDurationMillis,
                    goTimestampMillis + ANALYSIS_DURATION_MILLIS,
                ),
                frameIntervalMillis = FRAME_INTERVAL_MILLIS,
            )
        }
    }
}

data class VideoTestAnalysisResult(
    val videoName: String,
    val videoDurationMillis: Long,
    val goTimestampMillis: Long,
    val groundTruthRightSteps: Int,
    val sampledFrames: Int,
    val framesWithValidPose: Long,
    val lowVisibilityFrames: Long,
    val productionLeftLandings: Int,
    val productionRightLandings: Int,
    val productionLeftConservativeRearms: Int,
    val productionRightConservativeRearms: Int,
    val countedRightSteps: Int,
    val fixedReferenceLeftLandings: Int,
    val fixedReferenceRightLandings: Int,
    val fixedReferenceLeftStrictLandings: Int,
    val fixedReferenceRightStrictLandings: Int,
    val fixedReferenceLeftConservativeRearms: Int,
    val fixedReferenceRightConservativeRearms: Int,
    val repeatedLeftRejects: Int,
    val repeatedRightRejects: Int,
    val bothFeetRejects: Int,
    val unclearLandingRejects: Int,
    val landingEvents: List<VideoTestLandingEvent>,
    val fixedReferenceCandidate: VideoTestCandidateCounterResult,
    val rightPrimaryCandidate: VideoTestRightPrimaryCounterResult,
    val alternationGuardCandidate: VideoTestAlternationGuardCounterResult,
    val minimumTransitionGapCandidate: VideoTestAlternationGuardCounterResult,
) {
    val productionRawLandings: Int
        get() = productionLeftLandings + productionRightLandings

    val fixedReferenceRawLandings: Int
        get() = fixedReferenceLeftLandings + fixedReferenceRightLandings

    val fixedReferenceDifference: Int
        get() = fixedReferenceRawLandings - productionRawLandings

    val rightPrimaryError: Int
        get() = rightPrimaryCandidate.countedRightSteps - groundTruthRightSteps

    val rightPrimaryAbsoluteError: Int
        get() = kotlin.math.abs(rightPrimaryError)

    val rightPrimaryAgreementPercent: Double?
        get() = if (groundTruthRightSteps > 0) {
            (1.0 - rightPrimaryAbsoluteError.toDouble() / groundTruthRightSteps)
                .coerceIn(0.0, 1.0) * 100.0
        } else {
            null
        }

    val rightPrimaryMatchesGroundTruth: Boolean
        get() = rightPrimaryError == 0

    val alternationGuardError: Int
        get() = alternationGuardCandidate.countedRightSteps - groundTruthRightSteps

    val alternationGuardAbsoluteError: Int
        get() = kotlin.math.abs(alternationGuardError)

    val alternationGuardMatchesGroundTruth: Boolean
        get() = alternationGuardError == 0

    val minimumTransitionGapError: Int
        get() = minimumTransitionGapCandidate.countedRightSteps - groundTruthRightSteps

    val minimumTransitionGapAbsoluteError: Int
        get() = kotlin.math.abs(minimumTransitionGapError)

    val minimumTransitionGapMatchesGroundTruth: Boolean
        get() = minimumTransitionGapError == 0

    fun toCsv(): String = buildString {
        appendLine("metric,value")
        appendLine("video_name,${csvEscape(videoName)}")
        appendLine("video_duration_ms,$videoDurationMillis")
        appendLine("go_timestamp_ms,$goTimestampMillis")
        appendLine("ground_truth_right_steps,$groundTruthRightSteps")
        appendLine("sampled_frames,$sampledFrames")
        appendLine("valid_pose_frames,$framesWithValidPose")
        appendLine("low_visibility_frames,$lowVisibilityFrames")
        appendLine("production_left_landings,$productionLeftLandings")
        appendLine("production_right_landings,$productionRightLandings")
        appendLine("production_raw_landings,$productionRawLandings")
        appendLine("production_left_conservative_rearms,$productionLeftConservativeRearms")
        appendLine("production_right_conservative_rearms,$productionRightConservativeRearms")
        appendLine("counted_right_steps,$countedRightSteps")
        appendLine("fixed_reference_left_landings,$fixedReferenceLeftLandings")
        appendLine("fixed_reference_right_landings,$fixedReferenceRightLandings")
        appendLine("fixed_reference_raw_landings,$fixedReferenceRawLandings")
        appendLine("fixed_reference_left_strict_landings,$fixedReferenceLeftStrictLandings")
        appendLine("fixed_reference_right_strict_landings,$fixedReferenceRightStrictLandings")
        appendLine("fixed_reference_left_conservative_rearms,$fixedReferenceLeftConservativeRearms")
        appendLine("fixed_reference_right_conservative_rearms,$fixedReferenceRightConservativeRearms")
        appendLine("fixed_reference_difference,$fixedReferenceDifference")
        appendLine("repeated_left_rejects,$repeatedLeftRejects")
        appendLine("repeated_right_rejects,$repeatedRightRejects")
        appendLine("both_feet_rejects,$bothFeetRejects")
        appendLine("unclear_landing_rejects,$unclearLandingRejects")
        appendLine("fixed_candidate_counted_right_steps,${fixedReferenceCandidate.countedRightSteps}")
        appendLine("fixed_candidate_accepted_events,${fixedReferenceCandidate.acceptedEvents}")
        appendLine("fixed_candidate_rejected_events,${fixedReferenceCandidate.rejectedEvents}")
        appendLine("fixed_candidate_repeated_left_rejects,${fixedReferenceCandidate.repeatedLeftRejects}")
        appendLine("fixed_candidate_repeated_right_rejects,${fixedReferenceCandidate.repeatedRightRejects}")
        appendLine("fixed_candidate_both_feet_rejects,${fixedReferenceCandidate.bothFeetRejects}")
        appendLine("fixed_candidate_unclear_rejects,${fixedReferenceCandidate.unclearLandingRejects}")
        appendLine("right_primary_refractory_ms,${VideoTestRightPrimaryCounter.REFRACTORY_MILLIS}")
        appendLine("right_primary_counted_right_steps,${rightPrimaryCandidate.countedRightSteps}")
        appendLine("right_primary_accepted_right_events,${rightPrimaryCandidate.acceptedRightEvents}")
        appendLine("right_primary_rejected_right_events,${rightPrimaryCandidate.rejectedRightEvents}")
        appendLine("right_primary_refractory_rejects,${rightPrimaryCandidate.refractoryRejects}")
        appendLine("right_primary_minimum_interval_ms,${rightPrimaryCandidate.minimumAcceptedIntervalMillis.orEmptyCsv()}")
        appendLine("right_primary_median_interval_ms,${rightPrimaryCandidate.medianAcceptedIntervalMillis.orEmptyCsv()}")
        appendLine("right_primary_maximum_interval_ms,${rightPrimaryCandidate.maximumAcceptedIntervalMillis.orEmptyCsv()}")
        appendLine("right_primary_error,$rightPrimaryError")
        appendLine("right_primary_absolute_error,$rightPrimaryAbsoluteError")
        appendLine(
            "right_primary_agreement_percent," +
                (rightPrimaryAgreementPercent?.let { String.format(Locale.US, "%.1f", it) } ?: ""),
        )
        appendLine("right_primary_ground_truth_match,${if (rightPrimaryMatchesGroundTruth) "PASS" else "FAIL"}")
        appendLine("alternation_guard_max_gap_ms,${VideoTestAlternationGuardCounter.MAX_ALTERNATION_GAP_MILLIS}")
        appendLine(
            "alternation_guard_max_consecutive_bridges," +
                VideoTestAlternationGuardCounter.MAX_CONSECUTIVE_CADENCE_BRIDGES,
        )
        appendLine("alternation_guard_counted_right_steps,${alternationGuardCandidate.countedRightSteps}")
        appendLine("alternation_guard_accepted_right_events,${alternationGuardCandidate.acceptedRightEvents}")
        appendLine("alternation_guard_rejected_right_events,${alternationGuardCandidate.rejectedRightEvents}")
        appendLine("alternation_guard_confirmed_sequence_accepts,${alternationGuardCandidate.confirmedSequenceAccepts}")
        appendLine("alternation_guard_recent_left_accepts,${alternationGuardCandidate.recentLeftAccepts}")
        appendLine("alternation_guard_cadence_bridge_accepts,${alternationGuardCandidate.cadenceBridgeAccepts}")
        appendLine("alternation_guard_refractory_rejects,${alternationGuardCandidate.refractoryRejects}")
        appendLine(
            "alternation_guard_unconfirmed_rejects," +
                alternationGuardCandidate.unconfirmedAlternationRejects,
        )
        appendLine("alternation_guard_missing_rejects,${alternationGuardCandidate.missingAlternationRejects}")
        appendLine("alternation_guard_bridge_limit_rejects,${alternationGuardCandidate.bridgeLimitRejects}")
        appendLine("alternation_guard_error,$alternationGuardError")
        appendLine("alternation_guard_absolute_error,$alternationGuardAbsoluteError")
        appendLine(
            "alternation_guard_ground_truth_match," +
                if (alternationGuardMatchesGroundTruth) "PASS" else "FAIL",
        )
        appendLine(
            "minimum_transition_guard_min_gap_ms," +
                VideoTestMinimumTransitionGapCounter.MIN_TRANSITION_GAP_MILLIS,
        )
        appendLine(
            "minimum_transition_guard_max_gap_ms," +
                VideoTestAlternationGuardCounter.MAX_ALTERNATION_GAP_MILLIS,
        )
        appendLine(
            "minimum_transition_guard_counted_right_steps," +
                minimumTransitionGapCandidate.countedRightSteps,
        )
        appendLine(
            "minimum_transition_guard_accepted_right_events," +
                minimumTransitionGapCandidate.acceptedRightEvents,
        )
        appendLine(
            "minimum_transition_guard_rejected_right_events," +
                minimumTransitionGapCandidate.rejectedRightEvents,
        )
        appendLine(
            "minimum_transition_guard_confirmed_sequence_accepts," +
                minimumTransitionGapCandidate.confirmedSequenceAccepts,
        )
        appendLine(
            "minimum_transition_guard_recent_left_accepts," +
                minimumTransitionGapCandidate.recentLeftAccepts,
        )
        appendLine(
            "minimum_transition_guard_cadence_bridge_accepts," +
                minimumTransitionGapCandidate.cadenceBridgeAccepts,
        )
        appendLine(
            "minimum_transition_guard_transition_too_fast_rejects," +
                minimumTransitionGapCandidate.transitionTooFastRejects,
        )
        appendLine(
            "minimum_transition_guard_unconfirmed_rejects," +
                minimumTransitionGapCandidate.unconfirmedAlternationRejects,
        )
        appendLine(
            "minimum_transition_guard_missing_rejects," +
                minimumTransitionGapCandidate.missingAlternationRejects,
        )
        appendLine(
            "minimum_transition_guard_bridge_limit_rejects," +
                minimumTransitionGapCandidate.bridgeLimitRejects,
        )
        appendLine("minimum_transition_guard_error,$minimumTransitionGapError")
        appendLine("minimum_transition_guard_absolute_error,$minimumTransitionGapAbsoluteError")
        appendLine(
            "minimum_transition_guard_ground_truth_match," +
                if (minimumTransitionGapMatchesGroundTruth) "PASS" else "FAIL",
        )
        appendLine()
        appendLine(
            "event_index,detector_source,video_timestamp_ms,relative_to_go_ms,foot," +
                "landing_method,counted_right_step,counter_reject_reason",
        )
        landingEvents.sortedWith(
            compareBy<VideoTestLandingEvent> { it.videoTimestampMillis }
                .thenBy { it.detectorSource.name }
                .thenBy { it.foot.name },
        ).forEachIndexed { index, event ->
            appendLine(
                listOf(
                    index + 1,
                    event.detectorSource.name,
                    event.videoTimestampMillis,
                    event.videoTimestampMillis - goTimestampMillis,
                    event.foot.name,
                    event.landingMethod.name,
                    event.countedRightStep,
                    event.counterRejectReason.name,
                ).joinToString(","),
            )
        }
        appendLine()
        appendLine(
            "candidate_event_index,video_timestamp_ms,relative_to_go_ms,foot," +
                "landing_method,counted_right_step,count_after,state_before,state_after," +
                "counter_reject_reason",
        )
        fixedReferenceCandidate.decisions.forEachIndexed { index, decision ->
            appendLine(
                listOf(
                    index + 1,
                    decision.videoTimestampMillis,
                    decision.videoTimestampMillis - goTimestampMillis,
                    decision.landing.name,
                    decision.landingMethod.name,
                    decision.countedRightStep,
                    decision.countAfter,
                    decision.stateBefore.name,
                    decision.stateAfter.name,
                    decision.rejectReason.name,
                ).joinToString(","),
            )
        }
        appendLine()
        appendLine(
            "right_primary_event_index,video_timestamp_ms,relative_to_go_ms,landing_method," +
                "accepted,count_after,interval_since_accepted_ms,reject_reason",
        )
        rightPrimaryCandidate.decisions.forEachIndexed { index, decision ->
            appendLine(
                listOf(
                    index + 1,
                    decision.videoTimestampMillis,
                    decision.videoTimestampMillis - goTimestampMillis,
                    decision.landingMethod.name,
                    decision.accepted,
                    decision.countAfter,
                    decision.intervalSinceAcceptedMillis.orEmptyCsv(),
                    decision.rejectReason.name,
                ).joinToString(","),
            )
        }
        appendLine()
        appendLine(
            "alternation_guard_event_index,video_timestamp_ms,relative_to_go_ms,landing_method," +
                "accepted,count_after,interval_since_accepted_ms,evidence,reject_reason",
        )
        alternationGuardCandidate.decisions.forEachIndexed { index, decision ->
            appendLine(
                listOf(
                    index + 1,
                    decision.videoTimestampMillis,
                    decision.videoTimestampMillis - goTimestampMillis,
                    decision.landingMethod.name,
                    decision.accepted,
                    decision.countAfter,
                    decision.intervalSinceAcceptedMillis.orEmptyCsv(),
                    decision.evidence.name,
                    decision.rejectReason.name,
                ).joinToString(","),
            )
        }
        appendLine()
        appendLine(
            "minimum_transition_guard_event_index,video_timestamp_ms,relative_to_go_ms," +
                "landing_method,accepted,count_after,interval_since_accepted_ms,evidence," +
                "reject_reason",
        )
        minimumTransitionGapCandidate.decisions.forEachIndexed { index, decision ->
            appendLine(
                listOf(
                    index + 1,
                    decision.videoTimestampMillis,
                    decision.videoTimestampMillis - goTimestampMillis,
                    decision.landingMethod.name,
                    decision.accepted,
                    decision.countAfter,
                    decision.intervalSinceAcceptedMillis.orEmptyCsv(),
                    decision.evidence.name,
                    decision.rejectReason.name,
                ).joinToString(","),
            )
        }
    }

    fun summaryLine(): String = String.format(
        Locale.US,
        "Production %d vs fixed-reference %d (%+d)",
        productionRawLandings,
        fixedReferenceRawLandings,
        fixedReferenceDifference,
    )

    private fun csvEscape(value: String): String =
        "\"${value.replace("\"", "\"\"")}\""

    private fun Long?.orEmptyCsv(): String = this?.toString().orEmpty()
}

enum class VideoTestDetectorSource {
    PRODUCTION,
    FIXED_REFERENCE_SHADOW,
}

data class VideoTestLandingEvent(
    val detectorSource: VideoTestDetectorSource,
    val videoTimestampMillis: Long,
    val foot: SpeedLanding,
    val landingMethod: SpeedLandingDetectionMethod,
    val countedRightStep: Boolean = false,
    val counterRejectReason: SpeedRejectReason = SpeedRejectReason.NONE,
)
