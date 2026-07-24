package com.ropeskill.app

import kotlin.math.abs

enum class PositioningGuidance {
    FULL_BODY_REQUIRED,
    STEP_BACK,
    MOVE_CLOSER,
    DISTANCE_GOOD,
}

/**
 * Gives camera-distance guidance from normalized pose landmarks before training starts.
 *
 * This is intentionally separate from BasicBounceDetector so positioning thresholds cannot change
 * jump counting. Values are MVP starting points that must be verified on the target device.
 */
class PositioningGuide {
    fun evaluate(frame: PoseFrame): PositioningGuidance {
        val leftShoulder = frame.visiblePoint(LEFT_SHOULDER)
        val rightShoulder = frame.visiblePoint(RIGHT_SHOULDER)
        val leftHip = frame.visiblePoint(LEFT_HIP)
        val rightHip = frame.visiblePoint(RIGHT_HIP)
        val nose = frame.visiblePoint(NOSE)
        val leftAnkle = frame.visiblePoint(LEFT_ANKLE)
        val rightAnkle = frame.visiblePoint(RIGHT_ANKLE)

        val torsoHeight = if (
            leftShoulder != null &&
            rightShoulder != null &&
            leftHip != null &&
            rightHip != null
        ) {
            abs(
                (leftHip.y + rightHip.y) / 2f -
                    (leftShoulder.y + rightShoulder.y) / 2f,
            )
        } else {
            null
        }

        val hasFullBody = nose != null &&
            leftShoulder != null &&
            rightShoulder != null &&
            leftHip != null &&
            rightHip != null &&
            leftAnkle != null &&
            rightAnkle != null

        if (!hasFullBody) {
            return if (torsoHeight != null && torsoHeight >= CLOSE_TORSO_HEIGHT) {
                PositioningGuidance.STEP_BACK
            } else {
                PositioningGuidance.FULL_BODY_REQUIRED
            }
        }

        val averageAnkleY = (leftAnkle!!.y + rightAnkle!!.y) / 2f
        val bodyHeight = averageAnkleY - nose!!.y
        return when {
            bodyHeight >= MAX_BODY_HEIGHT -> PositioningGuidance.STEP_BACK
            bodyHeight <= MIN_BODY_HEIGHT -> PositioningGuidance.MOVE_CLOSER
            else -> PositioningGuidance.DISTANCE_GOOD
        }
    }

    private fun PoseFrame.visiblePoint(index: Int): NormalizedPoint? =
        landmarks.getOrNull(index)?.takeIf { it.isVisible }

    private companion object {
        const val NOSE = 0
        const val LEFT_SHOULDER = 11
        const val RIGHT_SHOULDER = 12
        const val LEFT_HIP = 23
        const val RIGHT_HIP = 24
        const val LEFT_ANKLE = 27
        const val RIGHT_ANKLE = 28

        const val CLOSE_TORSO_HEIGHT = 0.28f
        const val MIN_BODY_HEIGHT = 0.50f
        const val MAX_BODY_HEIGHT = 0.88f
    }
}
