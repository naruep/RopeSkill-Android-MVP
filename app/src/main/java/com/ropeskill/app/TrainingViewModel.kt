package com.ropeskill.app

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

enum class WorkoutStatus(val displayName: String) {
    IDLE("Ready"),
    POSITIONING("Positioning"),
    COUNTDOWN("Ready"),
    ARMED("Start"),
    RUNNING("Running"),
    PAUSED("Paused"),
    FINISHED("Finished"),
}

enum class WorkoutMode(val displayName: String) {
    BASIC_BOUNCE("Basic Bounce"),
    SPEED_30("Speed 30"),
}

data class TrainingUiState(
    val workoutMode: WorkoutMode = WorkoutMode.BASIC_BOUNCE,
    val jumpCount: Int = 0,
    val elapsedMillis: Long = 0,
    val status: WorkoutStatus = WorkoutStatus.IDLE,
    val trackingStatus: BounceTrackingStatus = BounceTrackingStatus.WAITING,
    val detectorDiagnostic: BounceDiagnostic = BounceDiagnostic.FULL_BODY_REQUIRED,
    val positioningGuidance: PositioningGuidance = PositioningGuidance.FULL_BODY_REQUIRED,
    val lastCountEvidence: CountEvidence? = null,
    val countEvidenceHistory: List<CountEvidence> = emptyList(),
    val rejectedTakeoffEvidenceHistory: List<RejectedTakeoffEvidence> = emptyList(),
    val diagnosticTransitionCounts: Map<BounceDiagnostic, Int> = emptyMap(),
    val cooldownSuppressedCount: Int = 0,
    val lastCooldownSuppressedEvidence: CooldownSuppressedEvidence? = null,
    val strongHipRescueCount: Int = 0,
    val cycleTraceHistory: List<CycleTraceEvidence> = emptyList(),
    val takeoffPeakEvidenceHistory: List<TakeoffPeakEvidence> = emptyList(),
    val t729ExperimentSnapshot: T729ExperimentSnapshot? = null,
    val t733RaCandidateSnapshot: T733RaCandidateSnapshot? = null,
    val t735TakeoffGateSnapshot: T735TakeoffGateSnapshot? = null,
    val t730AttributionSnapshot: T730AttributionSnapshot? = null,
    val t743LandingStateSnapshot: T743LandingStateSnapshot? = null,
    val countdownSeconds: Int? = null,
    val showGo: Boolean = false,
    val hasWorkoutStarted: Boolean = false,
    val musicAvailable: Boolean = false,
    val musicTrackName: String = "",
    val musicMuted: Boolean = false,
    val musicPlaybackError: String? = null,
    val speedClassifierDiagnostic: SpeedClassifierDiagnostic =
        SpeedClassifierDiagnostic.CALIBRATING,
    val speedStepDiagnostics: SpeedStepDiagnostics? = null,
    val speedClassifierDiagnostics: SpeedLandingClassifierDiagnostics? = null,
    val speedPerformanceSnapshot: PosePerformanceSnapshot = PosePerformanceSnapshot(),
)

class TrainingViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(TrainingUiState())
    val uiState: StateFlow<TrainingUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var countdownJob: Job? = null
    private var cueJob: Job? = null
    private var startedAtMillis = 0L
    private var workoutCountdownSeconds = DEFAULT_COUNTDOWN_SECONDS
    // T-736 keeps the completed T-733 shadows off and traces the tuned production detector.
    private val detectorExperiment = T733RaCandidateShadowRunner(
        shadowEnabled = false,
        productionThresholds = T736DetectorProfiles.PRODUCTION,
        landingStateEvidenceEnabled = BuildConfig.DEBUG,
    )
    private val t735TakeoffGateTrace = T735PassiveTakeoffGateCollector(
        enabled = false,
    )
    private val t730Attribution = T730PassiveGateAttributionCollector(
        enabled = false,
    )
    private val t743LandingStateTrace = T743PassiveLandingStateCollector(
        enabled = BuildConfig.DEBUG,
    )
    private val positioningGuide = PositioningGuide()
    private val trackingLossPauseController = TrackingLossPauseController()
    private val speedLandingClassifier = PoseSpeedLandingClassifier()
    private val speedStepDetector = SpeedStepDetector()
    private val trainingMusicPlayer = TrainingMusicPlayer(application) { errorCode ->
        _uiState.update {
            it.copy(musicPlaybackError = "Music could not be played ($errorCode)")
        }
    }
    private val sessionRepository = SessionRepository.create(application)
    private var sessionStartedAtEpochMillis = 0L

    val latestSavedSession: StateFlow<TrainingSession?> =
        sessionRepository.latestSession.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null,
        )

    val savedSessions: StateFlow<List<TrainingSession>> =
        sessionRepository.sessions.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList(),
        )

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            sessionRepository.delete(sessionId)
        }
    }

    fun configureCountdownSeconds(seconds: Int) {
        require(seconds in SUPPORTED_COUNTDOWN_SECONDS)
        if (_uiState.value.status == WorkoutStatus.IDLE) {
            workoutCountdownSeconds = seconds
        }
    }

    fun selectWorkoutMode(mode: WorkoutMode) {
        if (_uiState.value.status != WorkoutStatus.IDLE) return
        _uiState.update { it.copy(workoutMode = mode) }
    }

    fun configureTrainingMusic(settings: UserSettings) {
        if (_uiState.value.status != WorkoutStatus.IDLE) return
        val available =
            settings.trainingMusicEnabled && settings.trainingMusicUri.isNotBlank()
        trainingMusicPlayer.configure(
            enabled = available,
            uri = settings.trainingMusicUri,
            volume = settings.trainingMusicVolume,
        )
        _uiState.update {
            it.copy(
                musicAvailable = available,
                musicTrackName = settings.trainingMusicName,
                musicMuted = false,
                musicPlaybackError = null,
            )
        }
    }

    fun toggleTrainingMusicMuted() {
        val state = _uiState.value
        if (!state.musicAvailable) return
        val muted = !state.musicMuted
        trainingMusicPlayer.setMuted(muted)
        _uiState.update { it.copy(musicMuted = muted) }
    }

    fun startWorkout() {
        val currentState = _uiState.value
        if (currentState.status != WorkoutStatus.IDLE && currentState.status != WorkoutStatus.PAUSED) return

        cancelPreparationJobs()
        detectorExperiment.reset()
        t735TakeoffGateTrace.reset()
        t730Attribution.reset()
        t743LandingStateTrace.reset()
        trackingLossPauseController.reset()
        speedLandingClassifier.reset()
        if (currentState.status == WorkoutStatus.IDLE) speedStepDetector.reset()
        _uiState.update {
            it.copy(
                status = WorkoutStatus.POSITIONING,
                trackingStatus = BounceTrackingStatus.WAITING,
                detectorDiagnostic = BounceDiagnostic.FULL_BODY_REQUIRED,
                positioningGuidance = PositioningGuidance.FULL_BODY_REQUIRED,
                lastCountEvidence = null,
                countEvidenceHistory = emptyList(),
                rejectedTakeoffEvidenceHistory = emptyList(),
                diagnosticTransitionCounts = emptyMap(),
                cooldownSuppressedCount = 0,
                lastCooldownSuppressedEvidence = null,
                strongHipRescueCount = 0,
                cycleTraceHistory = emptyList(),
                takeoffPeakEvidenceHistory = emptyList(),
                t729ExperimentSnapshot = null,
                t733RaCandidateSnapshot = null,
                t735TakeoffGateSnapshot = null,
                t730AttributionSnapshot = null,
                t743LandingStateSnapshot = null,
                countdownSeconds = null,
                showGo = false,
                speedClassifierDiagnostic = SpeedClassifierDiagnostic.CALIBRATING,
                speedStepDiagnostics = speedStepDetector.diagnostics(),
                speedClassifierDiagnostics = speedLandingClassifier.diagnostics(),
            )
        }
    }

    fun pauseWorkout() {
        if (_uiState.value.status !in ACTIVE_STATUSES) return

        if (
            _uiState.value.workoutMode == WorkoutMode.SPEED_30 &&
            _uiState.value.status == WorkoutStatus.RUNNING
        ) {
            finishWorkout()
            return
        }

        trainingMusicPlayer.pause()
        if (_uiState.value.status == WorkoutStatus.RUNNING) updateElapsedTime()
        timerJob?.cancel()
        timerJob = null
        cancelPreparationJobs()
        detectorExperiment.reset()
        t735TakeoffGateTrace.reset()
        t730Attribution.reset()
        t743LandingStateTrace.reset()
        trackingLossPauseController.reset()
        speedLandingClassifier.reset()
        _uiState.update {
            it.copy(
                status = WorkoutStatus.PAUSED,
                trackingStatus = BounceTrackingStatus.WAITING,
                detectorDiagnostic = BounceDiagnostic.FULL_BODY_REQUIRED,
                positioningGuidance = PositioningGuidance.FULL_BODY_REQUIRED,
                lastCountEvidence = null,
                countEvidenceHistory = emptyList(),
                rejectedTakeoffEvidenceHistory = emptyList(),
                diagnosticTransitionCounts = emptyMap(),
                cooldownSuppressedCount = 0,
                lastCooldownSuppressedEvidence = null,
                strongHipRescueCount = 0,
                cycleTraceHistory = emptyList(),
                takeoffPeakEvidenceHistory = emptyList(),
                t729ExperimentSnapshot = null,
                t733RaCandidateSnapshot = null,
                t735TakeoffGateSnapshot = null,
                t730AttributionSnapshot = null,
                t743LandingStateSnapshot = null,
                countdownSeconds = null,
                showGo = false,
            )
        }
    }

    fun finishWorkout() {
        if (_uiState.value.status == WorkoutStatus.RUNNING) {
            updateElapsedTime()
            if (_uiState.value.workoutMode == WorkoutMode.SPEED_30) {
                processSpeedLandingEvents(speedLandingClassifier.flushPending())
            }
        }
        if (_uiState.value.status !in ACTIVE_STATUSES && _uiState.value.status != WorkoutStatus.PAUSED) return

        val completedState = _uiState.value
        val completedAtEpochMillis = System.currentTimeMillis()
        timerJob?.cancel()
        timerJob = null
        cancelPreparationJobs()
        trainingMusicPlayer.stopAndRewind()
        detectorExperiment.reset()
        t735TakeoffGateTrace.reset()
        t730Attribution.reset()
        t743LandingStateTrace.reset()
        trackingLossPauseController.reset()
        _uiState.update {
            it.copy(
                status = WorkoutStatus.FINISHED,
                trackingStatus = BounceTrackingStatus.WAITING,
                t729ExperimentSnapshot = null,
                t733RaCandidateSnapshot = null,
                t735TakeoffGateSnapshot = null,
                t730AttributionSnapshot = null,
                t743LandingStateSnapshot = null,
            )
        }
        if (
            completedState.workoutMode == WorkoutMode.BASIC_BOUNCE &&
            shouldPersistSession(completedState.elapsedMillis)
        ) {
            val startedAtEpochMillis = sessionStartedAtEpochMillis.takeIf { it > 0L }
                ?: (completedAtEpochMillis - completedState.elapsedMillis)
            viewModelScope.launch {
                sessionRepository.save(
                    NewTrainingSession(
                        startedAtEpochMillis = startedAtEpochMillis,
                        completedAtEpochMillis = completedAtEpochMillis,
                        durationMillis = completedState.elapsedMillis,
                        jumpCount = completedState.jumpCount,
                    ),
                )
            }
        }
    }

    fun resetWorkout(mode: WorkoutMode = _uiState.value.workoutMode) {
        trainingMusicPlayer.stopAndRewind()
        timerJob?.cancel()
        timerJob = null
        cancelPreparationJobs()
        startedAtMillis = 0L
        sessionStartedAtEpochMillis = 0L
        detectorExperiment.reset()
        t735TakeoffGateTrace.reset()
        t730Attribution.reset()
        t743LandingStateTrace.reset()
        trackingLossPauseController.reset()
        speedLandingClassifier.reset()
        speedStepDetector.reset()
        _uiState.value = TrainingUiState(workoutMode = mode)
    }

    fun addJump() {
        if (_uiState.value.status != WorkoutStatus.RUNNING) return
        _uiState.update { it.copy(jumpCount = it.jumpCount + 1) }
    }

    fun processPoseFrame(frame: PoseFrame) {
        if (_uiState.value.status !in ACTIVE_STATUSES) {
            trackingLossPauseController.reset()
            return
        }

        val preparationStatus = _uiState.value.status
        if (
            preparationStatus == WorkoutStatus.POSITIONING ||
            preparationStatus == WorkoutStatus.COUNTDOWN
        ) {
            val guidance = positioningGuide.evaluate(frame)
            _uiState.update { it.copy(positioningGuidance = guidance) }
            if (guidance != PositioningGuidance.DISTANCE_GOOD) {
                detectorExperiment.reset()
                t735TakeoffGateTrace.reset()
                t743LandingStateTrace.reset()
                if (preparationStatus == WorkoutStatus.COUNTDOWN) {
                    cancelCountdown(guidance)
                } else {
                    _uiState.update {
                        it.copy(
                            trackingStatus = BounceTrackingStatus.WAITING,
                            detectorDiagnostic = BounceDiagnostic.FULL_BODY_REQUIRED,
                        )
                    }
                }
                return
            }
        }

        if (_uiState.value.workoutMode == WorkoutMode.SPEED_30) {
            processSpeedPoseFrame(frame)
            return
        }

        val timestampMillis = SystemClock.elapsedRealtime()
        val result = detectorExperiment.process(frame, timestampMillis)
        val t733RaCandidateSnapshot = detectorExperiment.takePublishedSnapshot()
        val t735TakeoffGateSnapshot = t735TakeoffGateTrace.record(
            frame = frame,
            result = result,
            timestampMillis = timestampMillis,
        )
        val t730AttributionSnapshot = t730Attribution.record(
            result = result,
            timestampMillis = timestampMillis,
        )
        val t743LandingStateSnapshot = t743LandingStateTrace.record(
            frame = frame,
            result = result,
            timestampMillis = timestampMillis,
        )
        val trackingLostWhileRunning =
            _uiState.value.status == WorkoutStatus.RUNNING &&
                result.trackingStatus == BounceTrackingStatus.WAITING &&
                result.diagnostic == BounceDiagnostic.FULL_BODY_REQUIRED
        if (
            trackingLossPauseController.update(
                trackingLost = trackingLostWhileRunning,
                timestampMillis = timestampMillis,
            )
        ) {
            pauseWorkout()
            return
        }
        when (_uiState.value.status) {
            WorkoutStatus.POSITIONING -> {
                if (result.trackingStatus == BounceTrackingStatus.READY) startCountdown()
            }
            WorkoutStatus.COUNTDOWN -> {
                if (result.trackingStatus != BounceTrackingStatus.READY) {
                    cancelCountdown(PositioningGuidance.DISTANCE_GOOD)
                }
            }
            WorkoutStatus.ARMED -> {
                if (result.event == BounceEvent.TAKEOFF) beginRunning()
            }
            else -> Unit
        }

        _uiState.update { state ->
            val countedEvidence = result.lastCountEvidence.takeIf {
                BuildConfig.DEBUG && result.countedJump
            }
            val rejectedTakeoffEvidence =
                result.rejectedTakeoffEvidence.takeIf {
                    BuildConfig.DEBUG && state.status == WorkoutStatus.RUNNING
                }
            val cooldownSuppressedEvidence =
                result.cooldownSuppressedEvidence.takeIf {
                    BuildConfig.DEBUG && state.status == WorkoutStatus.RUNNING
                }
            val cycleTraceEvidence =
                result.cycleTraceEvidence.takeIf {
                    BuildConfig.DEBUG && state.status == WorkoutStatus.RUNNING
                }
            val takeoffPeakEvidence =
                result.takeoffPeakEvidence.takeIf {
                    BuildConfig.DEBUG && state.status == WorkoutStatus.RUNNING
                }
            if (!result.countedJump &&
                rejectedTakeoffEvidence == null &&
                cooldownSuppressedEvidence == null &&
                cycleTraceEvidence == null &&
                takeoffPeakEvidence == null &&
                t733RaCandidateSnapshot == null &&
                t735TakeoffGateSnapshot == null &&
                t730AttributionSnapshot == null &&
                t743LandingStateSnapshot == null &&
                state.trackingStatus == result.trackingStatus &&
                state.detectorDiagnostic == result.diagnostic
            ) {
                state
            } else {
                val diagnosticTransitionCounts = if (
                    BuildConfig.DEBUG &&
                    state.status == WorkoutStatus.RUNNING &&
                    state.detectorDiagnostic != result.diagnostic
                ) {
                    recordDiagnosticTransition(
                        counts = state.diagnosticTransitionCounts,
                        diagnostic = result.diagnostic,
                    )
                } else {
                    state.diagnosticTransitionCounts
                }
                state.copy(
                    jumpCount = state.jumpCount + if (result.countedJump) 1 else 0,
                    trackingStatus = result.trackingStatus,
                    detectorDiagnostic = result.diagnostic,
                    lastCountEvidence = if (BuildConfig.DEBUG) {
                        result.lastCountEvidence ?: state.lastCountEvidence
                    } else {
                        null
                    },
                    countEvidenceHistory = countedEvidence?.let {
                        (state.countEvidenceHistory + it).takeLast(MAX_EVIDENCE_HISTORY)
                    } ?: state.countEvidenceHistory,
                    rejectedTakeoffEvidenceHistory = rejectedTakeoffEvidence?.let {
                        (state.rejectedTakeoffEvidenceHistory + it)
                            .sortedByDescending { evidence -> evidence.ankleRiseRatio }
                            .take(MAX_EVIDENCE_HISTORY)
                    } ?: state.rejectedTakeoffEvidenceHistory,
                    diagnosticTransitionCounts = diagnosticTransitionCounts,
                    cooldownSuppressedCount = state.cooldownSuppressedCount +
                        if (cooldownSuppressedEvidence != null) 1 else 0,
                    lastCooldownSuppressedEvidence =
                        cooldownSuppressedEvidence
                            ?: state.lastCooldownSuppressedEvidence,
                    strongHipRescueCount = if (BuildConfig.DEBUG) {
                        state.strongHipRescueCount +
                            if (countedEvidence?.usedStrongHipRescue == true) 1 else 0
                    } else {
                        0
                    },
                    cycleTraceHistory = cycleTraceEvidence?.let {
                        (state.cycleTraceHistory + it).takeLast(MAX_CYCLE_TRACE_HISTORY)
                    } ?: state.cycleTraceHistory,
                    takeoffPeakEvidenceHistory = takeoffPeakEvidence?.let {
                        recordTakeoffPeakEvidence(
                            history = state.takeoffPeakEvidenceHistory,
                            evidence = it,
                            maxSize = MAX_TAKEOFF_PEAK_HISTORY,
                        )
                    } ?: state.takeoffPeakEvidenceHistory,
                    t733RaCandidateSnapshot =
                        t733RaCandidateSnapshot
                            ?: state.t733RaCandidateSnapshot,
                    t735TakeoffGateSnapshot =
                        t735TakeoffGateSnapshot
                            ?: state.t735TakeoffGateSnapshot,
                    t730AttributionSnapshot =
                        t730AttributionSnapshot
                            ?: state.t730AttributionSnapshot,
                    t743LandingStateSnapshot =
                        t743LandingStateSnapshot
                            ?: state.t743LandingStateSnapshot,
                )
            }
        }
    }

    fun updatePosePerformance(snapshot: PosePerformanceSnapshot) {
        if (_uiState.value.workoutMode != WorkoutMode.SPEED_30) return
        _uiState.update { it.copy(speedPerformanceSnapshot = snapshot) }
    }

    private fun processSpeedPoseFrame(frame: PoseFrame) {
        val result = speedLandingClassifier.process(frame)
        val timestampMillis = frame.sourceTimestampMillis.takeIf { it > 0L }
            ?: SystemClock.uptimeMillis()
        val status = _uiState.value.status

        if (status == WorkoutStatus.RUNNING) {
            if (result.trackingValid) {
                speedStepDetector.onTrackingRestored(timestampMillis)
            } else {
                speedStepDetector.onTrackingLost(timestampMillis)
            }
            processSpeedLandingEvents(result.events)
        }

        when (status) {
            WorkoutStatus.POSITIONING -> {
                if (
                    result.trackingValid &&
                    result.diagnostic == SpeedClassifierDiagnostic.READY
                ) {
                    startCountdown()
                }
            }
            WorkoutStatus.COUNTDOWN -> {
                if (!result.trackingValid) {
                    cancelCountdown(PositioningGuidance.DISTANCE_GOOD)
                }
            }
            else -> Unit
        }

        _uiState.update {
            it.copy(
                trackingStatus = if (result.trackingValid) {
                    BounceTrackingStatus.READY
                } else {
                    BounceTrackingStatus.WAITING
                },
                speedClassifierDiagnostic = result.diagnostic,
                speedStepDiagnostics = speedStepDetector.diagnostics(),
                speedClassifierDiagnostics = speedLandingClassifier.diagnostics(),
            )
        }
    }

    private fun processSpeedLandingEvents(events: List<SpeedLandingEvent>) {
        if (_uiState.value.status != WorkoutStatus.RUNNING) return
        var count = _uiState.value.jumpCount
        events.forEach { event ->
            count = speedStepDetector.processLanding(
                landing = event.landing,
                timestampMillis = event.timestampMillis,
            ).count
        }
        _uiState.update {
            it.copy(
                jumpCount = count,
                speedStepDiagnostics = speedStepDetector.diagnostics(),
                speedClassifierDiagnostics = speedLandingClassifier.diagnostics(),
            )
        }
    }

    private fun startCountdown() {
        if (_uiState.value.status != WorkoutStatus.POSITIONING) return
        countdownJob?.cancel()
        _uiState.update {
            it.copy(status = WorkoutStatus.COUNTDOWN, countdownSeconds = workoutCountdownSeconds)
        }
        countdownJob = viewModelScope.launch {
            for (seconds in workoutCountdownSeconds downTo 1) {
                _uiState.update { it.copy(countdownSeconds = seconds) }
                delay(ONE_SECOND_MILLIS)
                if (_uiState.value.status != WorkoutStatus.COUNTDOWN) return@launch
            }
            _uiState.update {
                it.copy(status = WorkoutStatus.ARMED, countdownSeconds = null)
            }
            countdownJob = null
            beginRunning()
        }
    }

    private fun cancelCountdown(guidance: PositioningGuidance) {
        countdownJob?.cancel()
        countdownJob = null
        detectorExperiment.reset()
        t735TakeoffGateTrace.reset()
        t730Attribution.reset()
        t743LandingStateTrace.reset()
        _uiState.update {
            it.copy(
                status = WorkoutStatus.POSITIONING,
                countdownSeconds = null,
                trackingStatus = BounceTrackingStatus.WAITING,
                detectorDiagnostic = BounceDiagnostic.FULL_BODY_REQUIRED,
                positioningGuidance = guidance,
                lastCountEvidence = null,
                countEvidenceHistory = emptyList(),
                rejectedTakeoffEvidenceHistory = emptyList(),
                diagnosticTransitionCounts = emptyMap(),
                cooldownSuppressedCount = 0,
                lastCooldownSuppressedEvidence = null,
                strongHipRescueCount = 0,
                cycleTraceHistory = emptyList(),
                takeoffPeakEvidenceHistory = emptyList(),
                t729ExperimentSnapshot = null,
                t733RaCandidateSnapshot = null,
                t735TakeoffGateSnapshot = null,
                t730AttributionSnapshot = null,
                t743LandingStateSnapshot = null,
            )
        }
    }

    private fun beginRunning() {
        if (_uiState.value.status != WorkoutStatus.ARMED) return
        trackingLossPauseController.reset()
        val t733RaCandidateSnapshot = detectorExperiment.startMeasurement()
        val measurementStartedAtMillis = SystemClock.elapsedRealtime()
        if (_uiState.value.workoutMode == WorkoutMode.SPEED_30) {
            // PoseDetector timestamps use uptimeMillis; the Speed window must use the same clock.
            speedStepDetector.start(SystemClock.uptimeMillis())
        }
        val t735TakeoffGateSnapshot =
            t735TakeoffGateTrace.startMeasurement(measurementStartedAtMillis)
        val t730AttributionSnapshot = t730Attribution.startMeasurement(
            timestampMillis = measurementStartedAtMillis,
        )
        val t743LandingStateSnapshot =
            t743LandingStateTrace.startMeasurement(measurementStartedAtMillis)
        startedAtMillis = measurementStartedAtMillis - _uiState.value.elapsedMillis
        if (sessionStartedAtEpochMillis == 0L) {
            sessionStartedAtEpochMillis =
                System.currentTimeMillis() - _uiState.value.elapsedMillis
        }
        _uiState.update {
            it.copy(
                status = WorkoutStatus.RUNNING,
                showGo = true,
                hasWorkoutStarted = true,
                t733RaCandidateSnapshot = t733RaCandidateSnapshot,
                t735TakeoffGateSnapshot = t735TakeoffGateSnapshot,
                t730AttributionSnapshot = t730AttributionSnapshot,
                t743LandingStateSnapshot = t743LandingStateSnapshot,
            )
        }
        if (_uiState.value.musicAvailable) {
            trainingMusicPlayer.play()
        }
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                updateElapsedTime()
                if (
                    _uiState.value.workoutMode == WorkoutMode.SPEED_30 &&
                    _uiState.value.elapsedMillis >= SpeedStepDetector.SPEED_30_DURATION_MILLIS
                ) {
                    finishWorkout()
                    break
                }
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
        _uiState.update {
            it.copy(
                elapsedMillis = if (it.workoutMode == WorkoutMode.SPEED_30) {
                    elapsedMillis.coerceAtMost(SpeedStepDetector.SPEED_30_DURATION_MILLIS)
                } else {
                    elapsedMillis
                },
            )
        }
    }

    override fun onCleared() {
        timerJob?.cancel()
        cancelPreparationJobs()
        trainingMusicPlayer.release()
        super.onCleared()
    }

    private companion object {
        const val TIMER_UPDATE_INTERVAL_MILLIS = 100L
        const val ONE_SECOND_MILLIS = 1_000L
        const val GO_CUE_DURATION_MILLIS = 700L
        const val DEFAULT_COUNTDOWN_SECONDS = 5
        const val MAX_EVIDENCE_HISTORY = 3
        const val MAX_CYCLE_TRACE_HISTORY = 6
        const val MAX_TAKEOFF_PEAK_HISTORY = 3
        val ACTIVE_STATUSES = setOf(
            WorkoutStatus.POSITIONING,
            WorkoutStatus.COUNTDOWN,
            WorkoutStatus.ARMED,
            WorkoutStatus.RUNNING,
        )
    }
}

internal fun shouldPersistSession(elapsedMillis: Long): Boolean = elapsedMillis > 0L

internal fun recordDiagnosticTransition(
    counts: Map<BounceDiagnostic, Int>,
    diagnostic: BounceDiagnostic,
): Map<BounceDiagnostic, Int> {
    if (diagnostic !in EXPERIMENT_DIAGNOSTICS) return counts
    return counts + (diagnostic to (counts[diagnostic] ?: 0) + 1)
}

internal fun recordTakeoffPeakEvidence(
    history: List<TakeoffPeakEvidence>,
    evidence: TakeoffPeakEvidence,
    maxSize: Int,
): List<TakeoffPeakEvidence> {
    require(maxSize > 0)
    val combined = history + evidence
    val rejected = combined
        .filter { it.outcome == TakeoffPeakOutcome.REJECTED }
        .sortedByDescending { it.smoothedAnkleRiseRatio }
        .take(minOf(2, maxSize))
    val accepted = combined
        .filter { it.outcome != TakeoffPeakOutcome.REJECTED }
        .takeLast(maxSize - rejected.size)
    return accepted + rejected
}

private val EXPERIMENT_DIAGNOSTICS = setOf(
    BounceDiagnostic.FEET_NOT_SYNCHRONIZED,
    BounceDiagnostic.ANKLE_RISE_TOO_SMALL,
    BounceDiagnostic.HIP_RISE_TOO_SMALL,
    BounceDiagnostic.AIRBORNE,
    BounceDiagnostic.LANDED,
)
