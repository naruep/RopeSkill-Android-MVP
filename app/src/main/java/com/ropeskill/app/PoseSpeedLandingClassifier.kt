package com.ropeskill.app

import java.util.ArrayDeque
import kotlin.math.abs
import kotlin.math.max

enum class SpeedClassifierDiagnostic {
    CALIBRATING,
    READY,
    LOW_VISIBILITY,
    OUT_OF_ORDER_TIMESTAMP,
}

data class SpeedLandingEvent(
    val landing: SpeedLanding,
    val timestampMillis: Long,
    val detectionMethod: SpeedLandingDetectionMethod = SpeedLandingDetectionMethod.NOT_APPLICABLE,
)

enum class SpeedLandingDetectionMethod {
    STRICT,
    CONSERVATIVE_REARM,
    MIXED,
    NOT_APPLICABLE,
}

enum class SpeedGroundReferencePolicy {
    ADAPTIVE,
    FIXED_AFTER_CALIBRATION,
}

data class SpeedFixedReferenceLandingEvent(
    val landing: SpeedLanding,
    val timestampMillis: Long,
    val detectionMethod: SpeedLandingDetectionMethod,
)

data class SpeedLandingClassifierResult(
    val events: List<SpeedLandingEvent>,
    val trackingValid: Boolean,
    val diagnostic: SpeedClassifierDiagnostic,
)

data class SpeedLandingClassifierDiagnostics(
    val validFrames: Long,
    val lowVisibilityFrames: Long,
    val outOfOrderFrames: Long,
    val trackingLossEvents: Int,
    val unclearLandings: Int,
    val calibrationFrames: Int,
    val leftConservativeRearms: Int,
    val rightConservativeRearms: Int,
    val leftNearGroundEvidence: SpeedNearGroundEvidence,
    val rightNearGroundEvidence: SpeedNearGroundEvidence,
    val leftGroundReferenceEvidence: SpeedGroundReferenceEvidence? = null,
    val rightGroundReferenceEvidence: SpeedGroundReferenceEvidence? = null,
    val leftFixedReferenceShadowEvidence: SpeedFixedReferenceShadowEvidence? = null,
    val rightFixedReferenceShadowEvidence: SpeedFixedReferenceShadowEvidence? = null,
    val motionEvidence: SpeedMotionEvidence? = null,
    val currentLeftPhase: SpeedFootPhase? = null,
    val currentRightPhase: SpeedFootPhase? = null,
    val currentFrameEvidence: SpeedClassifierFrameEvidence? = null,
)

enum class SpeedCandidateOutcome {
    NOT_EVALUATED,
    NO_CANDIDATE,
    AIRBORNE_STARTED,
    REJECTED,
    PENDING,
    EMITTED,
    EMITTED_AS_BOTH,
}

enum class SpeedCandidateRejectReason {
    NONE,
    CALIBRATING,
    GROUNDED_BELOW_LIFT_THRESHOLD,
    AIRBORNE_ABOVE_CONSERVATIVE_REARM_THRESHOLD,
    CONSERVATIVE_REARM_FRAMES_PENDING,
}

data class SpeedFootFrameEvidence(
    val side: SpeedLanding,
    val baselineY: Float,
    val groundY: Float,
    val legLength: Float,
    val riseRatio: Float,
    val phaseBefore: SpeedFootPhase,
    val phaseAfter: SpeedFootPhase,
    val conservativeRearmFramesAfter: Int,
    val candidateMethod: SpeedLandingDetectionMethod?,
    val candidateOutcome: SpeedCandidateOutcome,
    val candidateRejectReason: SpeedCandidateRejectReason,
)

data class SpeedClassifierFrameEvidence(
    val timestampMillis: Long,
    val left: SpeedFootFrameEvidence?,
    val right: SpeedFootFrameEvidence?,
)

data class SpeedNearGroundEvidence(
    val currentConsecutiveFrames: Int,
    val maximumConsecutiveFrames: Int,
    val streakStarts: Int,
    val outOfBandBreaks: Int,
    val strictLandingCompletions: Int,
    val trackingLossBreaks: Int,
)

data class SpeedGroundReferenceEvidence(
    val goBaselineY: Float?,
    val currentBaselineY: Float?,
    val currentGroundY: Float?,
    val currentLegLength: Float?,
    val currentBaselineShiftRatio: Float?,
    val maximumAbsoluteBaselineShiftRatio: Float,
    val closestAirborneBaselineY: Float?,
    val closestAirborneGroundY: Float?,
    val closestAirborneLegLength: Float?,
    val closestAirborneGapY: Float?,
    val closestAirborneRiseRatio: Float?,
)

data class SpeedFixedReferenceShadowEvidence(
    val fixedBaselineY: Float?,
    val phase: SpeedFootPhase,
    val currentRiseRatio: Float?,
    val currentConservativeRearmFrames: Int,
    val maximumConservativeRearmFrames: Int,
    val airborneTransitions: Int,
    val strictLandings: Int,
    val conservativeRearms: Int,
    val totalLandings: Int,
    val currentCandidateMethod: SpeedLandingDetectionMethod? = null,
    val currentCandidateOutcome: SpeedCandidateOutcome = SpeedCandidateOutcome.NOT_EVALUATED,
    val currentCandidateRejectReason: SpeedCandidateRejectReason =
        SpeedCandidateRejectReason.NONE,
)

enum class SpeedFootPhase {
    GROUNDED,
    AIRBORNE,
}

data class SpeedFootMotionEvidence(
    val phase: SpeedFootPhase,
    val classificationRiseRatio: Float,
    val currentAirborneDurationMillis: Long,
    val maximumAirborneDurationMillis: Long,
    val currentAirborneMinimumRiseRatio: Float?,
    val longestAirborneMinimumRiseRatio: Float?,
    val currentAverageRiseRatio: Float,
    val maximumAverageRiseRatio: Float,
    val currentAnkleRiseRatio: Float,
    val maximumAnkleRiseRatio: Float,
    val currentHeelRiseRatio: Float,
    val maximumHeelRiseRatio: Float,
    val currentToeRiseRatio: Float,
    val maximumToeRiseRatio: Float,
)

data class SpeedMotionEvidence(
    val timestampMillis: Long,
    val left: SpeedFootMotionEvidence,
    val right: SpeedFootMotionEvidence,
)

/**
 * Converts anatomical MediaPipe foot motion into conservative landing events.
 *
 * Thresholds are pilot hypotheses for T-752 and must be calibrated on the Samsung Galaxy S23
 * Ultra. The classifier intentionally emits no compensating event when a camera frame is skipped.
 */
class PoseSpeedLandingClassifier(
    private val calibrationFramesRequired: Int = DEFAULT_CALIBRATION_FRAMES,
    private val liftRatio: Float = DEFAULT_LIFT_RATIO,
    private val landingRatio: Float = DEFAULT_LANDING_RATIO,
    private val simultaneousWindowMillis: Long = DEFAULT_SIMULTANEOUS_WINDOW_MILLIS,
    private val conservativeRearmRatio: Float = DEFAULT_CONSERVATIVE_REARM_RATIO,
    private val conservativeRearmFramesRequired: Int = DEFAULT_CONSERVATIVE_REARM_FRAMES,
    private val groundReferencePolicy: SpeedGroundReferencePolicy =
        SpeedGroundReferencePolicy.ADAPTIVE,
    private val evidenceEnabled: Boolean = false,
    private val onFixedReferenceLanding: ((SpeedFixedReferenceLandingEvent) -> Unit)? = null,
) {
    private enum class FootPhase { GROUNDED, AIRBORNE }
    private enum class FootSide { LEFT, RIGHT }

    private data class FootSample(
        val ankleY: Float,
        val heelY: Float,
        val toeY: Float,
        val groundY: Float,
        val legLength: Float,
    )

    private data class FootEvidenceBaseline(
        val ankleY: Float,
        val heelY: Float,
        val toeY: Float,
    )

    private data class FootEvidenceMaximums(
        val average: Float = 0f,
        val ankle: Float = 0f,
        val heel: Float = 0f,
        val toe: Float = 0f,
    )

    private data class AirborneEpisodeEvidence(
        var startedAtMillis: Long? = null,
        var minimumRiseRatio: Float? = null,
        var maximumDurationMillis: Long = 0L,
        var longestEpisodeMinimumRiseRatio: Float? = null,
    )

    private data class AirborneEpisodeSnapshot(
        val currentDurationMillis: Long,
        val maximumDurationMillis: Long,
        val currentMinimumRiseRatio: Float?,
        val longestEpisodeMinimumRiseRatio: Float?,
    )

    private class GroundReferenceObserver {
        private var goBaselineY: Float? = null
        private var currentBaselineY: Float? = null
        private var currentGroundY: Float? = null
        private var currentLegLength: Float? = null
        private var currentBaselineShiftRatio: Float? = null
        private var maximumAbsoluteBaselineShiftRatio = 0f
        private var closestAirborneBaselineY: Float? = null
        private var closestAirborneGroundY: Float? = null
        private var closestAirborneLegLength: Float? = null
        private var closestAirborneGapY: Float? = null
        private var closestAirborneRiseRatio: Float? = null

        fun reset(baselineY: Float?) {
            goBaselineY = baselineY
            currentBaselineY = null
            currentGroundY = null
            currentLegLength = null
            currentBaselineShiftRatio = null
            maximumAbsoluteBaselineShiftRatio = 0f
            closestAirborneBaselineY = null
            closestAirborneGroundY = null
            closestAirborneLegLength = null
            closestAirborneGapY = null
            closestAirborneRiseRatio = null
        }

        fun observe(
            baselineY: Float,
            sample: FootSample,
            phase: FootPhase,
            riseRatio: Float,
        ) {
            if (goBaselineY == null) goBaselineY = baselineY
            currentBaselineY = baselineY
            currentGroundY = sample.groundY
            currentLegLength = sample.legLength
            currentBaselineShiftRatio = goBaselineY?.let { (baselineY - it) / sample.legLength }
            currentBaselineShiftRatio?.let {
                maximumAbsoluteBaselineShiftRatio = max(
                    maximumAbsoluteBaselineShiftRatio,
                    abs(it),
                )
            }
            if (
                phase == FootPhase.AIRBORNE &&
                (closestAirborneRiseRatio == null || riseRatio < closestAirborneRiseRatio!!)
            ) {
                closestAirborneBaselineY = baselineY
                closestAirborneGroundY = sample.groundY
                closestAirborneLegLength = sample.legLength
                closestAirborneGapY = baselineY - sample.groundY
                closestAirborneRiseRatio = riseRatio
            }
        }

        fun snapshot() = SpeedGroundReferenceEvidence(
            goBaselineY = goBaselineY,
            currentBaselineY = currentBaselineY,
            currentGroundY = currentGroundY,
            currentLegLength = currentLegLength,
            currentBaselineShiftRatio = currentBaselineShiftRatio,
            maximumAbsoluteBaselineShiftRatio = maximumAbsoluteBaselineShiftRatio,
            closestAirborneBaselineY = closestAirborneBaselineY,
            closestAirborneGroundY = closestAirborneGroundY,
            closestAirborneLegLength = closestAirborneLegLength,
            closestAirborneGapY = closestAirborneGapY,
            closestAirborneRiseRatio = closestAirborneRiseRatio,
        )
    }

    private class FixedReferenceShadowObserver(
        private val liftRatio: Float,
        private val landingRatio: Float,
        private val conservativeRearmRatio: Float,
        private val conservativeRearmFramesRequired: Int,
    ) {
        private var fixedBaselineY: Float? = null
        private var phase = FootPhase.GROUNDED
        private var currentRiseRatio: Float? = null
        private var conservativeRearmFrames = 0
        private var maximumConservativeRearmFrames = 0
        private var airborneTransitions = 0
        private var strictLandings = 0
        private var conservativeRearms = 0
        private var currentCandidateMethod: SpeedLandingDetectionMethod? = null
        private var currentCandidateOutcome = SpeedCandidateOutcome.NOT_EVALUATED
        private var currentCandidateRejectReason = SpeedCandidateRejectReason.NONE

        fun reset(baselineY: Float?) {
            fixedBaselineY = baselineY
            phase = FootPhase.GROUNDED
            currentRiseRatio = null
            conservativeRearmFrames = 0
            maximumConservativeRearmFrames = 0
            airborneTransitions = 0
            strictLandings = 0
            conservativeRearms = 0
            currentCandidateMethod = null
            currentCandidateOutcome = SpeedCandidateOutcome.NOT_EVALUATED
            currentCandidateRejectReason = SpeedCandidateRejectReason.NONE
        }

        fun resetMotion() {
            phase = FootPhase.GROUNDED
            currentRiseRatio = null
            conservativeRearmFrames = 0
            currentCandidateMethod = null
            currentCandidateOutcome = SpeedCandidateOutcome.NOT_EVALUATED
            currentCandidateRejectReason = SpeedCandidateRejectReason.NONE
        }

        fun observe(sample: FootSample): SpeedLandingDetectionMethod? {
            currentCandidateMethod = null
            currentCandidateOutcome = SpeedCandidateOutcome.NOT_EVALUATED
            currentCandidateRejectReason = SpeedCandidateRejectReason.NONE
            val baseline = fixedBaselineY ?: return null
            val riseRatio = (baseline - sample.groundY) / sample.legLength
            currentRiseRatio = riseRatio
            return when (phase) {
                FootPhase.GROUNDED -> {
                    conservativeRearmFrames = 0
                    if (riseRatio >= liftRatio) {
                        phase = FootPhase.AIRBORNE
                        airborneTransitions += 1
                        currentCandidateOutcome = SpeedCandidateOutcome.AIRBORNE_STARTED
                    } else {
                        currentCandidateOutcome = SpeedCandidateOutcome.NO_CANDIDATE
                        currentCandidateRejectReason =
                            SpeedCandidateRejectReason.GROUNDED_BELOW_LIFT_THRESHOLD
                    }
                    null
                }
                FootPhase.AIRBORNE -> when {
                    riseRatio <= landingRatio -> {
                        conservativeRearmFrames = 0
                        phase = FootPhase.GROUNDED
                        strictLandings += 1
                        currentCandidateMethod = SpeedLandingDetectionMethod.STRICT
                        currentCandidateOutcome = SpeedCandidateOutcome.EMITTED
                        SpeedLandingDetectionMethod.STRICT
                    }
                    riseRatio <= conservativeRearmRatio -> {
                        currentCandidateMethod = SpeedLandingDetectionMethod.CONSERVATIVE_REARM
                        conservativeRearmFrames += 1
                        maximumConservativeRearmFrames = max(
                            maximumConservativeRearmFrames,
                            conservativeRearmFrames,
                        )
                        if (conservativeRearmFrames >= conservativeRearmFramesRequired) {
                            conservativeRearmFrames = 0
                            phase = FootPhase.GROUNDED
                            conservativeRearms += 1
                            currentCandidateOutcome = SpeedCandidateOutcome.EMITTED
                            SpeedLandingDetectionMethod.CONSERVATIVE_REARM
                        } else {
                            currentCandidateOutcome = SpeedCandidateOutcome.REJECTED
                            currentCandidateRejectReason =
                                SpeedCandidateRejectReason.CONSERVATIVE_REARM_FRAMES_PENDING
                            null
                        }
                    }
                    else -> {
                        conservativeRearmFrames = 0
                        currentCandidateOutcome = SpeedCandidateOutcome.NO_CANDIDATE
                        currentCandidateRejectReason =
                            SpeedCandidateRejectReason.AIRBORNE_ABOVE_CONSERVATIVE_REARM_THRESHOLD
                        null
                    }
                }
            }
        }

        fun snapshot() = SpeedFixedReferenceShadowEvidence(
            fixedBaselineY = fixedBaselineY,
            phase = if (phase == FootPhase.GROUNDED) {
                SpeedFootPhase.GROUNDED
            } else {
                SpeedFootPhase.AIRBORNE
            },
            currentRiseRatio = currentRiseRatio,
            currentConservativeRearmFrames = conservativeRearmFrames,
            maximumConservativeRearmFrames = maximumConservativeRearmFrames,
            airborneTransitions = airborneTransitions,
            strictLandings = strictLandings,
            conservativeRearms = conservativeRearms,
            totalLandings = strictLandings + conservativeRearms,
            currentCandidateMethod = currentCandidateMethod,
            currentCandidateOutcome = currentCandidateOutcome,
            currentCandidateRejectReason = currentCandidateRejectReason,
        )
    }

    private data class PendingLanding(
        val side: FootSide,
        val timestampMillis: Long,
        val detectionMethod: SpeedLandingDetectionMethod,
    )

    private data class FootUpdate(
        val landed: Boolean = false,
        val conservativeRearm: Boolean = false,
    )

    private var calibrationFrames = 0
    private var leftBaselineY: Float? = null
    private var rightBaselineY: Float? = null
    private var leftEvidenceBaseline: FootEvidenceBaseline? = null
    private var rightEvidenceBaseline: FootEvidenceBaseline? = null
    private var leftEvidenceMaximums = FootEvidenceMaximums()
    private var rightEvidenceMaximums = FootEvidenceMaximums()
    private var motionEvidence: SpeedMotionEvidence? = null
    private var currentFrameEvidence: SpeedClassifierFrameEvidence? = null
    private val leftAirborneEvidence = AirborneEpisodeEvidence()
    private val rightAirborneEvidence = AirborneEpisodeEvidence()
    private val leftGroundReferenceObserver = GroundReferenceObserver()
    private val rightGroundReferenceObserver = GroundReferenceObserver()
    private val leftFixedReferenceShadowObserver = FixedReferenceShadowObserver(
        liftRatio,
        landingRatio,
        conservativeRearmRatio,
        conservativeRearmFramesRequired,
    )
    private val rightFixedReferenceShadowObserver = FixedReferenceShadowObserver(
        liftRatio,
        landingRatio,
        conservativeRearmRatio,
        conservativeRearmFramesRequired,
    )
    private var leftPhase = FootPhase.GROUNDED
    private var rightPhase = FootPhase.GROUNDED
    private var pendingLanding: PendingLanding? = null
    private var lastTimestampMillis: Long? = null
    private var trackingWasValid = false
    private var validFrames = 0L
    private var lowVisibilityFrames = 0L
    private var outOfOrderFrames = 0L
    private var trackingLossEvents = 0
    private var unclearLandings = 0
    private var leftConservativeRearms = 0
    private var rightConservativeRearms = 0
    private var leftConservativeRearmFrames = 0
    private var rightConservativeRearmFrames = 0
    private var leftMaximumConservativeRearmFrames = 0
    private var rightMaximumConservativeRearmFrames = 0
    private var leftNearGroundStreakStarts = 0
    private var rightNearGroundStreakStarts = 0
    private var leftNearGroundOutOfBandBreaks = 0
    private var rightNearGroundOutOfBandBreaks = 0
    private var leftNearGroundStrictLandingCompletions = 0
    private var rightNearGroundStrictLandingCompletions = 0
    private var leftNearGroundTrackingLossBreaks = 0
    private var rightNearGroundTrackingLossBreaks = 0
    private var preGoCalibrationActive = false
    private val preGoLeftSamples = ArrayDeque<FootSample>(PRE_GO_CALIBRATION_FRAMES)
    private val preGoRightSamples = ArrayDeque<FootSample>(PRE_GO_CALIBRATION_FRAMES)

    init {
        require(calibrationFramesRequired > 0)
        require(liftRatio > landingRatio && landingRatio > 0f)
        require(conservativeRearmRatio > landingRatio && conservativeRearmRatio < liftRatio)
        require(conservativeRearmFramesRequired >= 2)
        require(simultaneousWindowMillis >= 0L)
    }

    fun process(frame: PoseFrame): SpeedLandingClassifierResult {
        val timestampMillis = frame.sourceTimestampMillis
        val previousTimestamp = lastTimestampMillis
        if (timestampMillis <= 0L || (previousTimestamp != null && timestampMillis <= previousTimestamp)) {
            outOfOrderFrames += 1L
            if (evidenceEnabled) {
                currentFrameEvidence = SpeedClassifierFrameEvidence(timestampMillis, null, null)
            }
            return SpeedLandingClassifierResult(
                events = emptyList(),
                trackingValid = false,
                diagnostic = SpeedClassifierDiagnostic.OUT_OF_ORDER_TIMESTAMP,
            )
        }
        lastTimestampMillis = timestampMillis

        val left = footSample(frame, LEFT_HIP, LEFT_ANKLE, LEFT_HEEL, LEFT_FOOT_INDEX)
        val right = footSample(frame, RIGHT_HIP, RIGHT_ANKLE, RIGHT_HEEL, RIGHT_FOOT_INDEX)
        if (left == null || right == null) {
            lowVisibilityFrames += 1L
            if (evidenceEnabled) {
                currentFrameEvidence = SpeedClassifierFrameEvidence(timestampMillis, null, null)
            }
            val events = mutableListOf<SpeedLandingEvent>()
            if (trackingWasValid) trackingLossEvents += 1
            if (
                leftPhase == FootPhase.AIRBORNE ||
                rightPhase == FootPhase.AIRBORNE ||
                pendingLanding != null
            ) {
                unclearLandings += 1
                events += SpeedLandingEvent(SpeedLanding.UNCLEAR, timestampMillis)
            }
            trackingWasValid = false
            if (evidenceEnabled) recordNearGroundTrackingLossBreaks()
            resetMotionState()
            return SpeedLandingClassifierResult(
                events = events,
                trackingValid = false,
                diagnostic = SpeedClassifierDiagnostic.LOW_VISIBILITY,
            )
        }

        trackingWasValid = true
        validFrames += 1L
        collectPreGoSample(left, right)
        val events = flushExpiredPending(timestampMillis).toMutableList()
        if (calibrationFrames < calibrationFramesRequired) {
            leftBaselineY = calibrateBaseline(leftBaselineY, left.groundY)
            rightBaselineY = calibrateBaseline(rightBaselineY, right.groundY)
            if (evidenceEnabled) {
                leftEvidenceBaseline = calibrateEvidenceBaseline(leftEvidenceBaseline, left)
                rightEvidenceBaseline = calibrateEvidenceBaseline(rightEvidenceBaseline, right)
            }
            calibrationFrames += 1
            if (evidenceEnabled) {
                currentFrameEvidence = SpeedClassifierFrameEvidence(timestampMillis, null, null)
            }
            return SpeedLandingClassifierResult(
                events = events,
                trackingValid = true,
                diagnostic = SpeedClassifierDiagnostic.CALIBRATING,
            )
        }

        val leftClassificationBaseline = leftBaselineY ?: left.groundY
        val rightClassificationBaseline = rightBaselineY ?: right.groundY
        val leftClassificationRiseRatio = classificationRiseRatio(leftClassificationBaseline, left)
        val rightClassificationRiseRatio = classificationRiseRatio(rightClassificationBaseline, right)
        if (evidenceEnabled) {
            leftGroundReferenceObserver.observe(
                baselineY = leftClassificationBaseline,
                sample = left,
                phase = leftPhase,
                riseRatio = leftClassificationRiseRatio,
            )
            rightGroundReferenceObserver.observe(
                baselineY = rightClassificationBaseline,
                sample = right,
                phase = rightPhase,
                riseRatio = rightClassificationRiseRatio,
            )
            leftFixedReferenceShadowObserver.observe(left)?.let { method ->
                onFixedReferenceLanding?.invoke(
                    SpeedFixedReferenceLandingEvent(
                        landing = SpeedLanding.LEFT,
                        timestampMillis = timestampMillis,
                        detectionMethod = method,
                    ),
                )
            }
            rightFixedReferenceShadowObserver.observe(right)?.let { method ->
                onFixedReferenceLanding?.invoke(
                    SpeedFixedReferenceLandingEvent(
                        landing = SpeedLanding.RIGHT,
                        timestampMillis = timestampMillis,
                        detectionMethod = method,
                    ),
                )
            }
            observeNearGroundEvidence(
                riseRatio = leftClassificationRiseRatio,
                phase = leftPhase,
                conservativeRearmFrames = leftConservativeRearmFrames,
                onStreakStarted = { leftNearGroundStreakStarts += 1 },
                onMaximumChanged = { leftMaximumConservativeRearmFrames = it },
                maximumConservativeRearmFrames = leftMaximumConservativeRearmFrames,
                onOutOfBandBreak = { leftNearGroundOutOfBandBreaks += 1 },
                onStrictLandingCompletion = { leftNearGroundStrictLandingCompletions += 1 },
            )
            observeNearGroundEvidence(
                riseRatio = rightClassificationRiseRatio,
                phase = rightPhase,
                conservativeRearmFrames = rightConservativeRearmFrames,
                onStreakStarted = { rightNearGroundStreakStarts += 1 },
                onMaximumChanged = { rightMaximumConservativeRearmFrames = it },
                maximumConservativeRearmFrames = rightMaximumConservativeRearmFrames,
                onOutOfBandBreak = { rightNearGroundOutOfBandBreaks += 1 },
                onStrictLandingCompletion = { rightNearGroundStrictLandingCompletions += 1 },
            )
        }
        val leftPhaseBefore = leftPhase
        val rightPhaseBefore = rightPhase
        val leftUpdate = updateFoot(
            sample = left,
            baseline = leftClassificationBaseline,
            phase = leftPhase,
            onPhaseChanged = { leftPhase = it },
            onBaselineChanged = { leftBaselineY = it },
            conservativeRearmFrames = leftConservativeRearmFrames,
            onConservativeRearmFramesChanged = { leftConservativeRearmFrames = it },
        )
        val rightUpdate = updateFoot(
            sample = right,
            baseline = rightClassificationBaseline,
            phase = rightPhase,
            onPhaseChanged = { rightPhase = it },
            onBaselineChanged = { rightBaselineY = it },
            conservativeRearmFrames = rightConservativeRearmFrames,
            onConservativeRearmFramesChanged = { rightConservativeRearmFrames = it },
        )
        if (leftUpdate.conservativeRearm) leftConservativeRearms += 1
        if (rightUpdate.conservativeRearm) rightConservativeRearms += 1
        updateMotionEvidence(
            timestampMillis = frame.sourceTimestampMillis,
            left = left,
            right = right,
            leftClassificationRiseRatio = leftClassificationRiseRatio,
            rightClassificationRiseRatio = rightClassificationRiseRatio,
        )

        var leftFrameEvidence = if (evidenceEnabled) {
            footFrameEvidence(
                side = SpeedLanding.LEFT,
                sample = left,
                baseline = leftClassificationBaseline,
                riseRatio = leftClassificationRiseRatio,
                phaseBefore = leftPhaseBefore,
                phaseAfter = leftPhase,
                conservativeRearmFramesAfter = leftConservativeRearmFrames,
                update = leftUpdate,
            )
        } else {
            null
        }
        var rightFrameEvidence = if (evidenceEnabled) {
            footFrameEvidence(
                side = SpeedLanding.RIGHT,
                sample = right,
                baseline = rightClassificationBaseline,
                riseRatio = rightClassificationRiseRatio,
                phaseBefore = rightPhaseBefore,
                phaseAfter = rightPhase,
                conservativeRearmFramesAfter = rightConservativeRearmFrames,
                update = rightUpdate,
            )
        } else {
            null
        }

        when {
            leftUpdate.landed && rightUpdate.landed -> {
                pendingLanding = null
                events += SpeedLandingEvent(
                    landing = SpeedLanding.BOTH,
                    timestampMillis = timestampMillis,
                    detectionMethod = combinedDetectionMethod(leftUpdate, rightUpdate),
                )
                leftFrameEvidence = leftFrameEvidence?.copy(
                    candidateOutcome = SpeedCandidateOutcome.EMITTED_AS_BOTH,
                )
                rightFrameEvidence = rightFrameEvidence?.copy(
                    candidateOutcome = SpeedCandidateOutcome.EMITTED_AS_BOTH,
                )
            }
            leftUpdate.landed -> {
                val mergesAsBoth = pendingLanding?.let { pending ->
                    pending.side != FootSide.LEFT &&
                        timestampMillis - pending.timestampMillis <= simultaneousWindowMillis
                } == true
                collectLanding(
                    FootSide.LEFT,
                    timestampMillis,
                    leftUpdate.detectionMethod(),
                    events,
                )
                leftFrameEvidence = leftFrameEvidence?.copy(
                    candidateOutcome = if (mergesAsBoth) {
                        SpeedCandidateOutcome.EMITTED_AS_BOTH
                    } else {
                        SpeedCandidateOutcome.PENDING
                    },
                )
            }
            rightUpdate.landed -> {
                val mergesAsBoth = pendingLanding?.let { pending ->
                    pending.side != FootSide.RIGHT &&
                        timestampMillis - pending.timestampMillis <= simultaneousWindowMillis
                } == true
                collectLanding(
                    FootSide.RIGHT,
                    timestampMillis,
                    rightUpdate.detectionMethod(),
                    events,
                )
                rightFrameEvidence = rightFrameEvidence?.copy(
                    candidateOutcome = if (mergesAsBoth) {
                        SpeedCandidateOutcome.EMITTED_AS_BOTH
                    } else {
                        SpeedCandidateOutcome.PENDING
                    },
                )
            }
        }
        if (evidenceEnabled) {
            currentFrameEvidence = SpeedClassifierFrameEvidence(
                timestampMillis = timestampMillis,
                left = leftFrameEvidence,
                right = rightFrameEvidence,
            )
        }
        return SpeedLandingClassifierResult(
            events = events,
            trackingValid = true,
            diagnostic = SpeedClassifierDiagnostic.READY,
        )
    }

    fun flushPending(): List<SpeedLandingEvent> {
        val pending = pendingLanding ?: return emptyList()
        pendingLanding = null
        return listOf(pending.toEvent())
    }

    fun reset() {
        calibrationFrames = 0
        leftBaselineY = null
        rightBaselineY = null
        leftEvidenceBaseline = null
        rightEvidenceBaseline = null
        lastTimestampMillis = null
        trackingWasValid = false
        validFrames = 0L
        lowVisibilityFrames = 0L
        outOfOrderFrames = 0L
        trackingLossEvents = 0
        unclearLandings = 0
        leftConservativeRearms = 0
        rightConservativeRearms = 0
        cancelPreGoCalibration()
        resetEvidenceWindow()
        resetMotionState()
    }

    /** Starts a bounded rolling baseline window for the final valid Countdown frames. */
    fun startPreGoCalibration() {
        preGoCalibrationActive = true
        preGoLeftSamples.clear()
        preGoRightSamples.clear()
    }

    /**
     * Re-anchors the baseline to the latest valid Countdown frames and starts GO grounded.
     * Existing diagnostic totals and source timestamp ordering are intentionally preserved.
     */
    fun commitPreGoCalibration() {
        if (preGoCalibrationActive && preGoLeftSamples.isNotEmpty()) {
            leftBaselineY = preGoLeftSamples.maxOf { it.groundY }
            rightBaselineY = preGoRightSamples.maxOf { it.groundY }
            if (evidenceEnabled) {
                leftEvidenceBaseline = evidenceBaseline(preGoLeftSamples)
                rightEvidenceBaseline = evidenceBaseline(preGoRightSamples)
            }
        }
        cancelPreGoCalibration()
        resetMotionState()
        resetEvidenceWindow()
    }

    fun cancelPreGoCalibration() {
        preGoCalibrationActive = false
        preGoLeftSamples.clear()
        preGoRightSamples.clear()
    }

    /** Clears bounded peak evidence at GO without changing calibration or detector motion state. */
    fun resetEvidenceWindow() {
        leftEvidenceMaximums = FootEvidenceMaximums()
        rightEvidenceMaximums = FootEvidenceMaximums()
        leftAirborneEvidence.reset()
        rightAirborneEvidence.reset()
        leftConservativeRearms = 0
        rightConservativeRearms = 0
        leftMaximumConservativeRearmFrames = 0
        rightMaximumConservativeRearmFrames = 0
        leftNearGroundStreakStarts = 0
        rightNearGroundStreakStarts = 0
        leftNearGroundOutOfBandBreaks = 0
        rightNearGroundOutOfBandBreaks = 0
        leftNearGroundStrictLandingCompletions = 0
        rightNearGroundStrictLandingCompletions = 0
        leftNearGroundTrackingLossBreaks = 0
        rightNearGroundTrackingLossBreaks = 0
        leftGroundReferenceObserver.reset(leftBaselineY)
        rightGroundReferenceObserver.reset(rightBaselineY)
        leftFixedReferenceShadowObserver.reset(leftBaselineY)
        rightFixedReferenceShadowObserver.reset(rightBaselineY)
        motionEvidence = null
        currentFrameEvidence = null
    }

    fun diagnostics() = SpeedLandingClassifierDiagnostics(
        validFrames = validFrames,
        lowVisibilityFrames = lowVisibilityFrames,
        outOfOrderFrames = outOfOrderFrames,
        trackingLossEvents = trackingLossEvents,
        unclearLandings = unclearLandings,
        calibrationFrames = calibrationFrames,
        leftConservativeRearms = leftConservativeRearms,
        rightConservativeRearms = rightConservativeRearms,
        leftNearGroundEvidence = nearGroundEvidence(
            currentFrames = leftConservativeRearmFrames,
            maximumFrames = leftMaximumConservativeRearmFrames,
            streakStarts = leftNearGroundStreakStarts,
            outOfBandBreaks = leftNearGroundOutOfBandBreaks,
            strictLandingCompletions = leftNearGroundStrictLandingCompletions,
            trackingLossBreaks = leftNearGroundTrackingLossBreaks,
        ),
        rightNearGroundEvidence = nearGroundEvidence(
            currentFrames = rightConservativeRearmFrames,
            maximumFrames = rightMaximumConservativeRearmFrames,
            streakStarts = rightNearGroundStreakStarts,
            outOfBandBreaks = rightNearGroundOutOfBandBreaks,
            strictLandingCompletions = rightNearGroundStrictLandingCompletions,
            trackingLossBreaks = rightNearGroundTrackingLossBreaks,
        ),
        leftGroundReferenceEvidence = if (evidenceEnabled) {
            leftGroundReferenceObserver.snapshot()
        } else {
            null
        },
        rightGroundReferenceEvidence = if (evidenceEnabled) {
            rightGroundReferenceObserver.snapshot()
        } else {
            null
        },
        leftFixedReferenceShadowEvidence = if (evidenceEnabled) {
            leftFixedReferenceShadowObserver.snapshot()
        } else {
            null
        },
        rightFixedReferenceShadowEvidence = if (evidenceEnabled) {
            rightFixedReferenceShadowObserver.snapshot()
        } else {
            null
        },
        motionEvidence = motionEvidence,
        currentLeftPhase = if (evidenceEnabled) leftPhase.toDiagnosticPhase() else null,
        currentRightPhase = if (evidenceEnabled) rightPhase.toDiagnosticPhase() else null,
        currentFrameEvidence = if (evidenceEnabled) currentFrameEvidence else null,
    )

    private fun FootPhase.toDiagnosticPhase() = if (this == FootPhase.GROUNDED) {
        SpeedFootPhase.GROUNDED
    } else {
        SpeedFootPhase.AIRBORNE
    }

    private fun footFrameEvidence(
        side: SpeedLanding,
        sample: FootSample,
        baseline: Float,
        riseRatio: Float,
        phaseBefore: FootPhase,
        phaseAfter: FootPhase,
        conservativeRearmFramesAfter: Int,
        update: FootUpdate,
    ): SpeedFootFrameEvidence {
        val candidateMethod = when {
            update.landed -> update.detectionMethod()
            phaseBefore == FootPhase.AIRBORNE && riseRatio <= conservativeRearmRatio ->
                SpeedLandingDetectionMethod.CONSERVATIVE_REARM
            else -> null
        }
        val candidateOutcome: SpeedCandidateOutcome
        val candidateRejectReason: SpeedCandidateRejectReason
        when {
            update.landed -> {
                candidateOutcome = SpeedCandidateOutcome.PENDING
                candidateRejectReason = SpeedCandidateRejectReason.NONE
            }
            phaseBefore == FootPhase.GROUNDED && phaseAfter == FootPhase.AIRBORNE -> {
                candidateOutcome = SpeedCandidateOutcome.AIRBORNE_STARTED
                candidateRejectReason = SpeedCandidateRejectReason.NONE
            }
            phaseBefore == FootPhase.GROUNDED -> {
                candidateOutcome = SpeedCandidateOutcome.NO_CANDIDATE
                candidateRejectReason =
                    SpeedCandidateRejectReason.GROUNDED_BELOW_LIFT_THRESHOLD
            }
            riseRatio <= conservativeRearmRatio -> {
                candidateOutcome = SpeedCandidateOutcome.REJECTED
                candidateRejectReason =
                    SpeedCandidateRejectReason.CONSERVATIVE_REARM_FRAMES_PENDING
            }
            else -> {
                candidateOutcome = SpeedCandidateOutcome.NO_CANDIDATE
                candidateRejectReason =
                    SpeedCandidateRejectReason.AIRBORNE_ABOVE_CONSERVATIVE_REARM_THRESHOLD
            }
        }
        return SpeedFootFrameEvidence(
            side = side,
            baselineY = baseline,
            groundY = sample.groundY,
            legLength = sample.legLength,
            riseRatio = riseRatio,
            phaseBefore = phaseBefore.toDiagnosticPhase(),
            phaseAfter = phaseAfter.toDiagnosticPhase(),
            conservativeRearmFramesAfter = conservativeRearmFramesAfter,
            candidateMethod = candidateMethod,
            candidateOutcome = candidateOutcome,
            candidateRejectReason = candidateRejectReason,
        )
    }

    private fun observeNearGroundEvidence(
        riseRatio: Float,
        phase: FootPhase,
        conservativeRearmFrames: Int,
        onStreakStarted: () -> Unit,
        maximumConservativeRearmFrames: Int,
        onMaximumChanged: (Int) -> Unit,
        onOutOfBandBreak: () -> Unit,
        onStrictLandingCompletion: () -> Unit,
    ) {
        if (phase != FootPhase.AIRBORNE) return
        when {
            riseRatio <= landingRatio -> {
                if (conservativeRearmFrames > 0) onStrictLandingCompletion()
            }
            riseRatio <= conservativeRearmRatio -> {
                if (conservativeRearmFrames == 0) onStreakStarted()
                val nextFrames = conservativeRearmFrames + 1
                if (nextFrames > maximumConservativeRearmFrames) onMaximumChanged(nextFrames)
            }
            conservativeRearmFrames > 0 -> onOutOfBandBreak()
        }
    }

    private fun recordNearGroundTrackingLossBreaks() {
        if (leftConservativeRearmFrames > 0) leftNearGroundTrackingLossBreaks += 1
        if (rightConservativeRearmFrames > 0) rightNearGroundTrackingLossBreaks += 1
    }

    private fun nearGroundEvidence(
        currentFrames: Int,
        maximumFrames: Int,
        streakStarts: Int,
        outOfBandBreaks: Int,
        strictLandingCompletions: Int,
        trackingLossBreaks: Int,
    ) = SpeedNearGroundEvidence(
        currentConsecutiveFrames = currentFrames,
        maximumConsecutiveFrames = maximumFrames,
        streakStarts = streakStarts,
        outOfBandBreaks = outOfBandBreaks,
        strictLandingCompletions = strictLandingCompletions,
        trackingLossBreaks = trackingLossBreaks,
    )

    private fun updateFoot(
        sample: FootSample,
        baseline: Float,
        phase: FootPhase,
        onPhaseChanged: (FootPhase) -> Unit,
        onBaselineChanged: (Float) -> Unit,
        conservativeRearmFrames: Int,
        onConservativeRearmFramesChanged: (Int) -> Unit,
    ): FootUpdate {
        val rise = baseline - sample.groundY
        return when (phase) {
            FootPhase.GROUNDED -> {
                onConservativeRearmFramesChanged(0)
                if (rise >= sample.legLength * liftRatio) {
                    onPhaseChanged(FootPhase.AIRBORNE)
                } else {
                    onBaselineChanged(adaptGroundBaseline(baseline, sample.groundY))
                }
                FootUpdate()
            }
            FootPhase.AIRBORNE -> {
                if (rise <= sample.legLength * landingRatio) {
                    onConservativeRearmFramesChanged(0)
                    onPhaseChanged(FootPhase.GROUNDED)
                    onBaselineChanged(adaptGroundBaseline(baseline, sample.groundY))
                    FootUpdate(landed = true)
                } else if (rise <= sample.legLength * conservativeRearmRatio) {
                    val nearGroundFrames = conservativeRearmFrames + 1
                    if (nearGroundFrames >= conservativeRearmFramesRequired) {
                        onConservativeRearmFramesChanged(0)
                        onPhaseChanged(FootPhase.GROUNDED)
                        onBaselineChanged(sample.groundY)
                        FootUpdate(landed = true, conservativeRearm = true)
                    } else {
                        onConservativeRearmFramesChanged(nearGroundFrames)
                        FootUpdate()
                    }
                } else {
                    onConservativeRearmFramesChanged(0)
                    FootUpdate()
                }
            }
        }
    }

    private fun collectLanding(
        side: FootSide,
        timestampMillis: Long,
        detectionMethod: SpeedLandingDetectionMethod,
        events: MutableList<SpeedLandingEvent>,
    ) {
        val pending = pendingLanding
        if (
            pending != null &&
            pending.side != side &&
            timestampMillis - pending.timestampMillis <= simultaneousWindowMillis
        ) {
            pendingLanding = null
            events += SpeedLandingEvent(
                landing = SpeedLanding.BOTH,
                timestampMillis = timestampMillis,
                detectionMethod = combineDetectionMethods(
                    pending.detectionMethod,
                    detectionMethod,
                ),
            )
        } else {
            if (pending != null) events += pending.toEvent()
            pendingLanding = PendingLanding(side, timestampMillis, detectionMethod)
        }
    }

    private fun flushExpiredPending(timestampMillis: Long): List<SpeedLandingEvent> {
        val pending = pendingLanding ?: return emptyList()
        if (timestampMillis - pending.timestampMillis <= simultaneousWindowMillis) return emptyList()
        pendingLanding = null
        return listOf(pending.toEvent())
    }

    private fun PendingLanding.toEvent() = SpeedLandingEvent(
        landing = if (side == FootSide.LEFT) SpeedLanding.LEFT else SpeedLanding.RIGHT,
        timestampMillis = timestampMillis,
        detectionMethod = detectionMethod,
    )

    private fun FootUpdate.detectionMethod() = if (conservativeRearm) {
        SpeedLandingDetectionMethod.CONSERVATIVE_REARM
    } else {
        SpeedLandingDetectionMethod.STRICT
    }

    private fun combinedDetectionMethod(
        left: FootUpdate,
        right: FootUpdate,
    ) = combineDetectionMethods(left.detectionMethod(), right.detectionMethod())

    private fun combineDetectionMethods(
        first: SpeedLandingDetectionMethod,
        second: SpeedLandingDetectionMethod,
    ) = if (first == second) first else SpeedLandingDetectionMethod.MIXED

    private fun resetMotionState() {
        leftPhase = FootPhase.GROUNDED
        rightPhase = FootPhase.GROUNDED
        pendingLanding = null
        leftConservativeRearmFrames = 0
        rightConservativeRearmFrames = 0
        leftAirborneEvidence.cancelCurrentEpisode()
        rightAirborneEvidence.cancelCurrentEpisode()
        leftFixedReferenceShadowObserver.resetMotion()
        rightFixedReferenceShadowObserver.resetMotion()
    }

    private fun collectPreGoSample(left: FootSample, right: FootSample) {
        if (!preGoCalibrationActive) return
        addBounded(preGoLeftSamples, left)
        addBounded(preGoRightSamples, right)
    }

    private fun addBounded(samples: ArrayDeque<FootSample>, sample: FootSample) {
        if (samples.size == PRE_GO_CALIBRATION_FRAMES) samples.removeFirst()
        samples.addLast(sample)
    }

    private fun evidenceBaseline(samples: ArrayDeque<FootSample>) = FootEvidenceBaseline(
        ankleY = samples.maxOf { it.ankleY },
        heelY = samples.maxOf { it.heelY },
        toeY = samples.maxOf { it.toeY },
    )

    private fun footSample(
        frame: PoseFrame,
        hipIndex: Int,
        ankleIndex: Int,
        heelIndex: Int,
        footIndex: Int,
    ): FootSample? {
        val hip = frame.landmarks.visible(hipIndex) ?: return null
        val ankle = frame.landmarks.visible(ankleIndex) ?: return null
        val heel = frame.landmarks.visible(heelIndex) ?: return null
        val foot = frame.landmarks.visible(footIndex) ?: return null
        val groundY = (ankle.y + heel.y + foot.y) / 3f
        val legLength = max(abs(groundY - hip.y), MIN_LEG_LENGTH)
        return FootSample(
            ankleY = ankle.y,
            heelY = heel.y,
            toeY = foot.y,
            groundY = groundY,
            legLength = legLength,
        )
    }

    private fun updateMotionEvidence(
        timestampMillis: Long,
        left: FootSample,
        right: FootSample,
        leftClassificationRiseRatio: Float,
        rightClassificationRiseRatio: Float,
    ) {
        if (!evidenceEnabled) return
        val leftBaseline = leftEvidenceBaseline ?: return
        val rightBaseline = rightEvidenceBaseline ?: return
        val leftEvidence = footMotionEvidence(
            sample = left,
            baseline = leftBaseline,
            phase = leftPhase,
            classificationRiseRatio = leftClassificationRiseRatio,
            timestampMillis = timestampMillis,
            airborneEvidence = leftAirborneEvidence,
            previousMaximums = leftEvidenceMaximums,
        )
        val rightEvidence = footMotionEvidence(
            sample = right,
            baseline = rightBaseline,
            phase = rightPhase,
            classificationRiseRatio = rightClassificationRiseRatio,
            timestampMillis = timestampMillis,
            airborneEvidence = rightAirborneEvidence,
            previousMaximums = rightEvidenceMaximums,
        )
        leftEvidenceMaximums = leftEvidence.maximums
        rightEvidenceMaximums = rightEvidence.maximums
        motionEvidence = SpeedMotionEvidence(
            timestampMillis = timestampMillis,
            left = leftEvidence.evidence,
            right = rightEvidence.evidence,
        )

        if (leftPhase == FootPhase.GROUNDED) {
            leftEvidenceBaseline = adaptEvidenceBaseline(leftBaseline, left)
        }
        if (rightPhase == FootPhase.GROUNDED) {
            rightEvidenceBaseline = adaptEvidenceBaseline(rightBaseline, right)
        }
    }

    private data class FootEvidenceUpdate(
        val evidence: SpeedFootMotionEvidence,
        val maximums: FootEvidenceMaximums,
    )

    private fun footMotionEvidence(
        sample: FootSample,
        baseline: FootEvidenceBaseline,
        phase: FootPhase,
        classificationRiseRatio: Float,
        timestampMillis: Long,
        airborneEvidence: AirborneEpisodeEvidence,
        previousMaximums: FootEvidenceMaximums,
    ): FootEvidenceUpdate {
        val averageBaseline = (baseline.ankleY + baseline.heelY + baseline.toeY) / 3f
        val average = riseRatio(averageBaseline, sample.groundY, sample.legLength)
        val ankle = riseRatio(baseline.ankleY, sample.ankleY, sample.legLength)
        val heel = riseRatio(baseline.heelY, sample.heelY, sample.legLength)
        val toe = riseRatio(baseline.toeY, sample.toeY, sample.legLength)
        val maximums = FootEvidenceMaximums(
            average = max(previousMaximums.average, average),
            ankle = max(previousMaximums.ankle, ankle),
            heel = max(previousMaximums.heel, heel),
            toe = max(previousMaximums.toe, toe),
        )
        val airborne = airborneEvidence.update(
            phase = phase,
            timestampMillis = timestampMillis,
            riseRatio = classificationRiseRatio,
        )
        return FootEvidenceUpdate(
            evidence = SpeedFootMotionEvidence(
                phase = if (phase == FootPhase.GROUNDED) {
                    SpeedFootPhase.GROUNDED
                } else {
                    SpeedFootPhase.AIRBORNE
                },
                classificationRiseRatio = classificationRiseRatio,
                currentAirborneDurationMillis = airborne.currentDurationMillis,
                maximumAirborneDurationMillis = airborne.maximumDurationMillis,
                currentAirborneMinimumRiseRatio = airborne.currentMinimumRiseRatio,
                longestAirborneMinimumRiseRatio = airborne.longestEpisodeMinimumRiseRatio,
                currentAverageRiseRatio = average,
                maximumAverageRiseRatio = maximums.average,
                currentAnkleRiseRatio = ankle,
                maximumAnkleRiseRatio = maximums.ankle,
                currentHeelRiseRatio = heel,
                maximumHeelRiseRatio = maximums.heel,
                currentToeRiseRatio = toe,
                maximumToeRiseRatio = maximums.toe,
            ),
            maximums = maximums,
        )
    }

    private fun AirborneEpisodeEvidence.update(
        phase: FootPhase,
        timestampMillis: Long,
        riseRatio: Float,
    ): AirborneEpisodeSnapshot {
        if (phase == FootPhase.AIRBORNE) {
            val startedAt = startedAtMillis ?: timestampMillis.also { startedAtMillis = it }
            minimumRiseRatio = minimumRiseRatio?.let { minOf(it, riseRatio) } ?: riseRatio
            val durationMillis = (timestampMillis - startedAt).coerceAtLeast(0L)
            recordMaximum(durationMillis, minimumRiseRatio)
            return snapshot(durationMillis)
        }

        val startedAt = startedAtMillis
        if (startedAt != null) {
            minimumRiseRatio = minimumRiseRatio?.let { minOf(it, riseRatio) } ?: riseRatio
            recordMaximum(
                durationMillis = (timestampMillis - startedAt).coerceAtLeast(0L),
                episodeMinimumRiseRatio = minimumRiseRatio,
            )
        }
        startedAtMillis = null
        minimumRiseRatio = null
        return snapshot(currentDurationMillis = 0L)
    }

    private fun AirborneEpisodeEvidence.recordMaximum(
        durationMillis: Long,
        episodeMinimumRiseRatio: Float?,
    ) {
        if (durationMillis >= maximumDurationMillis) {
            maximumDurationMillis = durationMillis
            longestEpisodeMinimumRiseRatio = episodeMinimumRiseRatio
        }
    }

    private fun AirborneEpisodeEvidence.snapshot(currentDurationMillis: Long) =
        AirborneEpisodeSnapshot(
            currentDurationMillis = currentDurationMillis,
            maximumDurationMillis = maximumDurationMillis,
            currentMinimumRiseRatio = minimumRiseRatio,
            longestEpisodeMinimumRiseRatio = longestEpisodeMinimumRiseRatio,
        )

    private fun AirborneEpisodeEvidence.reset() {
        startedAtMillis = null
        minimumRiseRatio = null
        maximumDurationMillis = 0L
        longestEpisodeMinimumRiseRatio = null
    }

    private fun AirborneEpisodeEvidence.cancelCurrentEpisode() {
        startedAtMillis = null
        minimumRiseRatio = null
    }

    private fun calibrateEvidenceBaseline(
        current: FootEvidenceBaseline?,
        sample: FootSample,
    ) = FootEvidenceBaseline(
        ankleY = current?.let { max(it.ankleY, sample.ankleY) } ?: sample.ankleY,
        heelY = current?.let { max(it.heelY, sample.heelY) } ?: sample.heelY,
        toeY = current?.let { max(it.toeY, sample.toeY) } ?: sample.toeY,
    )

    private fun adaptEvidenceBaseline(
        current: FootEvidenceBaseline,
        sample: FootSample,
    ) = FootEvidenceBaseline(
        ankleY = adaptGroundBaseline(current.ankleY, sample.ankleY),
        heelY = adaptGroundBaseline(current.heelY, sample.heelY),
        toeY = adaptGroundBaseline(current.toeY, sample.toeY),
    )

    private fun riseRatio(baselineY: Float, sampleY: Float, legLength: Float): Float =
        ((baselineY - sampleY) / legLength).coerceAtLeast(0f)

    private fun classificationRiseRatio(baseline: Float, sample: FootSample): Float =
        (baseline - sample.groundY) / sample.legLength

    private fun List<NormalizedPoint>.visible(index: Int): NormalizedPoint? =
        getOrNull(index)?.takeIf { it.isVisible }

    private fun calibrateBaseline(current: Float?, sample: Float): Float =
        current?.let { max(it, sample) } ?: sample

    private fun adaptGroundBaseline(current: Float, sample: Float): Float =
        when (groundReferencePolicy) {
            SpeedGroundReferencePolicy.ADAPTIVE ->
                if (sample >= current) sample else current +
                    (sample - current) * BASELINE_ADAPTATION_RATE
            SpeedGroundReferencePolicy.FIXED_AFTER_CALIBRATION -> current
        }

    private companion object {
        const val LEFT_HIP = 23
        const val RIGHT_HIP = 24
        const val LEFT_ANKLE = 27
        const val RIGHT_ANKLE = 28
        const val LEFT_HEEL = 29
        const val RIGHT_HEEL = 30
        const val LEFT_FOOT_INDEX = 31
        const val RIGHT_FOOT_INDEX = 32
        const val DEFAULT_CALIBRATION_FRAMES = 6
        const val DEFAULT_LIFT_RATIO = 0.08f
        const val DEFAULT_LANDING_RATIO = 0.03f
        const val DEFAULT_CONSERVATIVE_REARM_RATIO = 0.04f
        const val DEFAULT_CONSERVATIVE_REARM_FRAMES = 2
        const val DEFAULT_SIMULTANEOUS_WINDOW_MILLIS = 70L
        const val PRE_GO_CALIBRATION_FRAMES = 6
        const val MIN_LEG_LENGTH = 0.15f
        const val BASELINE_ADAPTATION_RATE = 0.02f
    }
}
