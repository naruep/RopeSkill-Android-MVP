package com.ropeskill.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoTestAlternationGuardCounterTest {
    @Test
    fun referencePattern_countsEveryRightAndBridgesTwoMissingLeftEvents() {
        val events = mutableListOf<VideoTestLandingEvent>()
        var timestamp = 500L
        repeat(5) {
            events += fixed(timestamp, SpeedLanding.RIGHT)
            events += fixed(timestamp + 250L, SpeedLanding.LEFT)
            timestamp += 500L
        }
        events += fixed(timestamp, SpeedLanding.RIGHT)
        events += fixed(timestamp + 500L, SpeedLanding.RIGHT)
        events += fixed(timestamp + 1_000L, SpeedLanding.RIGHT)
        events += fixed(timestamp + 1_250L, SpeedLanding.LEFT)
        events += fixed(timestamp + 1_500L, SpeedLanding.RIGHT)

        val result = VideoTestAlternationGuardCounter.evaluate(events, goTimestampMillis = 0L)

        assertEquals(9, result.countedRightSteps)
        assertEquals(2, result.cadenceBridgeAccepts)
        assertEquals(0, result.rejectedRightEvents)
    }

    @Test
    fun leftOnlyPatternWithLateFalseRights_neverEstablishesAlternation() {
        val result = VideoTestAlternationGuardCounter.evaluate(
            fixedReferenceEvents = listOf(
                fixed(1_500L, SpeedLanding.LEFT),
                fixed(3_500L, SpeedLanding.LEFT),
                fixed(7_300L, SpeedLanding.LEFT),
                fixed(28_063L, SpeedLanding.RIGHT),
                fixed(29_878L, SpeedLanding.RIGHT),
            ),
            goTimestampMillis = 0L,
        )

        assertEquals(0, result.countedRightSteps)
        assertEquals(2, result.unconfirmedAlternationRejects)
        assertTrue(result.decisions.all { !it.accepted })
    }

    @Test
    fun rightOnlyPattern_neverEstablishesAlternation() {
        val result = VideoTestAlternationGuardCounter.evaluate(
            fixedReferenceEvents = listOf(
                fixed(500L, SpeedLanding.RIGHT),
                fixed(1_000L, SpeedLanding.RIGHT),
                fixed(1_500L, SpeedLanding.RIGHT),
            ),
            goTimestampMillis = 0L,
        )

        assertEquals(0, result.countedRightSteps)
        assertEquals(3, result.unconfirmedAlternationRejects)
    }

    @Test
    fun duplicateRightObjects_areTrackedSeparatelyAndCannotBootstrap() {
        val duplicate = fixed(500L, SpeedLanding.RIGHT)
        val result = VideoTestAlternationGuardCounter.evaluate(
            fixedReferenceEvents = listOf(
                duplicate,
                duplicate,
                fixed(600L, SpeedLanding.LEFT),
                fixed(700L, SpeedLanding.RIGHT),
            ),
            goTimestampMillis = 0L,
        )

        assertEquals(0, result.countedRightSteps)
        assertEquals(3, result.decisions.size)
        assertTrue(result.decisions.all { !it.accepted })
    }

    @Test
    fun alternatingSequence_canStartWithLeft() {
        val result = VideoTestAlternationGuardCounter.evaluate(
            fixedReferenceEvents = listOf(
                fixed(250L, SpeedLanding.LEFT),
                fixed(500L, SpeedLanding.RIGHT),
                fixed(750L, SpeedLanding.LEFT),
                fixed(1_000L, SpeedLanding.RIGHT),
            ),
            goTimestampMillis = 0L,
        )

        assertEquals(2, result.countedRightSteps)
        assertEquals(VideoTestAlternationGuardEvidence.CONFIRMED_SEQUENCE, result.decisions[0].evidence)
        assertEquals(VideoTestAlternationGuardEvidence.RECENT_LEFT, result.decisions[1].evidence)
    }

    @Test
    fun thirdMissingLeftExceedsBridgeLimit() {
        val result = VideoTestAlternationGuardCounter.evaluate(
            fixedReferenceEvents = listOf(
                fixed(500L, SpeedLanding.RIGHT),
                fixed(750L, SpeedLanding.LEFT),
                fixed(1_000L, SpeedLanding.RIGHT),
                fixed(1_500L, SpeedLanding.RIGHT),
                fixed(2_000L, SpeedLanding.RIGHT),
                fixed(2_500L, SpeedLanding.RIGHT),
            ),
            goTimestampMillis = 0L,
        )

        assertEquals(4, result.countedRightSteps)
        assertEquals(1, result.bridgeLimitRejects)
        assertFalse(result.decisions.last().accepted)
    }

    @Test
    fun productionAndPreGoEvents_doNotCreateAlternationEvidence() {
        val production = fixed(100L, SpeedLanding.LEFT).copy(
            detectorSource = VideoTestDetectorSource.PRODUCTION,
        )
        val result = VideoTestAlternationGuardCounter.evaluate(
            fixedReferenceEvents = listOf(
                fixed(100L, SpeedLanding.RIGHT),
                production,
                fixed(1_100L, SpeedLanding.RIGHT),
            ),
            goTimestampMillis = 1_000L,
        )

        assertEquals(0, result.countedRightSteps)
        assertEquals(VideoTestAlternationGuardRejectReason.BEFORE_GO, result.decisions[0].rejectReason)
        assertEquals(
            VideoTestAlternationGuardRejectReason.UNCONFIRMED_ALTERNATION,
            result.decisions[1].rejectReason,
        )
    }

    @Test
    fun v13RejectsRightOnlyFalseSequenceWithOneFrameTransitions() {
        val events = listOf(
            fixed(14_949L, SpeedLanding.RIGHT),
            fixed(15_411L, SpeedLanding.LEFT),
            fixed(17_589L, SpeedLanding.RIGHT),
            fixed(18_315L, SpeedLanding.LEFT),
            fixed(18_348L, SpeedLanding.RIGHT),
            fixed(24_123L, SpeedLanding.LEFT),
            fixed(24_156L, SpeedLanding.RIGHT),
        )

        val v12 = VideoTestAlternationGuardCounter.evaluate(events, goTimestampMillis = 2_000L)
        val v13 = VideoTestMinimumTransitionGapCounter.evaluate(
            events,
            goTimestampMillis = 2_000L,
        )

        assertEquals(3, v12.countedRightSteps)
        assertEquals(0, v13.countedRightSteps)
        assertEquals(4, v13.rejectedRightEvents)
        assertEquals(4, v13.unconfirmedAlternationRejects)
    }

    @Test
    fun v13KeepsReferenceTransitionsAboveMinimumGap() {
        val result = VideoTestMinimumTransitionGapCounter.evaluate(
            fixedReferenceEvents = listOf(
                fixed(500L, SpeedLanding.RIGHT),
                fixed(632L, SpeedLanding.LEFT),
                fixed(1_000L, SpeedLanding.RIGHT),
                fixed(1_132L, SpeedLanding.LEFT),
                fixed(1_500L, SpeedLanding.RIGHT),
            ),
            goTimestampMillis = 0L,
        )

        assertEquals(3, result.countedRightSteps)
        assertEquals(0, result.rejectedRightEvents)
    }

    @Test
    fun v13RejectsTooFastRecentLeftAfterValidEstablishment() {
        val result = VideoTestMinimumTransitionGapCounter.evaluate(
            fixedReferenceEvents = listOf(
                fixed(500L, SpeedLanding.RIGHT),
                fixed(750L, SpeedLanding.LEFT),
                fixed(1_000L, SpeedLanding.RIGHT),
                fixed(1_467L, SpeedLanding.LEFT),
                fixed(1_500L, SpeedLanding.RIGHT),
            ),
            goTimestampMillis = 0L,
        )

        assertEquals(2, result.countedRightSteps)
        assertEquals(1, result.transitionTooFastRejects)
        assertEquals(
            VideoTestAlternationGuardRejectReason.TRANSITION_TOO_FAST,
            result.decisions.last().rejectReason,
        )
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
