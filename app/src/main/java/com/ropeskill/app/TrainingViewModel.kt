package com.ropeskill.app

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class WorkoutStatus(val displayName: String) {
    IDLE("Ready"),
    RUNNING("Running"),
    PAUSED("Paused"),
    FINISHED("Finished"),
}

data class TrainingUiState(
    val jumpCount: Int = 0,
    val elapsedMillis: Long = 0,
    val status: WorkoutStatus = WorkoutStatus.IDLE,
    val trackingStatus: BounceTrackingStatus = BounceTrackingStatus.WAITING,
)

class TrainingViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(TrainingUiState())
    val uiState: StateFlow<TrainingUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var startedAtMillis = 0L
    private val bounceDetector = BasicBounceDetector()

    fun startWorkout() {
        val currentState = _uiState.value
        if (currentState.status != WorkoutStatus.IDLE && currentState.status != WorkoutStatus.PAUSED) return

        startedAtMillis = SystemClock.elapsedRealtime() - currentState.elapsedMillis
        _uiState.update { it.copy(status = WorkoutStatus.RUNNING) }
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                updateElapsedTime()
                delay(TIMER_UPDATE_INTERVAL_MILLIS)
            }
        }
    }

    fun pauseWorkout() {
        if (_uiState.value.status != WorkoutStatus.RUNNING) return

        updateElapsedTime()
        timerJob?.cancel()
        timerJob = null
        bounceDetector.reset()
        _uiState.update {
            it.copy(status = WorkoutStatus.PAUSED, trackingStatus = BounceTrackingStatus.WAITING)
        }
    }

    fun finishWorkout() {
        if (_uiState.value.status == WorkoutStatus.RUNNING) {
            updateElapsedTime()
        }
        if (_uiState.value.status != WorkoutStatus.RUNNING && _uiState.value.status != WorkoutStatus.PAUSED) return

        timerJob?.cancel()
        timerJob = null
        bounceDetector.reset()
        _uiState.update {
            it.copy(status = WorkoutStatus.FINISHED, trackingStatus = BounceTrackingStatus.WAITING)
        }
    }

    fun resetWorkout() {
        timerJob?.cancel()
        timerJob = null
        startedAtMillis = 0L
        bounceDetector.reset()
        _uiState.value = TrainingUiState()
    }

    fun addJump() {
        if (_uiState.value.status != WorkoutStatus.RUNNING) return
        _uiState.update { it.copy(jumpCount = it.jumpCount + 1) }
    }

    fun processPoseFrame(frame: PoseFrame) {
        if (_uiState.value.status != WorkoutStatus.RUNNING) return

        val result = bounceDetector.process(frame, SystemClock.elapsedRealtime())
        _uiState.update { state ->
            if (!result.countedJump && state.trackingStatus == result.trackingStatus) {
                state
            } else {
                state.copy(
                    jumpCount = state.jumpCount + if (result.countedJump) 1 else 0,
                    trackingStatus = result.trackingStatus,
                )
            }
        }
    }

    private fun updateElapsedTime() {
        val elapsedMillis = (SystemClock.elapsedRealtime() - startedAtMillis).coerceAtLeast(0L)
        _uiState.update { it.copy(elapsedMillis = elapsedMillis) }
    }

    override fun onCleared() {
        timerJob?.cancel()
        super.onCleared()
    }

    private companion object {
        const val TIMER_UPDATE_INTERVAL_MILLIS = 100L
    }
}
