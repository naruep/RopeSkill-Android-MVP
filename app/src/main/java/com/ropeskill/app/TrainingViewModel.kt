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
)

class TrainingViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(TrainingUiState())
    val uiState: StateFlow<TrainingUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var startedAtMillis = 0L

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
        _uiState.update { it.copy(status = WorkoutStatus.PAUSED) }
    }

    fun finishWorkout() {
        if (_uiState.value.status == WorkoutStatus.RUNNING) {
            updateElapsedTime()
        }
        if (_uiState.value.status != WorkoutStatus.RUNNING && _uiState.value.status != WorkoutStatus.PAUSED) return

        timerJob?.cancel()
        timerJob = null
        _uiState.update { it.copy(status = WorkoutStatus.FINISHED) }
    }

    fun resetWorkout() {
        timerJob?.cancel()
        timerJob = null
        startedAtMillis = 0L
        _uiState.value = TrainingUiState()
    }

    fun addJump() {
        if (_uiState.value.status != WorkoutStatus.RUNNING) return
        _uiState.update { it.copy(jumpCount = it.jumpCount + 1) }
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
