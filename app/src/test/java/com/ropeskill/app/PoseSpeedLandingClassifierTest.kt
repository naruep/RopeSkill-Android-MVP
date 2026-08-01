package com.ropeskill.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PoseSpeedLandingClassifierTest {
    @Test
    fun alternatingFeet_emitAnatomicalLeftThenRight() {
        val classifier = readyClassifier()

        classifier.process(frame(166L, leftY = LIFTED, rightY = GROUND))
        classifier.process(frame(199L, leftY = GROUND, rightY = GROUND))
        val left = classifier.process(frame(300L, leftY = GROUND, rightY = GROUND))

        classifier.process(frame(333L, leftY = GROUND, rightY = LIFTED))
        classifier.process(frame(366L, leftY = GROUND, rightY = GROUND))
        val right = classifier.process(frame(467L, leftY = GROUND, rightY = GROUND))

        assertEquals(listOf(SpeedLanding.LEFT), left.events.map { it.landing })
        assertEquals(199L, left.events.single().timestampMillis)
        assertEquals(listOf(SpeedLanding.RIGHT), right.events.map { it.landing })
        assertEquals(366L, right.events.single().timestampMillis)
    }

    @Test
    fun bothFeetLandingInSameFrame_emitsBoth() {
        val classifier = readyClassifier()

        classifier.process(frame(166L, LIFTED, LIFTED))
        val result = classifier.process(frame(199L, GROUND, GROUND))

        assertEquals(listOf(SpeedLanding.BOTH), result.events.map { it.landing })
    }

    @Test
    fun landingsWithinSimultaneousWindow_emitBoth() {
        val classifier = readyClassifier()

        classifier.process(frame(166L, LIFTED, LIFTED))
        val first = classifier.process(frame(199L, GROUND, LIFTED))
        val second = classifier.process(frame(232L, GROUND, GROUND))

        assertTrue(first.events.isEmpty())
        assertEquals(listOf(SpeedLanding.BOTH), second.events.map { it.landing })
    }

    @Test
    fun trackingLossWhileAirborne_emitsUnclearAndResetsMotionOnly() {
        val classifier = readyClassifier()
        classifier.process(frame(166L, LIFTED, GROUND))

        val lost = classifier.process(frame(199L, GROUND, GROUND, visible = false))
        val restored = classifier.process(frame(232L, GROUND, GROUND))

        assertFalse(lost.trackingValid)
        assertEquals(SpeedClassifierDiagnostic.LOW_VISIBILITY, lost.diagnostic)
        assertEquals(listOf(SpeedLanding.UNCLEAR), lost.events.map { it.landing })
        assertTrue(restored.trackingValid)
        assertEquals(1, classifier.diagnostics().trackingLossEvents)
        assertEquals(1, classifier.diagnostics().unclearLandings)
    }

    @Test
    fun lowVisibilityWhileGrounded_doesNotInventLanding() {
        val classifier = readyClassifier()

        val result = classifier.process(frame(166L, GROUND, GROUND, visible = false))

        assertTrue(result.events.isEmpty())
        assertEquals(1L, classifier.diagnostics().lowVisibilityFrames)
    }

    @Test
    fun outOfOrderTimestamp_isRejectedWithoutEvent() {
        val classifier = readyClassifier()

        val result = classifier.process(frame(120L, GROUND, GROUND))

        assertFalse(result.trackingValid)
        assertEquals(SpeedClassifierDiagnostic.OUT_OF_ORDER_TIMESTAMP, result.diagnostic)
        assertTrue(result.events.isEmpty())
        assertEquals(1L, classifier.diagnostics().outOfOrderFrames)
    }

    @Test
    fun skippedFrames_doNotCreateCompensatingEvents() {
        val classifier = readyClassifier()

        classifier.process(frame(166L, LIFTED, GROUND))
        val landing = classifier.process(frame(600L, GROUND, GROUND))
        val flushed = classifier.process(frame(800L, GROUND, GROUND))

        assertTrue(landing.events.isEmpty())
        assertEquals(1, flushed.events.size)
        assertEquals(SpeedLanding.LEFT, flushed.events.single().landing)
    }

    @Test
    fun classifierAndCounter_countOnlyRightAfterLeftUnlocks() {
        val classifier = readyClassifier()
        val detector = SpeedStepDetector().also { it.start(150L) }

        fun processAndFlush(liftAt: Long, landAt: Long, leftY: Float, rightY: Float) {
            classifier.process(frame(liftAt, leftY, rightY))
            classifier.process(frame(landAt, GROUND, GROUND))
            classifier.process(frame(landAt + 100L, GROUND, GROUND)).events.forEach {
                detector.processLanding(it.landing, it.timestampMillis)
            }
        }

        processAndFlush(166L, 199L, GROUND, LIFTED)
        processAndFlush(332L, 365L, GROUND, LIFTED)
        processAndFlush(498L, 531L, LIFTED, GROUND)
        processAndFlush(664L, 697L, GROUND, LIFTED)

        assertEquals(2, detector.diagnostics().countedRightTimestampsMillis.size)
        assertEquals(1, detector.diagnostics().repeatedRightRejects)
    }

    @Test
    fun evidence_reportsGroundedWhenAverageStaysBelowUnchangedLiftThreshold() {
        val classifier = readyClassifier(evidenceEnabled = true)

        classifier.process(
            detailedFrame(
                timestampMillis = 166L,
                leftAnkleY = 0.82f,
                leftHeelY = 0.90f,
                leftToeY = 0.90f,
            ),
        )

        val left = classifier.diagnostics().motionEvidence!!.left
        assertEquals(SpeedFootPhase.GROUNDED, left.phase)
        assertTrue(left.currentAnkleRiseRatio > 0.08f)
        assertTrue(left.currentAverageRiseRatio < 0.08f)
        assertEquals(left.currentAverageRiseRatio, left.maximumAverageRiseRatio, 0.0001f)
    }

    @Test
    fun evidence_keepsBoundedMaximumAndCanResetWindowWithoutRecalibration() {
        val classifier = readyClassifier(evidenceEnabled = true)
        classifier.process(frame(166L, leftY = LIFTED, rightY = GROUND))
        val peak = classifier.diagnostics().motionEvidence!!.left.maximumAverageRiseRatio

        classifier.process(frame(199L, leftY = GROUND, rightY = GROUND))
        assertEquals(peak, classifier.diagnostics().motionEvidence!!.left.maximumAverageRiseRatio, 0.0001f)

        classifier.resetEvidenceWindow()
        assertNull(classifier.diagnostics().motionEvidence)
        assertEquals(2, classifier.diagnostics().calibrationFrames)

        classifier.process(frame(232L, leftY = GROUND, rightY = GROUND))
        assertTrue(classifier.diagnostics().motionEvidence!!.left.maximumAverageRiseRatio < peak)
    }

    @Test
    fun evidence_reportsAirbornePhaseLatchWithoutChangingLandingEvents() {
        val classifier = readyClassifier(evidenceEnabled = true)

        val takeoff = classifier.process(frame(166L, leftY = LIFTED, rightY = GROUND))
        val nearLanding = classifier.process(frame(500L, leftY = 0.88f, rightY = GROUND))
        val latched = classifier.diagnostics().motionEvidence!!.left

        assertTrue(takeoff.events.isEmpty())
        assertTrue(nearLanding.events.isEmpty())
        assertEquals(SpeedFootPhase.AIRBORNE, latched.phase)
        assertTrue(latched.classificationRiseRatio > 0.03f)
        assertTrue(latched.classificationRiseRatio < 0.08f)
        assertEquals(334L, latched.currentAirborneDurationMillis)
        assertEquals(334L, latched.maximumAirborneDurationMillis)
        assertEquals(
            latched.classificationRiseRatio,
            latched.currentAirborneMinimumRiseRatio!!,
            0.0001f,
        )
        assertEquals(
            latched.classificationRiseRatio,
            latched.longestAirborneMinimumRiseRatio!!,
            0.0001f,
        )

        val landing = classifier.process(frame(600L, leftY = GROUND, rightY = GROUND))
        val grounded = classifier.diagnostics().motionEvidence!!.left

        assertTrue(landing.events.isEmpty())
        assertEquals(SpeedFootPhase.GROUNDED, grounded.phase)
        assertEquals(0L, grounded.currentAirborneDurationMillis)
        assertEquals(434L, grounded.maximumAirborneDurationMillis)
        assertEquals(0f, grounded.longestAirborneMinimumRiseRatio!!, 0.0001f)
    }

    @Test
    fun twoConsecutiveNearGroundFrames_conservativelyRearmLatchedFoot() {
        val classifier = readyClassifier(evidenceEnabled = true)

        classifier.process(frame(166L, leftY = LIFTED, rightY = GROUND))
        val firstNearGround = classifier.process(frame(199L, leftY = NEAR_GROUND, rightY = GROUND))
        val secondNearGround = classifier.process(frame(232L, leftY = NEAR_GROUND, rightY = GROUND))
        val flushed = classifier.process(frame(333L, leftY = NEAR_GROUND, rightY = GROUND))

        assertTrue(firstNearGround.events.isEmpty())
        assertTrue(secondNearGround.events.isEmpty())
        assertEquals(listOf(SpeedLanding.LEFT), flushed.events.map { it.landing })
        assertEquals(232L, flushed.events.single().timestampMillis)
        assertEquals(SpeedFootPhase.GROUNDED, classifier.diagnostics().motionEvidence!!.left.phase)
        assertEquals(1, classifier.diagnostics().leftConservativeRearms)
        assertEquals(0, classifier.diagnostics().rightConservativeRearms)
    }

    @Test
    fun oneNearGroundFrame_doesNotRearmOrInventLanding() {
        val classifier = readyClassifier(evidenceEnabled = true)

        classifier.process(frame(166L, leftY = LIFTED, rightY = GROUND))
        classifier.process(frame(199L, leftY = NEAR_GROUND, rightY = GROUND))
        classifier.process(frame(232L, leftY = LIFTED, rightY = GROUND))
        val result = classifier.process(frame(333L, leftY = LIFTED, rightY = GROUND))

        assertTrue(result.events.isEmpty())
        assertEquals(SpeedFootPhase.AIRBORNE, classifier.diagnostics().motionEvidence!!.left.phase)
        assertEquals(0, classifier.diagnostics().leftConservativeRearms)
    }

    @Test
    fun groundedNearBandFrames_doNotInventTakeoffOrLanding() {
        val classifier = readyClassifier(evidenceEnabled = true)

        classifier.process(frame(166L, leftY = NEAR_GROUND, rightY = GROUND))
        classifier.process(frame(199L, leftY = NEAR_GROUND, rightY = GROUND))
        val result = classifier.process(frame(300L, leftY = NEAR_GROUND, rightY = GROUND))

        assertTrue(result.events.isEmpty())
        assertEquals(SpeedFootPhase.GROUNDED, classifier.diagnostics().motionEvidence!!.left.phase)
        assertEquals(0, classifier.diagnostics().leftConservativeRearms)
    }

    @Test
    fun strictLanding_doesNotIncrementConservativeRearmEvidence() {
        val classifier = readyClassifier(evidenceEnabled = true)

        classifier.process(frame(166L, leftY = LIFTED, rightY = GROUND))
        classifier.process(frame(199L, leftY = GROUND, rightY = GROUND))
        val flushed = classifier.process(frame(300L, leftY = GROUND, rightY = GROUND))

        assertEquals(listOf(SpeedLanding.LEFT), flushed.events.map { it.landing })
        assertEquals(0, classifier.diagnostics().leftConservativeRearms)
    }

    @Test
    fun bothFeetConservativeRearm_emitsBothAndNeverCountsRight() {
        val classifier = readyClassifier(evidenceEnabled = true)
        val detector = SpeedStepDetector().also { it.start(150L) }

        classifier.process(frame(166L, leftY = LIFTED, rightY = LIFTED))
        classifier.process(frame(199L, leftY = NEAR_GROUND, rightY = NEAR_GROUND))
        val rearmed = classifier.process(frame(232L, leftY = NEAR_GROUND, rightY = NEAR_GROUND))
        rearmed.events.forEach { detector.processLanding(it.landing, it.timestampMillis) }

        assertEquals(listOf(SpeedLanding.BOTH), rearmed.events.map { it.landing })
        assertEquals(1, classifier.diagnostics().leftConservativeRearms)
        assertEquals(1, classifier.diagnostics().rightConservativeRearms)
        assertEquals(0, detector.diagnostics().countedRightTimestampsMillis.size)
        assertEquals(1, detector.diagnostics().bothFeetRejects)
    }

    @Test
    fun evidenceDisabled_doesNotExposeMotionPayload() {
        val classifier = readyClassifier(evidenceEnabled = false)

        classifier.process(frame(166L, leftY = LIFTED, rightY = GROUND))

        assertNull(classifier.diagnostics().motionEvidence)
    }

    @Test
    fun preGoCalibration_reanchorsRecentCountdownPoseAndStartsGrounded() {
        val classifier = readyClassifier(evidenceEnabled = true)

        classifier.startPreGoCalibration()
        classifier.process(frame(166L, leftY = GROUND, rightY = GROUND))
        (0 until 6).forEach { index ->
            classifier.process(
                frame(199L + index * 33L, leftY = 0.70f, rightY = 0.70f),
            )
        }
        assertEquals(SpeedFootPhase.AIRBORNE, classifier.diagnostics().motionEvidence!!.left.phase)
        classifier.commitPreGoCalibration()

        val firstGoFrame = classifier.process(frame(430L, leftY = 0.70f, rightY = 0.70f))
        val evidence = classifier.diagnostics().motionEvidence!!
        assertTrue(firstGoFrame.events.isEmpty())
        assertEquals(SpeedFootPhase.GROUNDED, evidence.left.phase)
        assertEquals(SpeedFootPhase.GROUNDED, evidence.right.phase)
        assertEquals(0f, evidence.left.currentAverageRiseRatio, 0.0001f)
        assertEquals(0f, evidence.right.currentAverageRiseRatio, 0.0001f)
    }

    @Test
    fun preGoCalibration_preservesFirstLandingCycleAfterGo() {
        val classifier = readyClassifier()
        classifier.startPreGoCalibration()
        (0 until 6).forEach { index ->
            classifier.process(
                frame(166L + index * 33L, leftY = 0.70f, rightY = 0.70f),
            )
        }
        classifier.commitPreGoCalibration()

        classifier.process(frame(400L, leftY = 0.64f, rightY = 0.70f))
        classifier.process(frame(433L, leftY = 0.70f, rightY = 0.70f))
        val flushed = classifier.process(frame(533L, leftY = 0.70f, rightY = 0.70f))

        assertEquals(listOf(SpeedLanding.LEFT), flushed.events.map { it.landing })
        assertEquals(433L, flushed.events.single().timestampMillis)
    }

    @Test
    fun cancelledPreGoCalibration_doesNotReplaceExistingBaseline() {
        val classifier = readyClassifier(evidenceEnabled = true)
        classifier.startPreGoCalibration()
        classifier.process(frame(166L, leftY = 0.70f, rightY = 0.70f))
        classifier.cancelPreGoCalibration()
        classifier.commitPreGoCalibration()

        classifier.process(frame(199L, leftY = 0.70f, rightY = 0.70f))

        assertEquals(SpeedFootPhase.AIRBORNE, classifier.diagnostics().motionEvidence!!.left.phase)
    }

    private fun readyClassifier(evidenceEnabled: Boolean = false): PoseSpeedLandingClassifier {
        val classifier = PoseSpeedLandingClassifier(
            calibrationFramesRequired = 2,
            evidenceEnabled = evidenceEnabled,
        )
        classifier.process(frame(100L, GROUND, GROUND))
        classifier.process(frame(133L, GROUND, GROUND))
        return classifier
    }

    private fun frame(
        timestampMillis: Long,
        leftY: Float,
        rightY: Float,
        visible: Boolean = true,
    ): PoseFrame {
        val points = MutableList(33) { NormalizedPoint(0.5f, 0.5f, false) }
        fun set(index: Int, x: Float, y: Float) {
            points[index] = NormalizedPoint(x, y, visible)
        }
        set(23, 0.45f, HIP)
        set(24, 0.55f, HIP)
        set(27, 0.45f, leftY)
        set(29, 0.44f, leftY)
        set(31, 0.46f, leftY)
        set(28, 0.55f, rightY)
        set(30, 0.54f, rightY)
        set(32, 0.56f, rightY)
        return PoseFrame(
            landmarks = points,
            imageWidth = 1_080,
            imageHeight = 1_920,
            sourceTimestampMillis = timestampMillis,
        )
    }

    private fun detailedFrame(
        timestampMillis: Long,
        leftAnkleY: Float = GROUND,
        leftHeelY: Float = GROUND,
        leftToeY: Float = GROUND,
        rightAnkleY: Float = GROUND,
        rightHeelY: Float = GROUND,
        rightToeY: Float = GROUND,
    ): PoseFrame {
        val points = MutableList(33) { NormalizedPoint(0.5f, 0.5f, false) }
        fun set(index: Int, x: Float, y: Float) {
            points[index] = NormalizedPoint(x, y, true)
        }
        set(23, 0.45f, HIP)
        set(24, 0.55f, HIP)
        set(27, 0.45f, leftAnkleY)
        set(29, 0.44f, leftHeelY)
        set(31, 0.46f, leftToeY)
        set(28, 0.55f, rightAnkleY)
        set(30, 0.54f, rightHeelY)
        set(32, 0.56f, rightToeY)
        return PoseFrame(
            landmarks = points,
            imageWidth = 1_080,
            imageHeight = 1_920,
            sourceTimestampMillis = timestampMillis,
        )
    }

    private companion object {
        const val HIP = 0.50f
        const val GROUND = 0.90f
        const val LIFTED = 0.84f
        const val NEAR_GROUND = 0.886f
    }
}
