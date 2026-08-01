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
    val motionEvidence: SpeedMotionEvidence? = null,
)

enum class SpeedFootPhase {
    GROUNDED,
    AIRBORNE,
}

data class SpeedFootMotionEvidence(
    val phase: SpeedFootPhase,
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
    private val evidenceEnabled: Boolean = false,
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

    private data class PendingLanding(
        val side: FootSide,
        val timestampMillis: Long,
    )

    private var calibrationFrames = 0
    private var leftBaselineY: Float? = null
    private var rightBaselineY: Float? = null
    private var leftEvidenceBaseline: FootEvidenceBaseline? = null
    private var rightEvidenceBaseline: FootEvidenceBaseline? = null
    private var leftEvidenceMaximums = FootEvidenceMaximums()
    private var rightEvidenceMaximums = FootEvidenceMaximums()
    private var motionEvidence: SpeedMotionEvidence? = null
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
    private var preGoCalibrationActive = false
    private val preGoLeftSamples = ArrayDeque<FootSample>(PRE_GO_CALIBRATION_FRAMES)
    private val preGoRightSamples = ArrayDeque<FootSample>(PRE_GO_CALIBRATION_FRAMES)

    init {
        require(calibrationFramesRequired > 0)
        require(liftRatio > landingRatio && landingRatio > 0f)
        require(simultaneousWindowMillis >= 0L)
    }

    fun process(frame: PoseFrame): SpeedLandingClassifierResult {
        val timestampMillis = frame.sourceTimestampMillis
        val previousTimestamp = lastTimestampMillis
        if (timestampMillis <= 0L || (previousTimestamp != null && timestampMillis <= previousTimestamp)) {
            outOfOrderFrames += 1L
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
            return SpeedLandingClassifierResult(
                events = events,
                trackingValid = true,
                diagnostic = SpeedClassifierDiagnostic.CALIBRATING,
            )
        }

        val leftLanded = updateFoot(
            sample = left,
            baseline = leftBaselineY ?: left.groundY,
            phase = leftPhase,
            onPhaseChanged = { leftPhase = it },
            onBaselineChanged = { leftBaselineY = it },
        )
        val rightLanded = updateFoot(
            sample = right,
            baseline = rightBaselineY ?: right.groundY,
            phase = rightPhase,
            onPhaseChanged = { rightPhase = it },
            onBaselineChanged = { rightBaselineY = it },
        )
        updateMotionEvidence(frame.sourceTimestampMillis, left, right)

        when {
            leftLanded && rightLanded -> {
                pendingLanding = null
                events += SpeedLandingEvent(SpeedLanding.BOTH, timestampMillis)
            }
            leftLanded -> collectLanding(FootSide.LEFT, timestampMillis, events)
            rightLanded -> collectLanding(FootSide.RIGHT, timestampMillis, events)
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
        motionEvidence = null
    }

    fun diagnostics() = SpeedLandingClassifierDiagnostics(
        validFrames = validFrames,
        lowVisibilityFrames = lowVisibilityFrames,
        outOfOrderFrames = outOfOrderFrames,
        trackingLossEvents = trackingLossEvents,
        unclearLandings = unclearLandings,
        calibrationFrames = calibrationFrames,
        motionEvidence = motionEvidence,
    )

    private fun updateFoot(
        sample: FootSample,
        baseline: Float,
        phase: FootPhase,
        onPhaseChanged: (FootPhase) -> Unit,
        onBaselineChanged: (Float) -> Unit,
    ): Boolean {
        val rise = baseline - sample.groundY
        return when (phase) {
            FootPhase.GROUNDED -> {
                if (rise >= sample.legLength * liftRatio) {
                    onPhaseChanged(FootPhase.AIRBORNE)
                } else {
                    onBaselineChanged(adaptGroundBaseline(baseline, sample.groundY))
                }
                false
            }
            FootPhase.AIRBORNE -> {
                if (rise <= sample.legLength * landingRatio) {
                    onPhaseChanged(FootPhase.GROUNDED)
                    onBaselineChanged(adaptGroundBaseline(baseline, sample.groundY))
                    true
                } else {
                    false
                }
            }
        }
    }

    private fun collectLanding(
        side: FootSide,
        timestampMillis: Long,
        events: MutableList<SpeedLandingEvent>,
    ) {
        val pending = pendingLanding
        if (
            pending != null &&
            pending.side != side &&
            timestampMillis - pending.timestampMillis <= simultaneousWindowMillis
        ) {
            pendingLanding = null
            events += SpeedLandingEvent(SpeedLanding.BOTH, timestampMillis)
        } else {
            if (pending != null) events += pending.toEvent()
            pendingLanding = PendingLanding(side, timestampMillis)
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
    )

    private fun resetMotionState() {
        leftPhase = FootPhase.GROUNDED
        rightPhase = FootPhase.GROUNDED
        pendingLanding = null
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
    ) {
        if (!evidenceEnabled) return
        val leftBaseline = leftEvidenceBaseline ?: return
        val rightBaseline = rightEvidenceBaseline ?: return
        val leftEvidence = footMotionEvidence(
            sample = left,
            baseline = leftBaseline,
            phase = leftPhase,
            previousMaximums = leftEvidenceMaximums,
        )
        val rightEvidence = footMotionEvidence(
            sample = right,
            baseline = rightBaseline,
            phase = rightPhase,
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
        return FootEvidenceUpdate(
            evidence = SpeedFootMotionEvidence(
                phase = if (phase == FootPhase.GROUNDED) {
                    SpeedFootPhase.GROUNDED
                } else {
                    SpeedFootPhase.AIRBORNE
                },
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

    private fun List<NormalizedPoint>.visible(index: Int): NormalizedPoint? =
        getOrNull(index)?.takeIf { it.isVisible }

    private fun calibrateBaseline(current: Float?, sample: Float): Float =
        current?.let { max(it, sample) } ?: sample

    private fun adaptGroundBaseline(current: Float, sample: Float): Float =
        if (sample >= current) sample else current + (sample - current) * BASELINE_ADAPTATION_RATE

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
        const val DEFAULT_SIMULTANEOUS_WINDOW_MILLIS = 70L
        const val PRE_GO_CALIBRATION_FRAMES = 6
        const val MIN_LEG_LENGTH = 0.15f
        const val BASELINE_ADAPTATION_RATE = 0.02f
    }
}
