package com.ropeskill.app

import java.util.Locale
import kotlin.math.max
import kotlin.math.min

data class VideoTestFramePlan(
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
        const val ANALYSIS_DURATION_MILLIS = 30_000L
        const val FRAME_INTERVAL_MILLIS = 33L

        fun create(videoDurationMillis: Long, goTimestampMillis: Long): VideoTestFramePlan {
            require(videoDurationMillis > 0L)
            require(goTimestampMillis in 0 until videoDurationMillis)
            return VideoTestFramePlan(
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

data class VideoTestAnalysisResult(
    val videoName: String,
    val videoDurationMillis: Long,
    val goTimestampMillis: Long,
    val sampledFrames: Int,
    val framesWithValidPose: Long,
    val lowVisibilityFrames: Long,
    val productionLeftLandings: Int,
    val productionRightLandings: Int,
    val productionLeftConservativeRearms: Int,
    val productionRightConservativeRearms: Int,
    val countedRightSteps: Int,
    val fixedReferenceLeftLandings: Int,
    val fixedReferenceRightLandings: Int,
    val fixedReferenceLeftStrictLandings: Int,
    val fixedReferenceRightStrictLandings: Int,
    val fixedReferenceLeftConservativeRearms: Int,
    val fixedReferenceRightConservativeRearms: Int,
    val repeatedLeftRejects: Int,
    val repeatedRightRejects: Int,
    val bothFeetRejects: Int,
    val unclearLandingRejects: Int,
    val landingEvents: List<VideoTestLandingEvent>,
) {
    val productionRawLandings: Int
        get() = productionLeftLandings + productionRightLandings

    val fixedReferenceRawLandings: Int
        get() = fixedReferenceLeftLandings + fixedReferenceRightLandings

    val fixedReferenceDifference: Int
        get() = fixedReferenceRawLandings - productionRawLandings

    fun toCsv(): String = buildString {
        appendLine("metric,value")
        appendLine("video_name,${csvEscape(videoName)}")
        appendLine("video_duration_ms,$videoDurationMillis")
        appendLine("go_timestamp_ms,$goTimestampMillis")
        appendLine("sampled_frames,$sampledFrames")
        appendLine("valid_pose_frames,$framesWithValidPose")
        appendLine("low_visibility_frames,$lowVisibilityFrames")
        appendLine("production_left_landings,$productionLeftLandings")
        appendLine("production_right_landings,$productionRightLandings")
        appendLine("production_raw_landings,$productionRawLandings")
        appendLine("production_left_conservative_rearms,$productionLeftConservativeRearms")
        appendLine("production_right_conservative_rearms,$productionRightConservativeRearms")
        appendLine("counted_right_steps,$countedRightSteps")
        appendLine("fixed_reference_left_landings,$fixedReferenceLeftLandings")
        appendLine("fixed_reference_right_landings,$fixedReferenceRightLandings")
        appendLine("fixed_reference_raw_landings,$fixedReferenceRawLandings")
        appendLine("fixed_reference_left_strict_landings,$fixedReferenceLeftStrictLandings")
        appendLine("fixed_reference_right_strict_landings,$fixedReferenceRightStrictLandings")
        appendLine("fixed_reference_left_conservative_rearms,$fixedReferenceLeftConservativeRearms")
        appendLine("fixed_reference_right_conservative_rearms,$fixedReferenceRightConservativeRearms")
        appendLine("fixed_reference_difference,$fixedReferenceDifference")
        appendLine("repeated_left_rejects,$repeatedLeftRejects")
        appendLine("repeated_right_rejects,$repeatedRightRejects")
        appendLine("both_feet_rejects,$bothFeetRejects")
        appendLine("unclear_landing_rejects,$unclearLandingRejects")
        appendLine()
        appendLine(
            "event_index,detector_source,video_timestamp_ms,relative_to_go_ms,foot," +
                "landing_method,counted_right_step,counter_reject_reason",
        )
        landingEvents.sortedWith(
            compareBy<VideoTestLandingEvent> { it.videoTimestampMillis }
                .thenBy { it.detectorSource.name }
                .thenBy { it.foot.name },
        ).forEachIndexed { index, event ->
            appendLine(
                listOf(
                    index + 1,
                    event.detectorSource.name,
                    event.videoTimestampMillis,
                    event.videoTimestampMillis - goTimestampMillis,
                    event.foot.name,
                    event.landingMethod.name,
                    event.countedRightStep,
                    event.counterRejectReason.name,
                ).joinToString(","),
            )
        }
    }

    fun summaryLine(): String = String.format(
        Locale.US,
        "Production %d vs fixed-reference %d (%+d)",
        productionRawLandings,
        fixedReferenceRawLandings,
        fixedReferenceDifference,
    )

    private fun csvEscape(value: String): String =
        "\"${value.replace("\"", "\"\"")}\""
}

enum class VideoTestDetectorSource {
    PRODUCTION,
    FIXED_REFERENCE_SHADOW,
}

data class VideoTestLandingEvent(
    val detectorSource: VideoTestDetectorSource,
    val videoTimestampMillis: Long,
    val foot: SpeedLanding,
    val landingMethod: SpeedLandingDetectionMethod,
    val countedRightStep: Boolean = false,
    val counterRejectReason: SpeedRejectReason = SpeedRejectReason.NONE,
)
