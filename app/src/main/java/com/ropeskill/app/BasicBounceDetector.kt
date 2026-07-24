package com.ropeskill.app

import kotlin.math.abs

enum class BounceTrackingStatus(val displayName: String) {
    WAITING("Stand fully in frame"),
    CALIBRATING("Hold still to calibrate"),
    READY("Ready to detect"),
    AIRBORNE("Jump detected"),
}

enum class BounceEvent {
    NONE,
    TAKEOFF,
    LANDING,
}

enum class BounceDiagnostic(val displayName: String) {
    FULL_BODY_REQUIRED("Full body required"),
    CALIBRATING("Calibrating"),
    READY("Ready"),
    FEET_NOT_SYNCHRONIZED("Feet not synchronized"),
    ANKLE_RISE_TOO_SMALL("Ankle rise too small"),
    HIP_RISE_TOO_SMALL("Hip rise too small"),
    AIRBORNE("Airborne"),
    LANDED("Landed"),
}

data class BounceDetectionResult(
    val countedJump: Boolean,
    val trackingStatus: BounceTrackingStatus,
    val event: BounceEvent = BounceEvent.NONE,
    val diagnostic: BounceDiagnostic,
    val lastCountEvidence: CountEvidence? = null,
)

data class CountEvidence(
    val leftAnkleRiseRatio: Float,
    val rightAnkleRiseRatio: Float,
    val hipRiseRatio: Float,
    val ankleDifferenceRatio: Float,
    val ankleDifference: Float,
    val ankleDifferenceLimit: Float,
    val feetSynchronized: Boolean,
    val airborneMillis: Long,
)

/**
 * Detects a small two-foot bounce from normalized MediaPipe landmarks.
 *
 * This first MVP baseline intentionally uses a simple state machine. Thresholds must be tuned from
 * real-device accuracy tests before they are treated as final.
 */
class BasicBounceDetector {
    private var phase = Phase.WAITING
    private var validCalibrationFrames = 0
    private var baselineAnkleY = 0f
    private var baselineAnkleDifference = 0f
    private var baselineHipY = 0f
    private var smoothedAnkleY: Float? = null
    private var smoothedHipY: Float? = null
    private var lastCountedAtMillis = Long.MIN_VALUE
    private var missingFrameCount = 0
    private var lastTrackingStatus = BounceTrackingStatus.WAITING
    private var pendingTakeoffEvidence: TakeoffEvidence? = null
    private var lastCountEvidence: CountEvidence? = null

    fun process(frame: PoseFrame, timestampMillis: Long): BounceDetectionResult {
        val measurement = measurement(frame) ?: run {
            missingFrameCount += 1
            if (missingFrameCount > MAX_MISSING_FRAME_COUNT) {
                resetTracking()
                return BounceDetectionResult(
                    countedJump = false,
                    trackingStatus = BounceTrackingStatus.WAITING,
                    diagnostic = BounceDiagnostic.FULL_BODY_REQUIRED,
                )
            }
            return BounceDetectionResult(
                countedJump = false,
                trackingStatus = lastTrackingStatus,
                diagnostic = BounceDiagnostic.FULL_BODY_REQUIRED,
            )
        }
        missingFrameCount = 0

        val ankleY = smoothAnkle(measurement.ankleY)
        val hipY = smoothHip(measurement.hipY)
        val takeoffDistance = measurement.legLength * TAKEOFF_LEG_RATIO
        val hipTakeoffDistance = measurement.legLength * HIP_TAKEOFF_LEG_RATIO
        val landingDistance = measurement.legLength * LANDING_LEG_RATIO

        return when (phase) {
            Phase.WAITING, Phase.CALIBRATING -> calibrate(
                ankleY = ankleY,
                hipY = hipY,
                ankleDifference = measurement.leftAnkleY - measurement.rightAnkleY,
                legLength = measurement.legLength,
            )
            Phase.GROUNDED -> {
                baselineAnkleY += BASELINE_ADAPTATION * (ankleY - baselineAnkleY)
                baselineHipY += BASELINE_ADAPTATION * (hipY - baselineHipY)
                val ankleDifference = abs(
                    (measurement.leftAnkleY - measurement.rightAnkleY) -
                        baselineAnkleDifference,
                )
                val ankleDifferenceLimit =
                    measurement.legLength * MAX_ANKLE_HEIGHT_DIFFERENCE_RATIO
                val bothFeetRiseTogether = ankleDifference <= ankleDifferenceLimit
                val anklesRise = baselineAnkleY - ankleY >= takeoffDistance
                val hipsRise = baselineHipY - hipY >= hipTakeoffDistance
                val averageAnkleRise =
                    baselineAnkleY - (measurement.leftAnkleY + measurement.rightAnkleY) / 2f
                val hipRise = baselineHipY - hipY
                val hipsRiseWithAnkles =
                    hipRise >= averageAnkleRise * MIN_HIP_TO_ANKLE_RISE_RATIO
                if (bothFeetRiseTogether && anklesRise && hipsRise && hipsRiseWithAnkles) {
                    phase = Phase.AIRBORNE
                    pendingTakeoffEvidence = TakeoffEvidence(
                        leftAnkleRiseRatio =
                            (baselineAnkleY - measurement.leftAnkleY) / measurement.legLength,
                        rightAnkleRiseRatio =
                            (baselineAnkleY - measurement.rightAnkleY) / measurement.legLength,
                        hipRiseRatio = hipRise / measurement.legLength,
                        ankleDifferenceRatio = ankleDifference / measurement.legLength,
                        ankleDifference = ankleDifference,
                        ankleDifferenceLimit = ankleDifferenceLimit,
                        feetSynchronized = bothFeetRiseTogether,
                        takeoffTimestampMillis = timestampMillis,
                    )
                    BounceDetectionResult(
                        countedJump = false,
                        trackingStatus = BounceTrackingStatus.AIRBORNE,
                        event = BounceEvent.TAKEOFF,
                        diagnostic = BounceDiagnostic.AIRBORNE,
                    )
                } else {
                    BounceDetectionResult(
                        countedJump = false,
                        trackingStatus = BounceTrackingStatus.READY,
                        diagnostic = when {
                            !bothFeetRiseTogether -> BounceDiagnostic.FEET_NOT_SYNCHRONIZED
                            !anklesRise -> BounceDiagnostic.ANKLE_RISE_TOO_SMALL
                            !hipsRise || !hipsRiseWithAnkles ->
                                BounceDiagnostic.HIP_RISE_TOO_SMALL
                            else -> BounceDiagnostic.READY
                        },
                    )
                }
            }
            Phase.AIRBORNE -> {
                val hasLanded = abs(baselineAnkleY - ankleY) <= landingDistance
                val takeoffTimestampMillis = pendingTakeoffEvidence?.takeoffTimestampMillis
                val airborneTooLong = !hasLanded &&
                    takeoffTimestampMillis != null &&
                    timestampMillis - takeoffTimestampMillis >= MAX_AIRBORNE_DURATION_MILLIS
                if (airborneTooLong) {
                    resetTracking()
                    calibrate(
                        ankleY = measurement.ankleY,
                        hipY = measurement.hipY,
                        ankleDifference = measurement.leftAnkleY - measurement.rightAnkleY,
                        legLength = measurement.legLength,
                    )
                } else if (!hasLanded) {
                    BounceDetectionResult(
                        countedJump = false,
                        trackingStatus = BounceTrackingStatus.AIRBORNE,
                        diagnostic = BounceDiagnostic.AIRBORNE,
                    )
                } else {
                    phase = Phase.GROUNDED
                    val outsideCooldown = lastCountedAtMillis == Long.MIN_VALUE ||
                        timestampMillis - lastCountedAtMillis >= COUNT_COOLDOWN_MILLIS
                    if (outsideCooldown) {
                        lastCountedAtMillis = timestampMillis
                        pendingTakeoffEvidence?.let { takeoff ->
                            lastCountEvidence = CountEvidence(
                                leftAnkleRiseRatio = takeoff.leftAnkleRiseRatio,
                                rightAnkleRiseRatio = takeoff.rightAnkleRiseRatio,
                                hipRiseRatio = takeoff.hipRiseRatio,
                                ankleDifferenceRatio = takeoff.ankleDifferenceRatio,
                                ankleDifference = takeoff.ankleDifference,
                                ankleDifferenceLimit = takeoff.ankleDifferenceLimit,
                                feetSynchronized = takeoff.feetSynchronized,
                                airborneMillis =
                                    (timestampMillis - takeoff.takeoffTimestampMillis)
                                        .coerceAtLeast(0L),
                            )
                        }
                    }
                    pendingTakeoffEvidence = null
                    BounceDetectionResult(
                        countedJump = outsideCooldown,
                        trackingStatus = BounceTrackingStatus.READY,
                        event = BounceEvent.LANDING,
                        diagnostic = BounceDiagnostic.LANDED,
                        lastCountEvidence = lastCountEvidence,
                    )
                }
            }
        }.also { lastTrackingStatus = it.trackingStatus }
    }

    fun reset() {
        lastCountedAtMillis = Long.MIN_VALUE
        lastCountEvidence = null
        resetTracking()
    }

    private fun calibrate(
        ankleY: Float,
        hipY: Float,
        ankleDifference: Float,
        legLength: Float,
    ): BounceDetectionResult {
        phase = Phase.CALIBRATING
        val movedTooMuch = validCalibrationFrames > 0 &&
            (abs(ankleY - baselineAnkleY) > legLength * CALIBRATION_MOTION_RATIO ||
                abs(hipY - baselineHipY) > legLength * CALIBRATION_MOTION_RATIO)
        if (movedTooMuch) validCalibrationFrames = 0

        baselineAnkleY = if (validCalibrationFrames == 0) {
            ankleY
        } else {
            baselineAnkleY + (ankleY - baselineAnkleY) / (validCalibrationFrames + 1)
        }
        baselineHipY = if (validCalibrationFrames == 0) {
            hipY
        } else {
            baselineHipY + (hipY - baselineHipY) / (validCalibrationFrames + 1)
        }
        baselineAnkleDifference = if (validCalibrationFrames == 0) {
            ankleDifference
        } else {
            baselineAnkleDifference +
                (ankleDifference - baselineAnkleDifference) / (validCalibrationFrames + 1)
        }
        validCalibrationFrames += 1
        if (validCalibrationFrames >= CALIBRATION_FRAME_COUNT) {
            phase = Phase.GROUNDED
            return BounceDetectionResult(
                countedJump = false,
                trackingStatus = BounceTrackingStatus.READY,
                diagnostic = BounceDiagnostic.READY,
            )
        }
        return BounceDetectionResult(
            countedJump = false,
            trackingStatus = BounceTrackingStatus.CALIBRATING,
            diagnostic = BounceDiagnostic.CALIBRATING,
        )
    }

    private fun measurement(frame: PoseFrame): Measurement? {
        val leftHip = frame.visiblePoint(LEFT_HIP) ?: return null
        val rightHip = frame.visiblePoint(RIGHT_HIP) ?: return null
        val leftAnkle = frame.visiblePoint(LEFT_ANKLE) ?: return null
        val rightAnkle = frame.visiblePoint(RIGHT_ANKLE) ?: return null

        val hipY = (leftHip.y + rightHip.y) / 2f
        val ankleY = (leftAnkle.y + rightAnkle.y) / 2f
        val legLength = ankleY - hipY
        if (legLength < MIN_NORMALIZED_LEG_LENGTH) return null
        return Measurement(
            ankleY = ankleY,
            hipY = hipY,
            leftAnkleY = leftAnkle.y,
            rightAnkleY = rightAnkle.y,
            legLength = legLength,
        )
    }

    private fun smoothAnkle(value: Float): Float {
        val previous = smoothedAnkleY
        return if (previous == null) {
            value
        } else {
            previous + SMOOTHING_ALPHA * (value - previous)
        }.also { smoothedAnkleY = it }
    }

    private fun smoothHip(value: Float): Float {
        val previous = smoothedHipY
        return if (previous == null) {
            value
        } else {
            previous + SMOOTHING_ALPHA * (value - previous)
        }.also { smoothedHipY = it }
    }

    private fun resetTracking() {
        phase = Phase.WAITING
        validCalibrationFrames = 0
        baselineAnkleY = 0f
        baselineAnkleDifference = 0f
        baselineHipY = 0f
        smoothedAnkleY = null
        smoothedHipY = null
        missingFrameCount = 0
        lastTrackingStatus = BounceTrackingStatus.WAITING
        pendingTakeoffEvidence = null
    }

    private fun PoseFrame.visiblePoint(index: Int): NormalizedPoint? =
        landmarks.getOrNull(index)?.takeIf { it.isVisible }

    private data class Measurement(
        val ankleY: Float,
        val hipY: Float,
        val leftAnkleY: Float,
        val rightAnkleY: Float,
        val legLength: Float,
    )

    private data class TakeoffEvidence(
        val leftAnkleRiseRatio: Float,
        val rightAnkleRiseRatio: Float,
        val hipRiseRatio: Float,
        val ankleDifferenceRatio: Float,
        val ankleDifference: Float,
        val ankleDifferenceLimit: Float,
        val feetSynchronized: Boolean,
        val takeoffTimestampMillis: Long,
    )

    private enum class Phase { WAITING, CALIBRATING, GROUNDED, AIRBORNE }

    private companion object {
        const val LEFT_HIP = 23
        const val RIGHT_HIP = 24
        const val LEFT_ANKLE = 27
        const val RIGHT_ANKLE = 28
        const val CALIBRATION_FRAME_COUNT = 45
        const val MIN_NORMALIZED_LEG_LENGTH = 0.12f
        const val TAKEOFF_LEG_RATIO = 0.045f
        const val HIP_TAKEOFF_LEG_RATIO = 0.025f
        const val MIN_HIP_TO_ANKLE_RISE_RATIO = 1.10f
        const val LANDING_LEG_RATIO = 0.04f
        const val MAX_ANKLE_HEIGHT_DIFFERENCE_RATIO = 0.08f
        const val CALIBRATION_MOTION_RATIO = 0.025f
        const val SMOOTHING_ALPHA = 0.60f
        const val BASELINE_ADAPTATION = 0.02f
        const val COUNT_COOLDOWN_MILLIS = 250L
        const val MAX_AIRBORNE_DURATION_MILLIS = 1_500L
        const val MAX_MISSING_FRAME_COUNT = 5
    }
}
