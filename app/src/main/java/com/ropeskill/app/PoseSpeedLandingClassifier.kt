package com.ropeskill.app

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
) {
    private enum class FootPhase { GROUNDED, AIRBORNE }
    private enum class FootSide { LEFT, RIGHT }

    private data class FootSample(
        val groundY: Float,
        val legLength: Float,
    )

    private data class PendingLanding(
        val side: FootSide,
        val timestampMillis: Long,
    )

    private var calibrationFrames = 0
    private var leftBaselineY: Float? = null
    private var rightBaselineY: Float? = null
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
        val events = flushExpiredPending(timestampMillis).toMutableList()
        if (calibrationFrames < calibrationFramesRequired) {
            leftBaselineY = calibrateBaseline(leftBaselineY, left.groundY)
            rightBaselineY = calibrateBaseline(rightBaselineY, right.groundY)
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
        lastTimestampMillis = null
        trackingWasValid = false
        validFrames = 0L
        lowVisibilityFrames = 0L
        outOfOrderFrames = 0L
        trackingLossEvents = 0
        unclearLandings = 0
        resetMotionState()
    }

    fun diagnostics() = SpeedLandingClassifierDiagnostics(
        validFrames = validFrames,
        lowVisibilityFrames = lowVisibilityFrames,
        outOfOrderFrames = outOfOrderFrames,
        trackingLossEvents = trackingLossEvents,
        unclearLandings = unclearLandings,
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
        return FootSample(groundY = groundY, legLength = legLength)
    }

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
        const val MIN_LEG_LENGTH = 0.15f
        const val BASELINE_ADAPTATION_RATE = 0.02f
    }
}
