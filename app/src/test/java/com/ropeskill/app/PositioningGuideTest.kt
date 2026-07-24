package com.ropeskill.app

import org.junit.Assert.assertEquals
import org.junit.Test

class PositioningGuideTest {
    private val guide = PositioningGuide()

    @Test
    fun croppedLegsWithLargeTorso_requestsStepBack() {
        val frame = frame(
            noseY = 0.08f,
            shoulderY = 0.20f,
            hipY = 0.52f,
            ankleY = null,
        )

        assertEquals(PositioningGuidance.STEP_BACK, guide.evaluate(frame))
    }

    @Test
    fun smallFullBody_requestsMoveCloser() {
        val frame = frame(
            noseY = 0.30f,
            shoulderY = 0.38f,
            hipY = 0.52f,
            ankleY = 0.68f,
        )

        assertEquals(PositioningGuidance.MOVE_CLOSER, guide.evaluate(frame))
    }

    @Test
    fun appropriatelySizedFullBody_reportsDistanceGood() {
        val frame = frame(
            noseY = 0.12f,
            shoulderY = 0.25f,
            hipY = 0.50f,
            ankleY = 0.88f,
        )

        assertEquals(PositioningGuidance.DISTANCE_GOOD, guide.evaluate(frame))
    }

    @Test
    fun missingBodyWithoutCloseTorso_requestsFullBody() {
        val frame = frame(
            noseY = null,
            shoulderY = 0.35f,
            hipY = 0.50f,
            ankleY = null,
        )

        assertEquals(PositioningGuidance.FULL_BODY_REQUIRED, guide.evaluate(frame))
    }

    private fun frame(
        noseY: Float?,
        shoulderY: Float,
        hipY: Float,
        ankleY: Float?,
    ): PoseFrame {
        val points = MutableList(33) {
            NormalizedPoint(x = 0.5f, y = 0.5f, isVisible = false)
        }
        noseY?.let { points[0] = visiblePoint(0.5f, it) }
        points[11] = visiblePoint(0.45f, shoulderY)
        points[12] = visiblePoint(0.55f, shoulderY)
        points[23] = visiblePoint(0.45f, hipY)
        points[24] = visiblePoint(0.55f, hipY)
        ankleY?.let {
            points[27] = visiblePoint(0.45f, it)
            points[28] = visiblePoint(0.55f, it)
        }
        return PoseFrame(points, imageWidth = 1080, imageHeight = 1920)
    }

    private fun visiblePoint(x: Float, y: Float) =
        NormalizedPoint(x = x, y = y, isVisible = true)
}
