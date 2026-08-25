package com.ropeskill.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrainingDetectorProfileRoutingTest {
    @Test
    fun productionAndOrdinaryDiagnostic_keepAcceptedT738Profile() {
        val thresholds = trainingBasicBounceThresholds(t757LiveEnabled = false)

        assertEquals(T738DetectorProfiles.PRODUCTION, thresholds)
        assertFalse(thresholds.landingRearmRescueEnabled)
    }

    @Test
    fun isolatedT757LiveBuild_usesLandingRearmProfile() {
        val thresholds = trainingBasicBounceThresholds(t757LiveEnabled = true)

        assertEquals(T757DetectorProfiles.SHADOW_ONLY, thresholds)
        assertTrue(thresholds.landingRearmRescueEnabled)
        assertEquals(0.047f, thresholds.landingRearmRescueAnkleBaselineRatio)
        assertEquals(300L, thresholds.landingRearmRescueMinimumAirborneMillis)
    }
}
