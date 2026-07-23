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

data class BounceDetectionResult(
    val countedJump: Boolean,
    val trackingStatus: BounceTrackingStatus,
    val event: BounceEvent = BounceEvent.NONE,
    val isStable: Boolean = false,
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
    private var baselineLeftAnkleX = 0f
    private var baselineRightAnkleX = 0f
    private var baselineHipY = 0f
    private var smoothedAnkleY: Float? = null
    private var smoothedHipY: Float? = null
    private var lastCountedAtMillis = Long.MIN_VALUE
    private var missingFrameCount = 0
    private var lastTrackingStatus = BounceTrackingStatus.WAITING

    fun process(frame: PoseFrame, timestampMillis: Long): BounceDetectionResult {
        val measurement = measurement(frame) ?: run {
            missingFrameCount += 1
            if (missingFrameCount > MAX_MISSING_FRAME_COUNT) {
                resetTracking()
                return BounceDetectionResult(false, BounceTrackingStatus.WAITING)
            }
            return BounceDetectionResult(false, lastTrackingStatus)
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
                leftAnkleX = measurement.leftAnkleX,
                rightAnkleX = measurement.rightAnkleX,
                legLength = measurement.legLength,
            )
            Phase.GROUNDED -> {
                val ankleRise = baselineAnkleY - ankleY
                val hipRise = baselineHipY - hipY
                val verticalMotionDifference = abs(ankleRise - hipRise)
                val horizontalFootMovement = maxOf(
                    abs(measurement.leftAnkleX - baselineLeftAnkleX),
                    abs(measurement.rightAnkleX - baselineRightAnkleX),
                )
                val bothFeetRiseTogether =
                    abs(
                        (measurement.leftAnkleY - measurement.rightAnkleY) -
                            baselineAnkleDifference,
                    ) <=
                        measurement.legLength * MAX_ANKLE_HEIGHT_DIFFERENCE_RATIO
                val hasJumpLikeVerticalMotion =
                    ankleRise >= takeoffDistance &&
                        hipRise >= hipTakeoffDistance &&
                        verticalMotionDifference <=
                        measurement.legLength * MAX_VERTICAL_MOTION_DIFFERENCE_RATIO
                val feetStayInJumpArea =
                    horizontalFootMovement <=
                        measurement.legLength * MAX_HORIZONTAL_FOOT_MOVEMENT_RATIO
                val isStable =
                    abs(ankleRise) <= measurement.legLength * READY_MOTION_RATIO &&
                        abs(hipRise) <= measurement.legLength * READY_MOTION_RATIO &&
                        horizontalFootMovement <=
                        measurement.legLength * READY_HORIZONTAL_MOTION_RATIO

                if (isStable) {
                    baselineAnkleY += BASELINE_ADAPTATION * (ankleY - baselineAnkleY)
                    baselineHipY += BASELINE_ADAPTATION * (hipY - baselineHipY)
                    baselineLeftAnkleX +=
                        BASELINE_ADAPTATION * (measurement.leftAnkleX - baselineLeftAnkleX)
                    baselineRightAnkleX +=
                        BASELINE_ADAPTATION * (measurement.rightAnkleX - baselineRightAnkleX)
                }

                if (bothFeetRiseTogether && hasJumpLikeVerticalMotion && feetStayInJumpArea) {
                    phase = Phase.AIRBORNE
                    BounceDetectionResult(
                        countedJump = false,
                        trackingStatus = BounceTrackingStatus.AIRBORNE,
                        event = BounceEvent.TAKEOFF,
                    )
                } else {
                    BounceDetectionResult(
                        countedJump = false,
                        trackingStatus = BounceTrackingStatus.READY,
                        isStable = isStable,
                    )
                }
            }
            Phase.AIRBORNE -> {
                val hasLanded = abs(baselineAnkleY - ankleY) <= landingDistance
                if (!hasLanded) {
                    BounceDetectionResult(false, BounceTrackingStatus.AIRBORNE)
                } else {
                    phase = Phase.GROUNDED
                    val outsideCooldown = lastCountedAtMillis == Long.MIN_VALUE ||
                        timestampMillis - lastCountedAtMillis >= COUNT_COOLDOWN_MILLIS
                    if (outsideCooldown) lastCountedAtMillis = timestampMillis
                    BounceDetectionResult(
                        countedJump = outsideCooldown,
                        trackingStatus = BounceTrackingStatus.READY,
                        event = BounceEvent.LANDING,
                    )
                }
            }
        }.also { lastTrackingStatus = it.trackingStatus }
    }

    fun reset() {
        lastCountedAtMillis = Long.MIN_VALUE
        resetTracking()
    }

    private fun calibrate(
        ankleY: Float,
        hipY: Float,
        ankleDifference: Float,
        leftAnkleX: Float,
        rightAnkleX: Float,
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
        baselineLeftAnkleX = if (validCalibrationFrames == 0) {
            leftAnkleX
        } else {
            baselineLeftAnkleX +
                (leftAnkleX - baselineLeftAnkleX) / (validCalibrationFrames + 1)
        }
        baselineRightAnkleX = if (validCalibrationFrames == 0) {
            rightAnkleX
        } else {
            baselineRightAnkleX +
                (rightAnkleX - baselineRightAnkleX) / (validCalibrationFrames + 1)
        }
        validCalibrationFrames += 1
        if (validCalibrationFrames >= CALIBRATION_FRAME_COUNT) {
            phase = Phase.GROUNDED
            return BounceDetectionResult(
                countedJump = false,
                trackingStatus = BounceTrackingStatus.READY,
                isStable = true,
            )
        }
        return BounceDetectionResult(false, BounceTrackingStatus.CALIBRATING)
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
            leftAnkleX = leftAnkle.x,
            rightAnkleX = rightAnkle.x,
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
        baselineLeftAnkleX = 0f
        baselineRightAnkleX = 0f
        baselineHipY = 0f
        smoothedAnkleY = null
        smoothedHipY = null
        missingFrameCount = 0
        lastTrackingStatus = BounceTrackingStatus.WAITING
    }

    private fun PoseFrame.visiblePoint(index: Int): NormalizedPoint? =
        landmarks.getOrNull(index)?.takeIf { it.isVisible }

    private data class Measurement(
        val ankleY: Float,
        val hipY: Float,
        val leftAnkleY: Float,
        val rightAnkleY: Float,
        val leftAnkleX: Float,
        val rightAnkleX: Float,
        val legLength: Float,
    )

    private enum class Phase { WAITING, CALIBRATING, GROUNDED, AIRBORNE }

    private companion object {
        const val LEFT_HIP = 23
        const val RIGHT_HIP = 24
        const val LEFT_ANKLE = 27
        const val RIGHT_ANKLE = 28
        const val CALIBRATION_FRAME_COUNT = 45
        const val MIN_NORMALIZED_LEG_LENGTH = 0.12f
        const val TAKEOFF_LEG_RATIO = 0.035f
        const val HIP_TAKEOFF_LEG_RATIO = 0.02f
        const val LANDING_LEG_RATIO = 0.025f
        const val MAX_ANKLE_HEIGHT_DIFFERENCE_RATIO = 0.08f
        const val MAX_VERTICAL_MOTION_DIFFERENCE_RATIO = 0.035f
        const val MAX_HORIZONTAL_FOOT_MOVEMENT_RATIO = 0.05f
        const val READY_MOTION_RATIO = 0.02f
        const val READY_HORIZONTAL_MOTION_RATIO = 0.025f
        const val CALIBRATION_MOTION_RATIO = 0.025f
        const val SMOOTHING_ALPHA = 0.80f
        const val BASELINE_ADAPTATION = 0.02f
        const val COUNT_COOLDOWN_MILLIS = 140L
        const val MAX_MISSING_FRAME_COUNT = 5
    }
}
