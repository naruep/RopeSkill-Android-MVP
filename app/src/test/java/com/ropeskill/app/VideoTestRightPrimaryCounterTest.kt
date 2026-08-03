package com.ropeskill.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoTestRightPrimaryCounterTest {
    @Test
    fun missingLeft_doesNotRejectValidRightLandings() {
        val result = VideoTestRightPrimaryCounter.evaluate(
            fixedReferenceEvents = listOf(
                fixed(100L, SpeedLanding.RIGHT),
                fixed(600L, SpeedLanding.RIGHT),
                fixed(1_100L, SpeedLanding.RIGHT),
            ),
            goTimestampMillis = 0L,
        )

        assertEquals(3, result.countedRightSteps)
        assertEquals(3, result.acceptedRightEvents)
        assertEquals(0, result.rejectedRightEvents)
        assertEquals(500L, result.minimumAcceptedIntervalMillis)
        assertEquals(500L, result.medianAcceptedIntervalMillis)
        assertEquals(500L, result.maximumAcceptedIntervalMillis)
        assertNull(result.decisions.first().intervalSinceAcceptedMillis)
    }

    @Test
    fun rightInsideRefractory_isRejectedWithoutMovingAcceptedAnchor() {
        val result = VideoTestRightPrimaryCounter.evaluate(
            fixedReferenceEvents = listOf(
                fixed(100L, SpeedLanding.RIGHT),
                fixed(250L, SpeedLanding.RIGHT),
                fixed(600L, SpeedLanding.RIGHT),
            ),
            goTimestampMillis = 0L,
        )

        assertEquals(2, result.countedRightSteps)
        assertEquals(1, result.refractoryRejects)
        assertFalse(result.decisions[1].accepted)
        assertEquals(150L, result.decisions[1].intervalSinceAcceptedMillis)
        assertEquals(VideoTestRightPrimaryRejectReason.REFRACTORY, result.decisions[1].rejectReason)
        assertEquals(500L, result.decisions[2].intervalSinceAcceptedMillis)
    }

    @Test
    fun exactRefractoryBoundary_isAccepted() {
        val result = VideoTestRightPrimaryCounter.evaluate(
            fixedReferenceEvents = listOf(
                fixed(100L, SpeedLanding.RIGHT),
                fixed(400L, SpeedLanding.RIGHT),
            ),
            goTimestampMillis = 0L,
        )

        assertEquals(2, result.countedRightSteps)
        assertTrue(result.decisions[1].accepted)
    }

    @Test
    fun productionLeftAndBothEvents_areExcluded() {
        val result = VideoTestRightPrimaryCounter.evaluate(
            fixedReferenceEvents = listOf(
                fixed(100L, SpeedLanding.RIGHT).copy(
                    detectorSource = VideoTestDetectorSource.PRODUCTION,
                ),
                fixed(200L, SpeedLanding.LEFT),
                fixed(300L, SpeedLanding.BOTH),
                fixed(400L, SpeedLanding.RIGHT),
            ),
            goTimestampMillis = 0L,
        )

        assertEquals(1, result.decisions.size)
        assertEquals(1, result.countedRightSteps)
    }

    @Test
    fun rightBeforeGo_isRetainedAsRejectedEvidence() {
        val result = VideoTestRightPrimaryCounter.evaluate(
            fixedReferenceEvents = listOf(
                fixed(900L, SpeedLanding.RIGHT),
                fixed(1_100L, SpeedLanding.RIGHT),
            ),
            goTimestampMillis = 1_000L,
        )

        assertEquals(1, result.countedRightSteps)
        assertEquals(VideoTestRightPrimaryRejectReason.BEFORE_GO, result.decisions[0].rejectReason)
        assertTrue(result.decisions[1].accepted)
    }

    private fun fixed(
        timestampMillis: Long,
        foot: SpeedLanding,
    ) = VideoTestLandingEvent(
        detectorSource = VideoTestDetectorSource.FIXED_REFERENCE_SHADOW,
        videoTimestampMillis = timestampMillis,
        foot = foot,
        landingMethod = SpeedLandingDetectionMethod.STRICT,
    )
}
