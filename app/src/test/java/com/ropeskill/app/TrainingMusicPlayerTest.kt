package com.ropeskill.app

import org.junit.Assert.assertEquals
import org.junit.Test

class TrainingMusicPlayerTest {
    @Test
    fun cueDuckingReducesConfiguredMusicVolume() {
        assertEquals(
            0.1f,
            resolvedTrainingMusicVolume(
                configuredVolume = 0.5f,
                muted = false,
                ducked = true,
            ),
            0f,
        )
    }

    @Test
    fun userMuteTakesPriorityOverCueDucking() {
        assertEquals(
            0f,
            resolvedTrainingMusicVolume(
                configuredVolume = 0.5f,
                muted = true,
                ducked = true,
            ),
            0f,
        )
    }

    @Test
    fun configuredVolumeReturnsAfterCue() {
        assertEquals(
            0.5f,
            resolvedTrainingMusicVolume(
                configuredVolume = 0.5f,
                muted = false,
                ducked = false,
            ),
            0f,
        )
    }
}
