package com.ropeskill.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackingLossPauseControllerTest {
    @Test
    fun shortTrackingLoss_doesNotPause() {
        val controller = TrackingLossPauseController(pauseAfterMillis = 1_000L)

        assertFalse(controller.update(trackingLost = true, timestampMillis = 100L))
        assertFalse(controller.update(trackingLost = true, timestampMillis = 1_099L))
        assertFalse(controller.update(trackingLost = false, timestampMillis = 1_100L))
    }

    @Test
    fun sustainedTrackingLoss_pausesAtThreshold() {
        val controller = TrackingLossPauseController(pauseAfterMillis = 1_000L)

        assertFalse(controller.update(trackingLost = true, timestampMillis = 100L))
        assertTrue(controller.update(trackingLost = true, timestampMillis = 1_100L))
    }

    @Test
    fun recoveredTracking_resetsLossWindow() {
        val controller = TrackingLossPauseController(pauseAfterMillis = 1_000L)

        assertFalse(controller.update(trackingLost = true, timestampMillis = 100L))
        assertFalse(controller.update(trackingLost = false, timestampMillis = 700L))
        assertFalse(controller.update(trackingLost = true, timestampMillis = 1_000L))
        assertFalse(controller.update(trackingLost = true, timestampMillis = 1_999L))
    }
}
