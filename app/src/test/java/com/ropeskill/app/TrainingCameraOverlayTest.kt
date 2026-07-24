package com.ropeskill.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrainingCameraOverlayTest {
    @Test
    fun cameraPermissionMissing_hidesTrainingCameraOverlays() {
        assertFalse(shouldShowTrainingCameraOverlays(cameraPermissionGranted = false))
    }

    @Test
    fun cameraPermissionGranted_showsTrainingCameraOverlays() {
        assertTrue(shouldShowTrainingCameraOverlays(cameraPermissionGranted = true))
    }

    @Test
    fun workoutMetrics_hiddenBeforeFirstGo() {
        assertFalse(shouldShowWorkoutMetrics(TrainingUiState()))
        assertFalse(
            shouldShowWorkoutMetrics(
                TrainingUiState(status = WorkoutStatus.COUNTDOWN),
            ),
        )
    }

    @Test
    fun workoutMetrics_remainVisibleAfterWorkoutHasStarted() {
        assertTrue(
            shouldShowWorkoutMetrics(
                TrainingUiState(
                    status = WorkoutStatus.PAUSED,
                    hasWorkoutStarted = true,
                ),
            ),
        )
        assertTrue(
            shouldShowWorkoutMetrics(
                TrainingUiState(
                    status = WorkoutStatus.POSITIONING,
                    hasWorkoutStarted = true,
                ),
            ),
        )
    }
}
