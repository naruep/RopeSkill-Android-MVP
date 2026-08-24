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
    val fixedGoPilotLeftLandings: Int,
    val fixedGoPilotRightLandings: Int,
    val fixedGoPilotCountedRightSteps: Int,
    val fixedGoPilotRepeatedLeftRejects: Int,
    val fixedGoPilotRepeatedRightRejects: Int,
    val fixedGoPilotBothFeetRejects: Int,
    val fixedGoPilotUnclearLandingRejects: Int,
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
    val frameTrace: List<VideoTestFrameTrace>,
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
        appendLine("fixed_go_pilot_left_landings,$fixedGoPilotLeftLandings")
        appendLine("fixed_go_pilot_right_landings,$fixedGoPilotRightLandings")
        appendLine("fixed_go_pilot_counted_right_steps,$fixedGoPilotCountedRightSteps")
        appendLine("fixed_go_pilot_repeated_left_rejects,$fixedGoPilotRepeatedLeftRejects")
        appendLine("fixed_go_pilot_repeated_right_rejects,$fixedGoPilotRepeatedRightRejects")
        appendLine("fixed_go_pilot_both_feet_rejects,$fixedGoPilotBothFeetRejects")
        appendLine("fixed_go_pilot_unclear_rejects,$fixedGoPilotUnclearLandingRejects")
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
        appendLine()
        appendLine(
            "frame_index,video_timestamp_ms,relative_to_go_ms,classifier_diagnostic," +
                "tracking_valid,left_ankle_y,left_ankle_visible,right_ankle_y," +
                "right_ankle_visible,left_heel_y,left_heel_visible,right_heel_y," +
                "right_heel_visible,left_toe_y,left_toe_visible,right_toe_y," +
                "right_toe_visible,left_leg_length,right_leg_length," +
                "fixed_left_ground_reference,fixed_right_ground_reference," +
                "production_left_phase_before,production_left_phase_after," +
                "production_left_rise_ratio,production_left_rearm_frames," +
                "production_left_candidate_method,production_left_candidate_outcome," +
                "production_left_candidate_reject_reason,production_right_phase_before," +
                "production_right_phase_after,production_right_rise_ratio," +
                "production_right_rearm_frames,production_right_candidate_method," +
                "production_right_candidate_outcome,production_right_candidate_reject_reason," +
                "fixed_left_phase," +
                "fixed_left_rise_ratio,fixed_left_rearm_frames,fixed_right_phase," +
                "fixed_right_rise_ratio,fixed_right_rearm_frames," +
                "fixed_left_candidate_method,fixed_left_candidate_outcome," +
                "fixed_left_candidate_reject_reason,fixed_right_candidate_method," +
                "fixed_right_candidate_outcome,fixed_right_candidate_reject_reason," +
                "production_events,fixed_reference_events",
        )
        frameTrace.forEach { frame ->
            appendLine(
                listOf(
                    frame.frameIndex,
                    frame.videoTimestampMillis,
                    frame.videoTimestampMillis - goTimestampMillis,
                    frame.classifierDiagnostic.name,
                    frame.trackingValid,
                    frame.pose.leftAnkleY.orEmptyCsv(),
                    frame.pose.leftAnkleVisible,
                    frame.pose.rightAnkleY.orEmptyCsv(),
                    frame.pose.rightAnkleVisible,
                    frame.pose.leftHeelY.orEmptyCsv(),
                    frame.pose.leftHeelVisible,
                    frame.pose.rightHeelY.orEmptyCsv(),
                    frame.pose.rightHeelVisible,
                    frame.pose.leftToeY.orEmptyCsv(),
                    frame.pose.leftToeVisible,
                    frame.pose.rightToeY.orEmptyCsv(),
                    frame.pose.rightToeVisible,
                    frame.pose.leftLegLength.orEmptyCsv(),
                    frame.pose.rightLegLength.orEmptyCsv(),
                    frame.fixedLeftGroundReference.orEmptyCsv(),
                    frame.fixedRightGroundReference.orEmptyCsv(),
                    frame.productionLeftPhaseBefore?.name.orEmpty(),
                    frame.productionLeftPhaseAfter?.name.orEmpty(),
                    frame.productionLeftRiseRatio.orEmptyCsv(),
                    frame.productionLeftRearmFrames.orEmptyCsv(),
                    frame.productionLeftCandidateMethod?.name.orEmpty(),
                    frame.productionLeftCandidateOutcome?.name.orEmpty(),
                    frame.productionLeftCandidateRejectReason?.name.orEmpty(),
                    frame.productionRightPhaseBefore?.name.orEmpty(),
                    frame.productionRightPhaseAfter?.name.orEmpty(),
                    frame.productionRightRiseRatio.orEmptyCsv(),
                    frame.productionRightRearmFrames.orEmptyCsv(),
                    frame.productionRightCandidateMethod?.name.orEmpty(),
                    frame.productionRightCandidateOutcome?.name.orEmpty(),
                    frame.productionRightCandidateRejectReason?.name.orEmpty(),
                    frame.fixedLeftPhase?.name.orEmpty(),
                    frame.fixedLeftRiseRatio.orEmptyCsv(),
                    frame.fixedLeftRearmFrames.orEmptyCsv(),
                    frame.fixedRightPhase?.name.orEmpty(),
                    frame.fixedRightRiseRatio.orEmptyCsv(),
                    frame.fixedRightRearmFrames.orEmptyCsv(),
                    frame.fixedLeftCandidateMethod?.name.orEmpty(),
                    frame.fixedLeftCandidateOutcome?.name.orEmpty(),
                    frame.fixedLeftCandidateRejectReason?.name.orEmpty(),
                    frame.fixedRightCandidateMethod?.name.orEmpty(),
                    frame.fixedRightCandidateOutcome?.name.orEmpty(),
                    frame.fixedRightCandidateRejectReason?.name.orEmpty(),
                    csvEscape(frame.productionEvents.toCsvValue()),
                    csvEscape(frame.fixedReferenceEvents.toCsvValue()),
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

    private fun Int?.orEmptyCsv(): String = this?.toString().orEmpty()

    private fun Float?.orEmptyCsv(): String =
        this?.let { String.format(Locale.US, "%.6f", it) }.orEmpty()

    private fun List<VideoTestFrameLandingEvent>.toCsvValue(): String =
        joinToString("|") { event ->
            listOfNotNull(
                event.videoTimestampMillis.toString(),
                event.foot.name,
                event.landingMethod.name,
                event.counterOutcome?.name,
                event.counterRejectReason?.name,
            ).joinToString(":")
        }
}

enum class VideoTestDetectorSource {
    PRODUCTION,
    FIXED_GO_PILOT,
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

/** Diagnostic-only snapshot from one sampled Video Test frame. */
data class VideoTestFrameTrace(
    val frameIndex: Int,
    val videoTimestampMillis: Long,
    val classifierDiagnostic: SpeedClassifierDiagnostic,
    val trackingValid: Boolean,
    val pose: VideoTestPoseEvidence,
    val fixedLeftGroundReference: Float?,
    val fixedRightGroundReference: Float?,
    val productionLeftPhaseBefore: SpeedFootPhase?,
    val productionLeftPhaseAfter: SpeedFootPhase?,
    val productionLeftRiseRatio: Float?,
    val productionLeftRearmFrames: Int?,
    val productionLeftCandidateMethod: SpeedLandingDetectionMethod?,
    val productionLeftCandidateOutcome: SpeedCandidateOutcome?,
    val productionLeftCandidateRejectReason: SpeedCandidateRejectReason?,
    val productionRightPhaseBefore: SpeedFootPhase?,
    val productionRightPhaseAfter: SpeedFootPhase?,
    val productionRightRiseRatio: Float?,
    val productionRightRearmFrames: Int?,
    val productionRightCandidateMethod: SpeedLandingDetectionMethod?,
    val productionRightCandidateOutcome: SpeedCandidateOutcome?,
    val productionRightCandidateRejectReason: SpeedCandidateRejectReason?,
    val fixedLeftPhase: SpeedFootPhase?,
    val fixedLeftRiseRatio: Float?,
    val fixedLeftRearmFrames: Int?,
    val fixedRightPhase: SpeedFootPhase?,
    val fixedRightRiseRatio: Float?,
    val fixedRightRearmFrames: Int?,
    val fixedLeftCandidateMethod: SpeedLandingDetectionMethod?,
    val fixedLeftCandidateOutcome: SpeedCandidateOutcome?,
    val fixedLeftCandidateRejectReason: SpeedCandidateRejectReason?,
    val fixedRightCandidateMethod: SpeedLandingDetectionMethod?,
    val fixedRightCandidateOutcome: SpeedCandidateOutcome?,
    val fixedRightCandidateRejectReason: SpeedCandidateRejectReason?,
    val productionEvents: List<VideoTestFrameLandingEvent>,
    val fixedReferenceEvents: List<VideoTestFrameLandingEvent>,
)

data class VideoTestFrameLandingEvent(
    val videoTimestampMillis: Long,
    val foot: SpeedLanding,
    val landingMethod: SpeedLandingDetectionMethod,
    val counterOutcome: VideoTestSpeedStepOutcome? = null,
    val counterRejectReason: SpeedRejectReason? = null,
)

enum class VideoTestSpeedStepOutcome {
    COUNTED_RIGHT_STEP,
    ACCEPTED_NON_COUNTING,
    REJECTED,
}

/** Raw Video Test input evidence captured before production classification. */
data class VideoTestPoseEvidence(
    val leftAnkleY: Float?,
    val leftAnkleVisible: Boolean,
    val rightAnkleY: Float?,
    val rightAnkleVisible: Boolean,
    val leftHeelY: Float?,
    val leftHeelVisible: Boolean,
    val rightHeelY: Float?,
    val rightHeelVisible: Boolean,
    val leftToeY: Float?,
    val leftToeVisible: Boolean,
    val rightToeY: Float?,
    val rightToeVisible: Boolean,
    val leftLegLength: Float?,
    val rightLegLength: Float?,
) {
    companion object {
        fun capture(
            frame: PoseFrame,
            leftLegLength: Float? = null,
            rightLegLength: Float? = null,
        ): VideoTestPoseEvidence {
            val leftAnkle = frame.landmarks.getOrNull(LEFT_ANKLE)
            val rightAnkle = frame.landmarks.getOrNull(RIGHT_ANKLE)
            val leftHeel = frame.landmarks.getOrNull(LEFT_HEEL)
            val rightHeel = frame.landmarks.getOrNull(RIGHT_HEEL)
            val leftToe = frame.landmarks.getOrNull(LEFT_FOOT_INDEX)
            val rightToe = frame.landmarks.getOrNull(RIGHT_FOOT_INDEX)
            return VideoTestPoseEvidence(
                leftAnkleY = leftAnkle?.y,
                leftAnkleVisible = leftAnkle?.isVisible == true,
                rightAnkleY = rightAnkle?.y,
                rightAnkleVisible = rightAnkle?.isVisible == true,
                leftHeelY = leftHeel?.y,
                leftHeelVisible = leftHeel?.isVisible == true,
                rightHeelY = rightHeel?.y,
                rightHeelVisible = rightHeel?.isVisible == true,
                leftToeY = leftToe?.y,
                leftToeVisible = leftToe?.isVisible == true,
                rightToeY = rightToe?.y,
                rightToeVisible = rightToe?.isVisible == true,
                leftLegLength = leftLegLength,
                rightLegLength = rightLegLength,
            )
        }
        private const val LEFT_ANKLE = 27
        private const val RIGHT_ANKLE = 28
        private const val LEFT_HEEL = 29
        private const val RIGHT_HEEL = 30
        private const val LEFT_FOOT_INDEX = 31
        private const val RIGHT_FOOT_INDEX = 32
    }
}
