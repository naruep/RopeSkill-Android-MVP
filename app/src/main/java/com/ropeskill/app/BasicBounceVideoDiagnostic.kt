package com.ropeskill.app

import android.app.Application
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.io.IOException
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
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

/** Debug-only offline evidence; it never feeds a result back into Training. */
data class BasicBounceVideoDiagnosticUiState(
    val videoUri: Uri? = null,
    val videoName: String = "",
    val durationMillis: Long = 0L,
    val goTimestampMillis: Long = 0L,
    val isAnalyzing: Boolean = false,
    val progress: Float = 0f,
    val processedFrames: Int = 0,
    val result: BasicBounceVideoDiagnosticResult? = null,
    val message: String? = null,
)

data class BasicBounceVideoDiagnosticResult(
    val videoName: String,
    val videoDurationMillis: Long,
    val goTimestampMillis: Long,
    val sampledFrames: Int,
    val countedJumps: Int,
    val finalTrackingStatus: BounceTrackingStatus,
    val finalDiagnostic: BounceDiagnostic,
    val landingSnapshot: T743LandingStateSnapshot,
    val shadowProfileName: String,
    val shadowCountedJumps: Int,
    val rejectedTakeoffCounts: Map<BounceDiagnostic, Int>,
    val rejectedTakeoffTimeline: List<BasicBounceVideoRejectedTakeoff>,
    val cycleEvents: List<CycleTraceEvidence>,
) {
    fun summaryLine(): String = String.format(
        Locale.US,
        "Offline diagnostic %d jumps · T/L %d/%d · %s",
        countedJumps,
        landingSnapshot.productionTakeoffCount,
        landingSnapshot.productionLandingCount,
        finalDiagnostic.name,
    )

    fun rejectedTakeoffSummary(): String =
        if (rejectedTakeoffCounts.isEmpty()) {
            "Rejected takeoffs 0"
        } else {
            rejectedTakeoffCounts.entries
                .sortedBy { it.key.name }
                .joinToString(prefix = "Rejected takeoffs ") { (diagnostic, count) ->
                    "${diagnostic.name}=$count"
                }
        }

    fun toCsv(): String = buildString {
        appendLine("metric,value")
        appendLine("diagnostic,BASIC_BOUNCE_VIDEO_DEBUG")
        appendLine("video_name,\"${videoName.replace("\"", "\"\"")}\"")
        appendLine("video_duration_ms,$videoDurationMillis")
        appendLine("go_timestamp_ms,$goTimestampMillis")
        appendLine("sampled_frames,$sampledFrames")
        appendLine("offline_counted_jumps,$countedJumps")
        appendLine("final_tracking_status,${finalTrackingStatus.name}")
        appendLine("final_diagnostic,${finalDiagnostic.name}")
        appendLine("production_takeoffs,${landingSnapshot.productionTakeoffCount}")
        appendLine("production_landings,${landingSnapshot.productionLandingCount}")
        appendLine("suppressed_landings,${landingSnapshot.productionSuppressedLandingCount}")
        appendLine("open_airborne_interval,${landingSnapshot.openAirInterval}")
        appendLine("airborne_motion_pulses,${landingSnapshot.physicalPulsesWhileAirborne}")
        appendLine("evidence_gaps,${landingSnapshot.evidenceGapCount}")
        appendLine("shadow_profile,$shadowProfileName")
        appendLine("shadow_offline_counted_jumps,$shadowCountedJumps")
        appendLine("rejected_takeoff_total,${rejectedTakeoffCounts.values.sum()}")
        rejectedTakeoffCounts.entries.sortedBy { it.key.name }.forEach { (diagnostic, count) ->
            appendLine("rejected_takeoff_${diagnostic.name},$count")
        }
        appendLine()
        appendLine("cycle_sequence,event,elapsed_ms,diagnostic,landing_reason,airborne_ms")
        cycleEvents.forEach { event ->
            appendLine(
                listOf(
                    event.sequence,
                    event.event.name,
                    event.elapsedMillis,
                    event.diagnostic.name,
                    event.landingReason?.name.orEmpty(),
                    event.airborneMillis?.toString().orEmpty(),
                ).joinToString(","),
            )
        }
        appendLine()
        appendLine("rejected_index,elapsed_ms,diagnostic")
        rejectedTakeoffTimeline.forEachIndexed { index, event ->
            appendLine(
                listOf(
                    index + 1,
                    event.elapsedMillis,
                    event.diagnostic.name,
                ).joinToString(","),
            )
        }
    }
}

/** Timestamp-only offline evidence; it deliberately excludes pose coordinates and ratios. */
data class BasicBounceVideoRejectedTakeoff(
    val elapsedMillis: Long,
    val diagnostic: BounceDiagnostic,
)

/**
 * Basic Bounce needs the still calibration before a long continuous run, unlike the 30-second
 * Speed Video Test. The cap keeps offline work bounded while covering a 100-jump reproduction.
 */
data class BasicBounceVideoFramePlan(
    val videoDurationMillis: Long,
    val goTimestampMillis: Long,
    val analysisStartMillis: Long,
    val analysisEndExclusiveMillis: Long,
    val frameIntervalMillis: Long,
) {
    val plannedFrameCount: Int
        get() = ((analysisEndExclusiveMillis - analysisStartMillis + frameIntervalMillis - 1L) /
            frameIntervalMillis).toInt()

    companion object {
        const val PRE_GO_CALIBRATION_MILLIS = 2_000L
        const val ANALYSIS_DURATION_MILLIS = 120_000L
        const val FRAME_INTERVAL_MILLIS = 33L

        fun create(videoDurationMillis: Long, goTimestampMillis: Long): BasicBounceVideoFramePlan {
            require(videoDurationMillis > 0L)
            require(goTimestampMillis in 0 until videoDurationMillis)
            return BasicBounceVideoFramePlan(
                videoDurationMillis = videoDurationMillis,
                goTimestampMillis = goTimestampMillis,
                analysisStartMillis = max(0L, goTimestampMillis - PRE_GO_CALIBRATION_MILLIS),
                analysisEndExclusiveMillis = min(
                    videoDurationMillis,
                    goTimestampMillis + ANALYSIS_DURATION_MILLIS,
                ),
                frameIntervalMillis = FRAME_INTERVAL_MILLIS,
            )
        }
    }
}

class BasicBounceVideoDiagnosticViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(BasicBounceVideoDiagnosticUiState())
    val uiState: StateFlow<BasicBounceVideoDiagnosticUiState> = _uiState.asStateFlow()
    private var analysisJob: Job? = null

    fun selectVideo(uri: Uri) {
        cancelAnalysis()
        val application = getApplication<Application>()
        viewModelScope.launch(Dispatchers.IO) {
            val duration = runCatching {
                withMediaRetriever { retriever ->
                    retriever.setDataSource(application, uri)
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                        ?.toLongOrNull()
                }
            }.getOrNull()
            val name = runCatching {
                application.contentResolver.query(
                    uri,
                    arrayOf(OpenableColumns.DISPLAY_NAME),
                    null,
                    null,
                    null,
                )?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
            }.getOrNull() ?: "selected-video"
            withContext(Dispatchers.Main) {
                _uiState.value = if (duration == null || duration <= 0L) {
                    BasicBounceVideoDiagnosticUiState(message = "The selected video cannot be read.")
                } else {
                    BasicBounceVideoDiagnosticUiState(
                        videoUri = uri,
                        videoName = name,
                        durationMillis = duration,
                        goTimestampMillis = minOf(
                            BasicBounceVideoFramePlan.PRE_GO_CALIBRATION_MILLIS,
                            duration - 1L,
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

    fun analyze() {
        val state = _uiState.value
        val uri = state.videoUri ?: return
        if (state.durationMillis <= 0L || state.isAnalyzing) return
        val plan = runCatching {
            BasicBounceVideoFramePlan.create(state.durationMillis, state.goTimestampMillis)
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
                val result = runAnalysis(uri, state.videoName, plan)
                _uiState.value = _uiState.value.copy(
                    isAnalyzing = false,
                    progress = 1f,
                    result = result,
                    message = "Analysis complete. No History entry was created.",
                )
            } catch (_: CancellationException) {
                _uiState.value = _uiState.value.copy(isAnalyzing = false, message = "Analysis cancelled.")
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
        plan: BasicBounceVideoFramePlan,
    ): BasicBounceVideoDiagnosticResult {
        val detector = BasicBounceDetector(
            thresholds = T738DetectorProfiles.PRODUCTION,
            landingStateEvidenceEnabled = true,
        )
        val shadowDetector = BasicBounceDetector(
            thresholds = T756DetectorProfiles.SHADOW_ONLY,
        )
        val landingTrace = T743PassiveLandingStateCollector(
            enabled = true,
            maxFrameHistory = MAX_RETAINED_DIAGNOSTIC_ROWS,
            maxIntervalHistory = MAX_RETAINED_DIAGNOSTIC_ROWS,
        )
        val rejectedTakeoffTimeline = ArrayDeque<BasicBounceVideoRejectedTakeoff>()
        val rejectedTakeoffCounts = mutableMapOf<BounceDiagnostic, Int>()
        val cycleEvents = ArrayDeque<CycleTraceEvidence>()
        var started = false
        var sampledFrames = 0
        var countedJumps = 0
        var shadowCountedJumps = 0
        var finalStatus = BounceTrackingStatus.WAITING
        var finalDiagnostic = BounceDiagnostic.FULL_BODY_REQUIRED

        withMediaRetriever { retriever ->
            retriever.setDataSource(getApplication(), uri)
            VideoPoseProcessor(getApplication()).use { poseProcessor ->
                forEachSampledFrame(retriever, plan) { bitmap, timestampMillis ->
                    currentCoroutineContext().ensureActive()
                    if (!started && timestampMillis >= plan.goTimestampMillis) {
                        landingTrace.startMeasurement(timestampMillis)
                        started = true
                    }
                    val poseFrame = poseProcessor.process(bitmap, timestampMillis + TIMESTAMP_OFFSET_MILLIS)
                    val result = detector.process(poseFrame, timestampMillis + TIMESTAMP_OFFSET_MILLIS)
                    val shadowResult = shadowDetector.process(
                        poseFrame,
                        timestampMillis + TIMESTAMP_OFFSET_MILLIS,
                    )
                    if (started) {
                        landingTrace.record(poseFrame, result, timestampMillis)
                        if (result.countedJump) countedJumps += 1
                        if (shadowResult.countedJump) shadowCountedJumps += 1
                        result.rejectedTakeoffEvidence?.let { evidence ->
                            rejectedTakeoffCounts[evidence.diagnostic] =
                                (rejectedTakeoffCounts[evidence.diagnostic] ?: 0) + 1
                            result.cycleTraceEvidence?.let { trace ->
                                rejectedTakeoffTimeline.addLast(
                                    BasicBounceVideoRejectedTakeoff(
                                        elapsedMillis = trace.elapsedMillis,
                                        diagnostic = evidence.diagnostic,
                                    ),
                                )
                                while (
                                    rejectedTakeoffTimeline.size >
                                        MAX_RETAINED_CYCLE_EVENTS
                                ) {
                                    rejectedTakeoffTimeline.removeFirst()
                                }
                            }
                        }
                        result.cycleTraceEvidence?.let { event ->
                            cycleEvents.addLast(event)
                            while (cycleEvents.size > MAX_RETAINED_CYCLE_EVENTS) {
                                cycleEvents.removeFirst()
                            }
                        }
                    }
                    finalStatus = result.trackingStatus
                    finalDiagnostic = result.diagnostic
                    sampledFrames += 1
                    if (sampledFrames % PROGRESS_UPDATE_FRAME_COUNT == 0) {
                        _uiState.value = _uiState.value.copy(
                            processedFrames = sampledFrames,
                            progress = sampledFrames.toFloat() / plan.plannedFrameCount.coerceAtLeast(1),
                        )
                    }
                }
            }
        }
        return BasicBounceVideoDiagnosticResult(
            videoName = videoName,
            videoDurationMillis = plan.videoDurationMillis,
            goTimestampMillis = plan.goTimestampMillis,
            sampledFrames = sampledFrames,
            countedJumps = countedJumps,
            finalTrackingStatus = finalStatus,
            finalDiagnostic = finalDiagnostic,
            landingSnapshot = requireNotNull(landingTrace.snapshotForPause()),
            shadowProfileName = T756DetectorProfiles.SHADOW_PROFILE_NAME,
            shadowCountedJumps = shadowCountedJumps,
            rejectedTakeoffCounts = rejectedTakeoffCounts.toMap(),
            rejectedTakeoffTimeline = rejectedTakeoffTimeline.toList(),
            cycleEvents = cycleEvents.toList(),
        )
    }

    private suspend fun forEachSampledFrame(
        retriever: MediaMetadataRetriever,
        plan: BasicBounceVideoFramePlan,
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
        const val ANALYSIS_MAX_DIMENSION = 720
        const val PROGRESS_UPDATE_FRAME_COUNT = 10
        const val MAX_RETAINED_DIAGNOSTIC_ROWS = 12
        const val MAX_RETAINED_CYCLE_EVENTS = 512
    }
}
