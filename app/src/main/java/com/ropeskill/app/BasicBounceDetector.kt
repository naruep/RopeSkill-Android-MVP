package com.ropeskill.app

import kotlin.math.abs

enum class BounceTrackingStatus(val displayName: String) {
    WAITING("Stand fully in frame"),
    CALIBRATING("Hold still to calibrate"),
    READY("Ready to detect"),
    AIRBORNE("Jump detected"),
}

data class BounceDetectionResult(
    val countedJump: Boolean,
    val trackingStatus: BounceTrackingStatus,
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
    private var smoothedAnkleY: Float? = null
    private var lastCountedAtMillis = Long.MIN_VALUE

    fun process(frame: PoseFrame, timestampMillis: Long): BounceDetectionResult {
        val measurement = measurement(frame) ?: run {
            resetTracking()
            return BounceDetectionResult(false, BounceTrackingStatus.WAITING)
        }

        val ankleY = smooth(measurement.ankleY)
        val takeoffDistance = measurement.legLength * TAKEOFF_LEG_RATIO
        val landingDistance = measurement.legLength * LANDING_LEG_RATIO

        return when (phase) {
            Phase.WAITING, Phase.CALIBRATING -> calibrate(ankleY)
            Phase.GROUNDED -> {
                baselineAnkleY += BASELINE_ADAPTATION * (ankleY - baselineAnkleY)
                if (baselineAnkleY - ankleY >= takeoffDistance) {
                    phase = Phase.AIRBORNE
                    BounceDetectionResult(false, BounceTrackingStatus.AIRBORNE)
                } else {
                    BounceDetectionResult(false, BounceTrackingStatus.READY)
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
                    BounceDetectionResult(outsideCooldown, BounceTrackingStatus.READY)
                }
            }
        }
    }

    fun reset() {
        lastCountedAtMillis = Long.MIN_VALUE
        resetTracking()
    }

    private fun calibrate(ankleY: Float): BounceDetectionResult {
        phase = Phase.CALIBRATING
        baselineAnkleY = if (validCalibrationFrames == 0) {
            ankleY
        } else {
            baselineAnkleY + (ankleY - baselineAnkleY) / (validCalibrationFrames + 1)
        }
        validCalibrationFrames += 1
        if (validCalibrationFrames >= CALIBRATION_FRAME_COUNT) {
            phase = Phase.GROUNDED
            return BounceDetectionResult(false, BounceTrackingStatus.READY)
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
        return Measurement(ankleY = ankleY, legLength = legLength)
    }

    private fun smooth(value: Float): Float {
        val previous = smoothedAnkleY
        return if (previous == null) {
            value
        } else {
            previous + SMOOTHING_ALPHA * (value - previous)
        }.also { smoothedAnkleY = it }
    }

    private fun resetTracking() {
        phase = Phase.WAITING
        validCalibrationFrames = 0
        baselineAnkleY = 0f
        smoothedAnkleY = null
    }

    private fun PoseFrame.visiblePoint(index: Int): NormalizedPoint? =
        landmarks.getOrNull(index)?.takeIf { it.isVisible }

    private data class Measurement(val ankleY: Float, val legLength: Float)

    private enum class Phase { WAITING, CALIBRATING, GROUNDED, AIRBORNE }

    private companion object {
        const val LEFT_HIP = 23
        const val RIGHT_HIP = 24
        const val LEFT_ANKLE = 27
        const val RIGHT_ANKLE = 28
        const val CALIBRATION_FRAME_COUNT = 15
        const val MIN_NORMALIZED_LEG_LENGTH = 0.12f
        const val TAKEOFF_LEG_RATIO = 0.10f
        const val LANDING_LEG_RATIO = 0.04f
        const val SMOOTHING_ALPHA = 0.45f
        const val BASELINE_ADAPTATION = 0.02f
        const val COUNT_COOLDOWN_MILLIS = 250L
    }
}
