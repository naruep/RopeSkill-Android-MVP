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
        assertFalse(shouldShowJumpMetric(TrainingUiState()))
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
    }

    @Test
    fun jumpMetric_hidesWhileCenteredInstructionIsVisible() {
        assertFalse(
            shouldShowJumpMetric(
                TrainingUiState(
                    status = WorkoutStatus.POSITIONING,
                    hasWorkoutStarted = true,
                ),
            ),
        )
        assertFalse(
            shouldShowJumpMetric(
                TrainingUiState(
                    status = WorkoutStatus.COUNTDOWN,
                    hasWorkoutStarted = true,
                ),
            ),
        )
        assertFalse(
            shouldShowJumpMetric(
                TrainingUiState(
                    status = WorkoutStatus.RUNNING,
                    showGo = true,
                    hasWorkoutStarted = true,
                ),
            ),
        )
    }

    @Test
    fun jumpMetric_returnsAfterInstructionClears() {
        assertTrue(
            shouldShowJumpMetric(
                TrainingUiState(
                    status = WorkoutStatus.RUNNING,
                    hasWorkoutStarted = true,
                ),
            ),
        )
        assertTrue(
            shouldShowJumpMetric(
                TrainingUiState(
                    status = WorkoutStatus.PAUSED,
                    hasWorkoutStarted = true,
                ),
            ),
        )
    }
}
