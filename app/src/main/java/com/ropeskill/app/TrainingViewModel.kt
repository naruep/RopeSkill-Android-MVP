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
    POSITIONING("Positioning"),
    COUNTDOWN("Get ready"),
    ARMED("Start"),
    RUNNING("Running"),
    PAUSED("Paused"),
    FINISHED("Finished"),
}

data class TrainingUiState(
    val jumpCount: Int = 0,
    val elapsedMillis: Long = 0,
    val status: WorkoutStatus = WorkoutStatus.IDLE,
    val trackingStatus: BounceTrackingStatus = BounceTrackingStatus.WAITING,
    val countdownSeconds: Int? = null,
    val showGo: Boolean = false,
)

class TrainingViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(TrainingUiState())
    val uiState: StateFlow<TrainingUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var countdownJob: Job? = null
    private var cueJob: Job? = null
    private var startedAtMillis = 0L
    private val bounceDetector = BasicBounceDetector()

    fun startWorkout() {
        val currentState = _uiState.value
        if (currentState.status != WorkoutStatus.IDLE && currentState.status != WorkoutStatus.PAUSED) return

        cancelPreparationJobs()
        bounceDetector.reset()
        _uiState.update {
            it.copy(
                status = WorkoutStatus.POSITIONING,
                trackingStatus = BounceTrackingStatus.WAITING,
                countdownSeconds = null,
                showGo = false,
            )
        }
    }

    fun pauseWorkout() {
        if (_uiState.value.status !in ACTIVE_STATUSES) return

        if (_uiState.value.status == WorkoutStatus.RUNNING) updateElapsedTime()
        timerJob?.cancel()
        timerJob = null
        cancelPreparationJobs()
        bounceDetector.reset()
        _uiState.update {
            it.copy(
                status = WorkoutStatus.PAUSED,
                trackingStatus = BounceTrackingStatus.WAITING,
                countdownSeconds = null,
                showGo = false,
            )
        }
    }

    fun finishWorkout() {
        if (_uiState.value.status == WorkoutStatus.RUNNING) {
            updateElapsedTime()
        }
        if (_uiState.value.status !in ACTIVE_STATUSES && _uiState.value.status != WorkoutStatus.PAUSED) return

        timerJob?.cancel()
        timerJob = null
        cancelPreparationJobs()
        bounceDetector.reset()
        _uiState.update {
            it.copy(status = WorkoutStatus.FINISHED, trackingStatus = BounceTrackingStatus.WAITING)
        }
    }

    fun resetWorkout() {
        timerJob?.cancel()
        timerJob = null
        cancelPreparationJobs()
        startedAtMillis = 0L
        bounceDetector.reset()
        _uiState.value = TrainingUiState()
    }

    fun addJump() {
        if (_uiState.value.status != WorkoutStatus.RUNNING) return
        _uiState.update { it.copy(jumpCount = it.jumpCount + 1) }
    }

    fun processPoseFrame(frame: PoseFrame) {
        if (_uiState.value.status !in ACTIVE_STATUSES) return

        val result = bounceDetector.process(frame, SystemClock.elapsedRealtime())
        when (_uiState.value.status) {
            WorkoutStatus.POSITIONING -> {
                if (result.trackingStatus == BounceTrackingStatus.READY) startCountdown()
            }
            WorkoutStatus.COUNTDOWN -> {
                if (result.trackingStatus != BounceTrackingStatus.READY || !result.isStable) {
                    cancelCountdown()
                }
            }
            WorkoutStatus.ARMED -> {
                if (result.event == BounceEvent.TAKEOFF) beginRunning()
            }
            else -> Unit
        }

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

    private fun startCountdown() {
        if (_uiState.value.status != WorkoutStatus.POSITIONING) return
        countdownJob?.cancel()
        _uiState.update {
            it.copy(status = WorkoutStatus.COUNTDOWN, countdownSeconds = COUNTDOWN_SECONDS)
        }
        countdownJob = viewModelScope.launch {
            for (seconds in COUNTDOWN_SECONDS downTo 1) {
                _uiState.update { it.copy(countdownSeconds = seconds) }
                delay(ONE_SECOND_MILLIS)
                if (_uiState.value.status != WorkoutStatus.COUNTDOWN) return@launch
            }
            _uiState.update {
                it.copy(status = WorkoutStatus.ARMED, countdownSeconds = null)
            }
            countdownJob = null
        }
    }

    private fun cancelCountdown() {
        countdownJob?.cancel()
        countdownJob = null
        bounceDetector.reset()
        _uiState.update {
            it.copy(
                status = WorkoutStatus.POSITIONING,
                countdownSeconds = null,
                trackingStatus = BounceTrackingStatus.WAITING,
            )
        }
    }

    private fun beginRunning() {
        if (_uiState.value.status != WorkoutStatus.ARMED) return
        startedAtMillis = SystemClock.elapsedRealtime() - _uiState.value.elapsedMillis
        _uiState.update { it.copy(status = WorkoutStatus.RUNNING, showGo = true) }
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                updateElapsedTime()
                delay(TIMER_UPDATE_INTERVAL_MILLIS)
            }
        }
        cueJob?.cancel()
        cueJob = viewModelScope.launch {
            delay(GO_CUE_DURATION_MILLIS)
            _uiState.update { it.copy(showGo = false) }
        }
    }

    private fun cancelPreparationJobs() {
        countdownJob?.cancel()
        countdownJob = null
        cueJob?.cancel()
        cueJob = null
    }

    private fun updateElapsedTime() {
        val elapsedMillis = (SystemClock.elapsedRealtime() - startedAtMillis).coerceAtLeast(0L)
        _uiState.update { it.copy(elapsedMillis = elapsedMillis) }
    }

    override fun onCleared() {
        timerJob?.cancel()
        cancelPreparationJobs()
        super.onCleared()
    }

    private companion object {
        const val TIMER_UPDATE_INTERVAL_MILLIS = 100L
        const val ONE_SECOND_MILLIS = 1_000L
        const val GO_CUE_DURATION_MILLIS = 700L
        const val COUNTDOWN_SECONDS = 5
        val ACTIVE_STATUSES = setOf(
            WorkoutStatus.POSITIONING,
            WorkoutStatus.COUNTDOWN,
            WorkoutStatus.ARMED,
            WorkoutStatus.RUNNING,
        )
    }
}
