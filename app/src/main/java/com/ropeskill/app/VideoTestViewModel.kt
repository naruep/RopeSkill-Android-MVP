package com.ropeskill.app

import android.app.Application
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class VideoTestUiState(
    val videoUri: Uri? = null,
    val videoName: String = "",
    val durationMillis: Long = 0L,
    val goTimestampMillis: Long = 0L,
    val groundTruthRightSteps: String = "",
    val isAnalyzing: Boolean = false,
    val progress: Float = 0f,
    val processedFrames: Int = 0,
    val result: VideoTestAnalysisResult? = null,
    val message: String? = null,
)

class VideoTestViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(VideoTestUiState())
    val uiState: StateFlow<VideoTestUiState> = _uiState.asStateFlow()
    private var analysisJob: Job? = null

    fun selectVideo(uri: Uri) {
        cancelAnalysis()
        val resolver = getApplication<Application>().contentResolver
        viewModelScope.launch(Dispatchers.IO) {
            val metadata = runCatching {
                withMediaRetriever { retriever ->
                    retriever.setDataSource(getApplication(), uri)
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                        ?.toLongOrNull()
                }
            }.getOrNull()
            val name = runCatching {
                resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                    ?.use { cursor ->
                        if (cursor.moveToFirst()) cursor.getString(0) else null
                    }
            }.getOrNull() ?: "selected-video"
            withContext(Dispatchers.Main) {
                if (metadata == null || metadata <= 0L) {
                    _uiState.value = VideoTestUiState(message = "The selected video cannot be read.")
                } else {
                    _uiState.value = VideoTestUiState(
                        videoUri = uri,
                        videoName = name,
                        durationMillis = metadata,
                        goTimestampMillis = minOf(
                            VideoTestFramePlan.PRE_GO_CALIBRATION_MILLIS,
                            metadata - 1L,
                        ),
                    )
                }
            }
        }
    }

    fun setGoTimestamp(timestampMillis: Long) {
        val state = _uiState.value
        if (state.isAnalyzing || state.durationMillis <= 0L) return
        _uiState.value = state.copy(
            goTimestampMillis = timestampMillis.coerceIn(0L, state.durationMillis - 1L),
            result = null,
            message = null,
        )
    }

    fun setGroundTruthRightSteps(value: String) {
        val state = _uiState.value
        if (state.isAnalyzing) return
        val sanitized = value.filter(Char::isDigit).take(MAX_GROUND_TRUTH_DIGITS)
        _uiState.value = state.copy(
            groundTruthRightSteps = sanitized,
            result = null,
            message = null,
        )
    }

    fun analyze() {
        val state = _uiState.value
        val uri = state.videoUri ?: return
        if (state.durationMillis <= 0L || state.isAnalyzing) return
        val groundTruthRightSteps = state.groundTruthRightSteps.toIntOrNull()
        if (groundTruthRightSteps == null) {
            _uiState.value = state.copy(
                message = "Enter the manually verified right-step ground truth. Use 0 for a negative control.",
            )
            return
        }
        val plan = runCatching {
            VideoTestFramePlan.create(state.durationMillis, state.goTimestampMillis)
        }.getOrElse {
            _uiState.value = state.copy(message = "Choose a valid GO position.")
            return
        }

        analysisJob = viewModelScope.launch(Dispatchers.Default) {
            _uiState.value = state.copy(
                isAnalyzing = true,
                progress = 0f,
                processedFrames = 0,
                result = null,
                message = "Analyzing on this device. The source video is not copied.",
            )
            try {
                val result = runAnalysis(uri, state.videoName, plan, groundTruthRightSteps)
                _uiState.value = _uiState.value.copy(
                    isAnalyzing = false,
                    progress = 1f,
                    result = result,
                    message = "Analysis complete. No History entry was created.",
                )
            } catch (_: CancellationException) {
                _uiState.value = _uiState.value.copy(
                    isAnalyzing = false,
                    message = "Analysis cancelled.",
                )
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(
                    isAnalyzing = false,
                    message = "Video analysis could not be completed.",
                )
            }
        }
    }

    fun cancelAnalysis() {
        analysisJob?.cancel()
        analysisJob = null
    }

    fun exportReport(uri: Uri) {
        val report = _uiState.value.result ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val saved = runCatching {
                getApplication<Application>().contentResolver.openOutputStream(uri)?.use { output ->
                    output.write(report.toCsv().toByteArray(Charsets.UTF_8))
                } ?: throw IOException("Cannot open report destination")
            }.isSuccess
            _uiState.value = _uiState.value.copy(
                message = if (saved) "CSV report saved." else "CSV report could not be saved.",
            )
        }
    }

    private suspend fun runAnalysis(
        uri: Uri,
        videoName: String,
        plan: VideoTestFramePlan,
        groundTruthRightSteps: Int,
    ): VideoTestAnalysisResult {
        val landingEvents = mutableListOf<VideoTestLandingEvent>()
        val classifier = PoseSpeedLandingClassifier(
            evidenceEnabled = true,
            onFixedReferenceLanding = { event ->
                landingEvents += event.toVideoTestEvent(
                    detectorSource = VideoTestDetectorSource.FIXED_REFERENCE_SHADOW,
                )
            },
        )
        val stepDetector = SpeedStepDetector()
        var started = false
        var sampledFrames = 0
        classifier.startPreGoCalibration()

        withMediaRetriever { retriever ->
            retriever.setDataSource(getApplication(), uri)
            VideoPoseProcessor(getApplication()).use { poseProcessor ->
                forEachSampledFrame(retriever, plan) { bitmap, videoTimestampMillis ->
                    currentCoroutineContext().ensureActive()
                    val detectorTimestampMillis = videoTimestampMillis + TIMESTAMP_OFFSET_MILLIS
                    if (!started && videoTimestampMillis >= plan.goTimestampMillis) {
                        classifier.commitPreGoCalibration()
                        landingEvents.clear()
                        stepDetector.start(plan.goTimestampMillis + TIMESTAMP_OFFSET_MILLIS)
                        started = true
                    }
                    val classifierResult = poseProcessor.process(bitmap, detectorTimestampMillis)
                        .let(classifier::process)
                    if (started) {
                        if (classifierResult.trackingValid) {
                            stepDetector.onTrackingRestored(detectorTimestampMillis)
                        } else {
                            stepDetector.onTrackingLost(detectorTimestampMillis)
                        }
                        classifierResult.events.forEach { event ->
                            val stepResult = stepDetector.processLanding(
                                event.landing,
                                event.timestampMillis,
                            )
                            landingEvents += event.toVideoTestEvent(stepResult)
                        }
                    }
                    sampledFrames += 1
                    if (sampledFrames % PROGRESS_UPDATE_FRAME_COUNT == 0) {
                        _uiState.value = _uiState.value.copy(
                            progress = sampledFrames.toFloat() / plan.plannedFrameCount,
                            processedFrames = sampledFrames,
                        )
                    }
                }
            }
        }
        if (!started) error("No frame reached GO")
        classifier.flushPending().forEach { event ->
            val stepResult = stepDetector.processLanding(event.landing, event.timestampMillis)
            landingEvents += event.toVideoTestEvent(stepResult)
        }
        val classifierDiagnostics = classifier.diagnostics()
        val stepDiagnostics = stepDetector.diagnostics()
        val leftFixed = classifierDiagnostics.leftFixedReferenceShadowEvidence
        val rightFixed = classifierDiagnostics.rightFixedReferenceShadowEvidence
        val fixedReferenceCandidate = VideoTestCandidateCounter.evaluate(
            fixedReferenceEvents = landingEvents,
            goTimestampMillis = plan.goTimestampMillis,
        )
        val rightPrimaryCandidate = VideoTestRightPrimaryCounter.evaluate(
            fixedReferenceEvents = landingEvents,
            goTimestampMillis = plan.goTimestampMillis,
        )
        return VideoTestAnalysisResult(
            videoName = videoName,
            videoDurationMillis = plan.videoDurationMillis,
            goTimestampMillis = plan.goTimestampMillis,
            groundTruthRightSteps = groundTruthRightSteps,
            sampledFrames = sampledFrames,
            framesWithValidPose = classifierDiagnostics.validFrames,
            lowVisibilityFrames = classifierDiagnostics.lowVisibilityFrames,
            productionLeftLandings = stepDiagnostics.leftLandingTimestampsMillis.size,
            productionRightLandings = stepDiagnostics.rightLandingTimestampsMillis.size,
            productionLeftConservativeRearms = classifierDiagnostics.leftConservativeRearms,
            productionRightConservativeRearms = classifierDiagnostics.rightConservativeRearms,
            countedRightSteps = stepDiagnostics.countedRightTimestampsMillis.size,
            fixedReferenceLeftLandings = leftFixed?.totalLandings ?: 0,
            fixedReferenceRightLandings = rightFixed?.totalLandings ?: 0,
            fixedReferenceLeftStrictLandings = leftFixed?.strictLandings ?: 0,
            fixedReferenceRightStrictLandings = rightFixed?.strictLandings ?: 0,
            fixedReferenceLeftConservativeRearms = leftFixed?.conservativeRearms ?: 0,
            fixedReferenceRightConservativeRearms = rightFixed?.conservativeRearms ?: 0,
            repeatedLeftRejects = stepDiagnostics.repeatedLeftRejects,
            repeatedRightRejects = stepDiagnostics.repeatedRightRejects,
            bothFeetRejects = stepDiagnostics.bothFeetRejects,
            unclearLandingRejects = stepDiagnostics.unclearLandingRejects,
            landingEvents = landingEvents.toList(),
            fixedReferenceCandidate = fixedReferenceCandidate,
            rightPrimaryCandidate = rightPrimaryCandidate,
        )
    }

    private fun SpeedFixedReferenceLandingEvent.toVideoTestEvent(
        detectorSource: VideoTestDetectorSource,
    ) = VideoTestLandingEvent(
        detectorSource = detectorSource,
        videoTimestampMillis = timestampMillis - TIMESTAMP_OFFSET_MILLIS,
        foot = landing,
        landingMethod = detectionMethod,
    )

    private fun SpeedLandingEvent.toVideoTestEvent(
        stepResult: SpeedStepResult,
    ) = VideoTestLandingEvent(
        detectorSource = VideoTestDetectorSource.PRODUCTION,
        videoTimestampMillis = timestampMillis - TIMESTAMP_OFFSET_MILLIS,
        foot = landing,
        landingMethod = detectionMethod,
        countedRightStep = stepResult.countedRightStep,
        counterRejectReason = stepResult.rejectReason,
    )

    private suspend fun forEachSampledFrame(
        retriever: MediaMetadataRetriever,
        plan: VideoTestFramePlan,
        action: suspend (Bitmap, Long) -> Unit,
    ) {
        var timestampMillis = plan.analysisStartMillis
        while (timestampMillis < plan.analysisEndExclusiveMillis) {
            currentCoroutineContext().ensureActive()
            val sourceBitmap = retriever.getFrameAtTime(
                timestampMillis * 1_000L,
                MediaMetadataRetriever.OPTION_CLOSEST,
            )
            if (sourceBitmap != null) {
                val bitmap = sourceBitmap.scaledForAnalysis()
                try {
                    action(bitmap, timestampMillis)
                } finally {
                    bitmap.recycle()
                    if (bitmap !== sourceBitmap) sourceBitmap.recycle()
                }
            }
            timestampMillis += plan.frameIntervalMillis
        }
    }

    override fun onCleared() {
        cancelAnalysis()
        super.onCleared()
    }

    private fun Bitmap.scaledForAnalysis(): Bitmap {
        val longestSide = maxOf(width, height)
        if (longestSide <= ANALYSIS_MAX_DIMENSION) return this
        val scale = ANALYSIS_MAX_DIMENSION.toFloat() / longestSide.toFloat()
        return Bitmap.createScaledBitmap(
            this,
            (width * scale).toInt().coerceAtLeast(1),
            (height * scale).toInt().coerceAtLeast(1),
            true,
        )
    }

    private suspend inline fun <T> withMediaRetriever(
        block: suspend (MediaMetadataRetriever) -> T,
    ): T {
        val retriever = MediaMetadataRetriever()
        return try {
            block(retriever)
        } finally {
            retriever.release()
        }
    }

    private companion object {
        const val TIMESTAMP_OFFSET_MILLIS = 1L
        const val PROGRESS_UPDATE_FRAME_COUNT = 10
        const val ANALYSIS_MAX_DIMENSION = 720
        const val MAX_GROUND_TRUTH_DIGITS = 4
    }
}
