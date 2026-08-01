package com.ropeskill.app

import org.junit.Assert.assertEquals
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
    fun releaseBuild_hidesDiagnosticPanelEvenWhenDiagnosticStateIsPopulated() {
        val diagnosticState = TrainingUiState(
            diagnosticTransitionCounts = mapOf(BounceDiagnostic.AIRBORNE to 1),
            cooldownSuppressedCount = 1,
            strongHipRescueCount = 1,
        )

        assertFalse(
            shouldShowDebugDiagnosticPanel(
                isDebugBuild = false,
                cameraPermissionGranted = true,
                uiState = diagnosticState,
            ),
        )
    }

    @Test
    fun debugBuild_requiresPermissionAndDiagnosticStateForDiagnosticPanel() {
        val diagnosticState = TrainingUiState(
            diagnosticTransitionCounts = mapOf(BounceDiagnostic.AIRBORNE to 1),
        )

        assertFalse(
            shouldShowDebugDiagnosticPanel(
                isDebugBuild = true,
                cameraPermissionGranted = false,
                uiState = diagnosticState,
            ),
        )
        assertFalse(
            shouldShowDebugDiagnosticPanel(
                isDebugBuild = true,
                cameraPermissionGranted = true,
                uiState = TrainingUiState(),
            ),
        )
        assertTrue(
            shouldShowDebugDiagnosticPanel(
                isDebugBuild = true,
                cameraPermissionGranted = true,
                uiState = diagnosticState,
            ),
        )
    }

    @Test
    fun speedMode_neverShowsBasicBounceDiagnosticPanel() {
        val speedState = TrainingUiState(
            workoutMode = WorkoutMode.SPEED_30,
            diagnosticTransitionCounts = mapOf(BounceDiagnostic.AIRBORNE to 1),
        )

        assertFalse(
            shouldShowDebugDiagnosticPanel(
                isDebugBuild = true,
                cameraPermissionGranted = true,
                uiState = speedState,
            ),
        )
    }

    @Test
    fun speedTime_displaysCeilingOfRemainingThirtySeconds() {
        assertEquals("00:30", formatWorkoutTime(0L, WorkoutMode.SPEED_30))
        assertEquals("00:30", formatWorkoutTime(1L, WorkoutMode.SPEED_30))
        assertEquals("00:01", formatWorkoutTime(29_999L, WorkoutMode.SPEED_30))
        assertEquals("00:00", formatWorkoutTime(30_000L, WorkoutMode.SPEED_30))
        assertEquals("00:05", formatWorkoutTime(5_000L, WorkoutMode.BASIC_BOUNCE))
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

    @Test
    fun peakEvidenceHistory_keepsRecentAcceptedAndUpToTwoRejectedCycles() {
        var history = emptyList<TakeoffPeakEvidence>()
        listOf(1f, 2f, 3f).forEach { marker ->
            history = recordTakeoffPeakEvidence(
                history = history,
                evidence = peakEvidence(TakeoffPeakOutcome.COUNTED, marker),
                maxSize = 3,
            )
        }
        history = recordTakeoffPeakEvidence(
            history = history,
            evidence = peakEvidence(TakeoffPeakOutcome.REJECTED, 4f),
            maxSize = 3,
        )
        history = recordTakeoffPeakEvidence(
            history = history,
            evidence = peakEvidence(TakeoffPeakOutcome.REJECTED, 5f),
            maxSize = 3,
        )
        history = recordTakeoffPeakEvidence(
            history = history,
            evidence = peakEvidence(TakeoffPeakOutcome.REJECTED, 0.5f),
            maxSize = 3,
        )

        assertEquals(
            listOf(
                TakeoffPeakOutcome.COUNTED,
                TakeoffPeakOutcome.REJECTED,
                TakeoffPeakOutcome.REJECTED,
            ),
            history.map { it.outcome },
        )
        assertEquals(listOf(3f, 5f, 4f), history.map { it.rawAnkleRiseRatio })
    }

    private fun peakEvidence(
        outcome: TakeoffPeakOutcome,
        marker: Float,
    ): TakeoffPeakEvidence =
        TakeoffPeakEvidence(
            outcome = outcome,
            smoothedAnkleRiseRatio = marker,
            rawAnkleRiseRatio = marker,
            rawLeftAnkleRiseRatio = marker,
            rawRightAnkleRiseRatio = marker,
            individualAnkleRiseThreshold = 0.010f,
            leftIndividualAnkleGatePassed = true,
            rightIndividualAnkleGatePassed = true,
            smoothedHipRiseRatio = marker,
            rawHipRiseRatio = marker,
            riseFrameCount = 1,
            riseMillis = 0L,
            peakFrameIntervalMillis = null,
            nextFrameIntervalMillis = null,
            diagnostic = BounceDiagnostic.READY,
        )
}
