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
    val shadowRejectedTakeoffCounts: Map<BounceDiagnostic, Int>,
    val shadowRejectedTakeoffTimeline: List<BasicBounceVideoRejectedTakeoff>,
    val shadowCycleEvents: List<CycleTraceEvidence>,
    val shadowGateMargins: List<BasicBounceVideoShadowGateMargin>,
    val shadowProposalSnapshot: T735TakeoffGateSnapshot,
    val shadowLongAirIntervals: List<T743AirInterval>,
    val shadowLongAirFrames: List<T743LandingFrame>,
    val shadowLongAirPulses: List<T743AirPulse>,
    val landingRearmProfileName: String,
    val landingRearmCountedJumps: Int,
    val landingRearmRescues: List<BasicBounceVideoLandingRearmRescue>,
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
        rejectionSummary("Rejected takeoffs", rejectedTakeoffCounts)

    fun shadowRejectedTakeoffSummary(): String =
        rejectionSummary("Shadow rejected takeoffs", shadowRejectedTakeoffCounts)

    fun shadowProposalSummary(): String = String.format(
        Locale.US,
        "Shadow proposals Q/M/U/UA/NP/P %d/%d/%d/%d/%d/%d",
        shadowProposalSnapshot.qualifiedPulseCount,
        shadowProposalSnapshot.matchedPulseCount,
        shadowProposalSnapshot.unmatchedPulseCount,
        shadowProposalSnapshot.unmatchedWhileAirborneCount,
        shadowProposalSnapshot.noProductionPeakCount,
        shadowProposalSnapshot.pendingEvidenceCount,
    )

    fun shadowLongAirSummary(): String = String.format(
        Locale.US,
        "Shadow long AIR intervals/frames/pulses %d/%d/%d",
        shadowLongAirIntervals.size,
        shadowLongAirFrames.size,
        shadowLongAirPulses.size,
    )

    fun landingRearmSummary(): String = String.format(
        Locale.US,
        "Landing re-arm shadow %d jumps · rescues %d",
        landingRearmCountedJumps,
        landingRearmRescues.size,
    )

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
        appendLine("shadow_rejected_takeoff_total,${shadowRejectedTakeoffCounts.values.sum()}")
        shadowRejectedTakeoffCounts.entries.sortedBy { it.key.name }.forEach { (diagnostic, count) ->
            appendLine("shadow_rejected_takeoff_${diagnostic.name},$count")
        }
        appendLine("shadow_proposal_raw_pulses,${shadowProposalSnapshot.rawPulseCount}")
        appendLine("shadow_proposal_qualified_pulses,${shadowProposalSnapshot.qualifiedPulseCount}")
        appendLine("shadow_proposal_matched_takeoffs,${shadowProposalSnapshot.matchedPulseCount}")
        appendLine("shadow_proposal_unmatched_pulses,${shadowProposalSnapshot.unmatchedPulseCount}")
        appendLine(
            "shadow_proposal_unmatched_while_airborne," +
                shadowProposalSnapshot.unmatchedWhileAirborneCount,
        )
        appendLine(
            "shadow_proposal_gate_attributed," +
                shadowProposalSnapshot.attributedUnmatchedCount,
        )
        appendLine("shadow_proposal_no_peak,${shadowProposalSnapshot.noProductionPeakCount}")
        appendLine("shadow_proposal_pending,${shadowProposalSnapshot.pendingEvidenceCount}")
        appendLine("shadow_proposal_interrupted,${shadowProposalSnapshot.interruptedPulseCount}")
        appendLine("shadow_long_air_interval_total,${shadowLongAirIntervals.size}")
        appendLine("shadow_long_air_frame_total,${shadowLongAirFrames.size}")
        appendLine("shadow_long_air_pulse_total,${shadowLongAirPulses.size}")
        appendLine("landing_rearm_profile,$landingRearmProfileName")
        appendLine("landing_rearm_offline_counted_jumps,$landingRearmCountedJumps")
        appendLine("landing_rearm_rescue_total,${landingRearmRescues.size}")
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
        appendLine()
        appendLine("shadow_cycle_sequence,event,elapsed_ms,diagnostic,landing_reason,airborne_ms")
        shadowCycleEvents.forEach { event ->
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
        appendLine("shadow_rejected_index,elapsed_ms,diagnostic")
        shadowRejectedTakeoffTimeline.forEachIndexed { index, event ->
            appendLine(
                listOf(
                    index + 1,
                    event.elapsedMillis,
                    event.diagnostic.name,
                ).joinToString(","),
            )
        }
        appendLine()
        appendLine(
            "shadow_margin_index,elapsed_ms,diagnostic,standard_ankle_margin," +
                "left_individual_ankle_margin,right_individual_ankle_margin," +
                "rescue_ankle_margin,rescue_hip_margin,hip_to_ankle_margin",
        )
        shadowGateMargins.forEachIndexed { index, margin ->
            appendLine(
                listOf(
                    index + 1,
                    margin.elapsedMillis,
                    margin.diagnostic.name,
                    margin.standardAnkleMargin.csvRatio(),
                    margin.leftIndividualAnkleMargin.csvRatio(),
                    margin.rightIndividualAnkleMargin.csvRatio(),
                    margin.rescueAnkleMargin.csvRatio(),
                    margin.rescueHipMargin.csvRatio(),
                    margin.hipToAnkleMargin.csvRatio(),
                ).joinToString(","),
            )
        }
        appendLine()
        appendLine(
            "shadow_proposal_sequence,peak_elapsed_ms,duration_ms,qualified," +
                "matched_shadow_takeoff,peak_tracking_status,attribution_route," +
                "blocking_gates,ankle_rise_ratio,hip_rise_ratio",
        )
        shadowProposalSnapshot.retainedPulses.forEach { pulse ->
            appendLine(
                listOf(
                    pulse.sequence,
                    pulse.elapsedMillis,
                    pulse.durationMillis,
                    pulse.qualified,
                    pulse.matchedProductionTakeoff,
                    pulse.peakTrackingStatus.name,
                    pulse.gateAttribution?.route?.name.orEmpty(),
                    pulse.gateAttribution?.blockingGates
                        ?.sortedBy { it.name }
                        ?.joinToString("+") { it.name }
                        .orEmpty(),
                    pulse.ankleRiseRatio.csvRatio(),
                    pulse.hipRiseRatio.csvRatio(),
                ).joinToString(","),
            )
        }
        appendLine()
        appendLine(
            "shadow_long_air_interval_sequence,started_elapsed_ms,ended_elapsed_ms,duration_ms," +
                "frame_samples,close_reason,physical_pulses_while_airborne",
        )
        shadowLongAirIntervals.forEach { interval ->
            appendLine(
                listOf(
                    interval.sequence,
                    interval.startedAtElapsedMillis,
                    interval.endedAtElapsedMillis,
                    interval.endedAtElapsedMillis - interval.startedAtElapsedMillis,
                    interval.frameSamples,
                    interval.closeReason.name,
                    interval.physicalPulsesWhileAirborne,
                ).joinToString(","),
            )
        }
        appendLine()
        appendLine(
            "shadow_long_air_interval_sequence,frame_sequence,elapsed_ms,status_before,status_after," +
                "event,airborne_ms,ankle_baseline_value,ankle_baseline_limit," +
                "ankle_baseline_margin,hip_baseline_value,hip_baseline_limit," +
                "hip_baseline_margin,returned_to_baseline,ankle_descent_value," +
                "ankle_descent_limit,ankle_descent_margin,hip_descent_value," +
                "hip_descent_limit,hip_descent_margin,descended_from_peak," +
                "ankle_next_rise,hip_next_rise,completed_vertical_cycle,airborne_too_long," +
                "recovered_after_timeout",
        )
        shadowLongAirFrames.forEach { frame ->
            val evidence = frame.evidence ?: return@forEach
            appendLine(
                listOf(
                    shadowLongAirIntervals.intervalSequenceAt(frame.elapsedMillis),
                    frame.sequence,
                    frame.elapsedMillis,
                    frame.statusBefore.name,
                    frame.statusAfter.name,
                    frame.productionEvent.name,
                    evidence.airborneMillis,
                    evidence.ankleFromBaselineRatio.csvRatio(),
                    evidence.ankleBaselineLimitRatio.csvRatio(),
                    (evidence.ankleBaselineLimitRatio - evidence.ankleFromBaselineRatio).csvRatio(),
                    evidence.hipFromBaselineRatio.csvRatio(),
                    evidence.hipBaselineLimitRatio.csvRatio(),
                    (evidence.hipBaselineLimitRatio - evidence.hipFromBaselineRatio).csvRatio(),
                    evidence.returnedToBaseline,
                    evidence.ankleDescentFromPeakRatio.csvRatio(),
                    evidence.ankleDescentLimitRatio.csvRatio(),
                    (evidence.ankleDescentFromPeakRatio - evidence.ankleDescentLimitRatio).csvRatio(),
                    evidence.hipDescentFromPeakRatio.csvRatio(),
                    evidence.hipDescentLimitRatio.csvRatio(),
                    (evidence.hipDescentFromPeakRatio - evidence.hipDescentLimitRatio).csvRatio(),
                    evidence.descendedFromPeak,
                    evidence.ankleStartedNextRise,
                    evidence.hipStartedNextRise,
                    evidence.completedVerticalCycle,
                    evidence.airborneTooLong,
                    evidence.recoveredLandingAfterTimeout,
                ).joinToString(","),
            )
        }
        appendLine()
        appendLine(
            "shadow_long_air_pulse_sequence,interval_sequence,peak_frame_sequence,elapsed_ms," +
                "ankle_rise_ratio,hip_rise_ratio,ankle_baseline_margin,hip_baseline_margin," +
                "ankle_descent_margin,hip_descent_margin,completed_vertical_cycle",
        )
        shadowLongAirPulses.forEach { pulse ->
            val evidence = pulse.landingEvidenceAtPeak
            appendLine(
                listOf(
                    pulse.sequence,
                    pulse.intervalSequence?.toString().orEmpty(),
                    pulse.peakFrameSequence,
                    pulse.elapsedMillis,
                    pulse.ankleRiseRatio.csvRatio(),
                    pulse.hipRiseRatio.csvRatio(),
                    evidence?.let {
                        (it.ankleBaselineLimitRatio - it.ankleFromBaselineRatio).csvRatio()
                    }.orEmpty(),
                    evidence?.let {
                        (it.hipBaselineLimitRatio - it.hipFromBaselineRatio).csvRatio()
                    }.orEmpty(),
                    evidence?.let {
                        (it.ankleDescentFromPeakRatio - it.ankleDescentLimitRatio).csvRatio()
                    }.orEmpty(),
                    evidence?.let {
                        (it.hipDescentFromPeakRatio - it.hipDescentLimitRatio).csvRatio()
                    }.orEmpty(),
                    evidence?.completedVerticalCycle?.toString().orEmpty(),
                ).joinToString(","),
            )
        }
        appendLine()
        appendLine(
            "landing_rearm_rescue_index,elapsed_ms,airborne_ms,ankle_baseline_margin," +
                "hip_baseline_margin,ankle_descent_margin,hip_descent_margin," +
                "ankle_next_rise,hip_next_rise",
        )
        landingRearmRescues.forEachIndexed { index, rescue ->
            appendLine(
                listOf(
                    index + 1,
                    rescue.elapsedMillis,
                    rescue.airborneMillis,
                    rescue.ankleBaselineMargin.csvRatio(),
                    rescue.hipBaselineMargin.csvRatio(),
                    rescue.ankleDescentMargin.csvRatio(),
                    rescue.hipDescentMargin.csvRatio(),
                    rescue.ankleStartedNextRise,
                    rescue.hipStartedNextRise,
                ).joinToString(","),
            )
        }
    }

    private fun rejectionSummary(
        prefix: String,
        counts: Map<BounceDiagnostic, Int>,
    ): String =
        if (counts.isEmpty()) {
            "$prefix 0"
        } else {
            counts.entries
                .sortedBy { it.key.name }
                .joinToString(prefix = "$prefix ") { (diagnostic, count) ->
                    "${diagnostic.name}=$count"
                }
        }
}

/** Timestamp-only offline evidence; it deliberately excludes pose coordinates and ratios. */
data class BasicBounceVideoRejectedTakeoff(
    val elapsedMillis: Long,
    val diagnostic: BounceDiagnostic,
)

/** Bounded, coordinate-free signed margins for a shadow rejected Takeoff. */
data class BasicBounceVideoShadowGateMargin(
    val elapsedMillis: Long,
    val diagnostic: BounceDiagnostic,
    val standardAnkleMargin: Float,
    val leftIndividualAnkleMargin: Float,
    val rightIndividualAnkleMargin: Float,
    val rescueAnkleMargin: Float,
    val rescueHipMargin: Float,
    val hipToAnkleMargin: Float,
) {
    companion object {
        internal fun from(
            elapsedMillis: Long,
            evidence: TakeoffPeakEvidence,
            thresholds: BasicBounceDetectorThresholds,
        ): BasicBounceVideoShadowGateMargin =
            BasicBounceVideoShadowGateMargin(
                elapsedMillis = elapsedMillis,
                diagnostic = evidence.diagnostic,
                standardAnkleMargin =
                    evidence.smoothedAnkleRiseRatio -
                        BasicBounceTakeoffGateConstants.STANDARD_ANKLE_RISE_RATIO,
                leftIndividualAnkleMargin =
                    evidence.rawLeftAnkleRiseRatio - evidence.individualAnkleRiseThreshold,
                rightIndividualAnkleMargin =
                    evidence.rawRightAnkleRiseRatio - evidence.individualAnkleRiseThreshold,
                rescueAnkleMargin =
                    evidence.smoothedAnkleRiseRatio - thresholds.strongHipRescueAnkleRiseRatio,
                rescueHipMargin =
                    evidence.smoothedHipRiseRatio -
                        BasicBounceTakeoffGateConstants.STRONG_HIP_RESCUE_HIP_RISE_RATIO,
                hipToAnkleMargin =
                    evidence.smoothedHipRiseRatio -
                        evidence.rawAnkleRiseRatio *
                        BasicBounceTakeoffGateConstants.MIN_HIP_TO_ANKLE_RISE_RATIO,
            )
    }
}

/** Coordinate-free evidence emitted only when the T757 shadow closes a guarded re-arm Landing. */
data class BasicBounceVideoLandingRearmRescue(
    val elapsedMillis: Long,
    val airborneMillis: Long,
    val ankleBaselineMargin: Float,
    val hipBaselineMargin: Float,
    val ankleDescentMargin: Float,
    val hipDescentMargin: Float,
    val ankleStartedNextRise: Boolean,
    val hipStartedNextRise: Boolean,
) {
    companion object {
        internal fun from(
            elapsedMillis: Long,
            evidence: LandingStateEvidence,
            thresholds: BasicBounceDetectorThresholds,
        ): BasicBounceVideoLandingRearmRescue = BasicBounceVideoLandingRearmRescue(
            elapsedMillis = elapsedMillis,
            airborneMillis = evidence.airborneMillis,
            ankleBaselineMargin =
                thresholds.landingRearmRescueAnkleBaselineRatio -
                    evidence.ankleFromBaselineRatio,
            hipBaselineMargin =
                evidence.hipBaselineLimitRatio - evidence.hipFromBaselineRatio,
            ankleDescentMargin =
                evidence.ankleDescentFromPeakRatio - evidence.ankleDescentLimitRatio,
            hipDescentMargin =
                evidence.hipDescentFromPeakRatio - evidence.hipDescentLimitRatio,
            ankleStartedNextRise = evidence.ankleStartedNextRise,
            hipStartedNextRise = evidence.hipStartedNextRise,
        )
    }
}

private fun Float.csvRatio(): String = String.format(Locale.US, "%.5f", this)

private fun List<T743AirInterval>.intervalSequenceAt(elapsedMillis: Long): Int? =
    firstOrNull {
        elapsedMillis in it.startedAtElapsedMillis..it.endedAtElapsedMillis
    }?.sequence

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
            landingStateEvidenceEnabled = true,
        )
        val landingRearmDetector = BasicBounceDetector(
            thresholds = T757DetectorProfiles.SHADOW_ONLY,
            landingStateEvidenceEnabled = true,
        )
        val landingTrace = T743PassiveLandingStateCollector(
            enabled = true,
            maxFrameHistory = MAX_RETAINED_DIAGNOSTIC_ROWS,
            maxIntervalHistory = MAX_RETAINED_DIAGNOSTIC_ROWS,
        )
        val shadowProposalTrace = T735PassiveTakeoffGateCollector(
            enabled = true,
            maxRetainedPulses = MAX_RETAINED_PROPOSAL_PULSES,
        )
        val shadowLandingTrace = T743PassiveLandingStateCollector(
            enabled = true,
            maxFrameHistory = MAX_RETAINED_LANDING_FRAMES,
            maxIntervalHistory = MAX_RETAINED_CYCLE_EVENTS,
        )
        val rejectedTakeoffTimeline = ArrayDeque<BasicBounceVideoRejectedTakeoff>()
        val rejectedTakeoffCounts = mutableMapOf<BounceDiagnostic, Int>()
        val cycleEvents = ArrayDeque<CycleTraceEvidence>()
        val shadowRejectedTakeoffTimeline = ArrayDeque<BasicBounceVideoRejectedTakeoff>()
        val shadowRejectedTakeoffCounts = mutableMapOf<BounceDiagnostic, Int>()
        val shadowCycleEvents = ArrayDeque<CycleTraceEvidence>()
        val shadowGateMargins = ArrayDeque<BasicBounceVideoShadowGateMargin>()
        val landingRearmRescues = ArrayDeque<BasicBounceVideoLandingRearmRescue>()
        var started = false
        var sampledFrames = 0
        var countedJumps = 0
        var shadowCountedJumps = 0
        var landingRearmCountedJumps = 0
        var finalStatus = BounceTrackingStatus.WAITING
        var finalDiagnostic = BounceDiagnostic.FULL_BODY_REQUIRED

        withMediaRetriever { retriever ->
            retriever.setDataSource(getApplication(), uri)
            VideoPoseProcessor(getApplication()).use { poseProcessor ->
                forEachSampledFrame(retriever, plan) { bitmap, timestampMillis ->
                    currentCoroutineContext().ensureActive()
                    if (!started && timestampMillis >= plan.goTimestampMillis) {
                        landingTrace.startMeasurement(timestampMillis)
                        shadowProposalTrace.startMeasurement(timestampMillis)
                        shadowLandingTrace.startMeasurement(timestampMillis)
                        started = true
                    }
                    val poseFrame = poseProcessor.process(bitmap, timestampMillis + TIMESTAMP_OFFSET_MILLIS)
                    val result = detector.process(poseFrame, timestampMillis + TIMESTAMP_OFFSET_MILLIS)
                    val shadowResult = shadowDetector.process(
                        poseFrame,
                        timestampMillis + TIMESTAMP_OFFSET_MILLIS,
                    )
                    val landingRearmResult = landingRearmDetector.process(
                        poseFrame,
                        timestampMillis + TIMESTAMP_OFFSET_MILLIS,
                    )
                    if (started) {
                        landingTrace.record(poseFrame, result, timestampMillis)
                        shadowProposalTrace.record(poseFrame, shadowResult, timestampMillis)
                        shadowLandingTrace.record(poseFrame, shadowResult, timestampMillis)
                        if (result.countedJump) countedJumps += 1
                        if (shadowResult.countedJump) shadowCountedJumps += 1
                        if (landingRearmResult.countedJump) landingRearmCountedJumps += 1
                        landingRearmResult.landingStateEvidence
                            ?.takeIf {
                                landingRearmResult.event == BounceEvent.LANDING &&
                                    it.landingRearmRescueApplied
                            }
                            ?.let { evidence ->
                                landingRearmRescues.addLast(
                                    BasicBounceVideoLandingRearmRescue.from(
                                        elapsedMillis =
                                            (timestampMillis - plan.goTimestampMillis)
                                                .coerceAtLeast(0L),
                                        evidence = evidence,
                                        thresholds = T757DetectorProfiles.SHADOW_ONLY,
                                    ),
                                )
                                while (
                                    landingRearmRescues.size > MAX_RETAINED_CYCLE_EVENTS
                                ) {
                                    landingRearmRescues.removeFirst()
                                }
                            }
                        shadowResult.rejectedTakeoffEvidence?.let { rejectedEvidence ->
                            shadowRejectedTakeoffCounts[rejectedEvidence.diagnostic] =
                                (shadowRejectedTakeoffCounts[rejectedEvidence.diagnostic] ?: 0) + 1
                            shadowResult.cycleTraceEvidence?.let { trace ->
                                shadowResult.takeoffPeakEvidence?.let { peakEvidence ->
                                    shadowGateMargins.addLast(
                                        BasicBounceVideoShadowGateMargin.from(
                                            elapsedMillis = trace.elapsedMillis,
                                            evidence = peakEvidence,
                                            thresholds = T756DetectorProfiles.SHADOW_ONLY,
                                        ),
                                    )
                                    while (shadowGateMargins.size > MAX_RETAINED_CYCLE_EVENTS) {
                                        shadowGateMargins.removeFirst()
                                    }
                                }
                                shadowRejectedTakeoffTimeline.addLast(
                                    BasicBounceVideoRejectedTakeoff(
                                        elapsedMillis = trace.elapsedMillis,
                                        diagnostic = rejectedEvidence.diagnostic,
                                    ),
                                )
                                while (
                                    shadowRejectedTakeoffTimeline.size >
                                        MAX_RETAINED_CYCLE_EVENTS
                                ) {
                                    shadowRejectedTakeoffTimeline.removeFirst()
                                }
                            }
                        }
                        shadowResult.cycleTraceEvidence?.let { event ->
                            shadowCycleEvents.addLast(event)
                            while (shadowCycleEvents.size > MAX_RETAINED_CYCLE_EVENTS) {
                                shadowCycleEvents.removeFirst()
                            }
                        }
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
        val shadowLandingSnapshot = requireNotNull(shadowLandingTrace.snapshotForPause())
        val shadowLongAirIntervals = shadowLandingSnapshot.recentIntervals.filter { interval ->
            interval.endedAtElapsedMillis - interval.startedAtElapsedMillis >=
                LONG_AIR_INTERVAL_MILLIS ||
                interval.physicalPulsesWhileAirborne > 0
        }
        val shadowLongAirIntervalSequences = shadowLongAirIntervals
            .mapTo(mutableSetOf()) { it.sequence }
        val shadowLongAirFrames = shadowLandingSnapshot.recentFrames.filter { frame ->
            frame.evidence != null && shadowLongAirIntervals.any { interval ->
                frame.elapsedMillis in
                    interval.startedAtElapsedMillis..interval.endedAtElapsedMillis
            }
        }
        val shadowLongAirPulses = shadowLandingSnapshot.recentAirPulses.filter { pulse ->
            pulse.intervalSequence?.let { it in shadowLongAirIntervalSequences } == true
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
            shadowRejectedTakeoffCounts = shadowRejectedTakeoffCounts.toMap(),
            shadowRejectedTakeoffTimeline = shadowRejectedTakeoffTimeline.toList(),
            shadowCycleEvents = shadowCycleEvents.toList(),
            shadowGateMargins = shadowGateMargins.toList(),
            shadowProposalSnapshot = requireNotNull(
                shadowProposalTrace.snapshotForPause(plan.analysisEndExclusiveMillis),
            ),
            shadowLongAirIntervals = shadowLongAirIntervals,
            shadowLongAirFrames = shadowLongAirFrames,
            shadowLongAirPulses = shadowLongAirPulses,
            landingRearmProfileName = T757DetectorProfiles.SHADOW_PROFILE_NAME,
            landingRearmCountedJumps = landingRearmCountedJumps,
            landingRearmRescues = landingRearmRescues.toList(),
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
        const val MAX_RETAINED_PROPOSAL_PULSES = 512
        const val MAX_RETAINED_LANDING_FRAMES = 4_096
        const val LONG_AIR_INTERVAL_MILLIS = 400L
    }
}
