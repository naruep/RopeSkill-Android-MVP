package com.ropeskill.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoTestCandidateCounterTest {
    @Test
    fun missingLeft_exposesRepeatedRightInsteadOfChangingProductionCounter() {
        val result = VideoTestCandidateCounter.evaluate(
            fixedReferenceEvents = listOf(
                fixed(100L, SpeedLanding.RIGHT),
                fixed(200L, SpeedLanding.LEFT),
                fixed(300L, SpeedLanding.RIGHT),
                fixed(400L, SpeedLanding.RIGHT),
                fixed(500L, SpeedLanding.LEFT),
                fixed(600L, SpeedLanding.RIGHT),
            ),
            goTimestampMillis = 0L,
        )

        assertEquals(3, result.countedRightSteps)
        assertEquals(1, result.repeatedRightRejects)
        assertEquals(5, result.acceptedEvents)
        assertEquals(1, result.rejectedEvents)
        assertEquals(SpeedRejectReason.REPEATED_RIGHT, result.decisions[3].rejectReason)
        assertFalse(result.decisions[3].countedRightStep)
    }

    @Test
    fun oppositeFeetWithinSeventyMilliseconds_areCombinedAndRejectedAsBoth() {
        val result = VideoTestCandidateCounter.evaluate(
            fixedReferenceEvents = listOf(
                fixed(100L, SpeedLanding.LEFT, SpeedLandingDetectionMethod.STRICT),
                fixed(160L, SpeedLanding.RIGHT, SpeedLandingDetectionMethod.CONSERVATIVE_REARM),
                fixed(300L, SpeedLanding.RIGHT),
            ),
            goTimestampMillis = 0L,
        )

        assertEquals(2, result.decisions.size)
        assertEquals(SpeedLanding.BOTH, result.decisions[0].landing)
        assertEquals(SpeedLandingDetectionMethod.MIXED, result.decisions[0].landingMethod)
        assertEquals(SpeedRejectReason.BOTH_FEET, result.decisions[0].rejectReason)
        assertEquals(1, result.bothFeetRejects)
        assertEquals(1, result.countedRightSteps)
        assertTrue(result.decisions[1].countedRightStep)
    }

    @Test
    fun productionEvents_areExcludedFromCandidateEvaluation() {
        val result = VideoTestCandidateCounter.evaluate(
            fixedReferenceEvents = listOf(
                fixed(100L, SpeedLanding.RIGHT).copy(
                    detectorSource = VideoTestDetectorSource.PRODUCTION,
                ),
                fixed(200L, SpeedLanding.RIGHT),
            ),
            goTimestampMillis = 0L,
        )

        assertEquals(1, result.decisions.size)
        assertEquals(1, result.countedRightSteps)
    }

    private fun fixed(
        timestampMillis: Long,
        foot: SpeedLanding,
        method: SpeedLandingDetectionMethod = SpeedLandingDetectionMethod.STRICT,
    ) = VideoTestLandingEvent(
        detectorSource = VideoTestDetectorSource.FIXED_REFERENCE_SHADOW,
        videoTimestampMillis = timestampMillis,
        foot = foot,
        landingMethod = method,
    )
}
