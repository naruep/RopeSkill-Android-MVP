package com.ropeskill.app

data class VideoTestCandidateDecision(
    val videoTimestampMillis: Long,
    val landing: SpeedLanding,
    val landingMethod: SpeedLandingDetectionMethod,
    val countedRightStep: Boolean,
    val countAfter: Int,
    val stateBefore: SpeedAlternationState,
    val stateAfter: SpeedAlternationState,
    val rejectReason: SpeedRejectReason,
)

data class VideoTestCandidateCounterResult(
    val countedRightSteps: Int,
    val acceptedEvents: Int,
    val rejectedEvents: Int,
    val repeatedLeftRejects: Int,
    val repeatedRightRejects: Int,
    val bothFeetRejects: Int,
    val unclearLandingRejects: Int,
    val decisions: List<VideoTestCandidateDecision>,
)

/**
 * Applies the existing Speed counter rule to fixed-reference shadow landings without changing
 * production behavior. Opposite-foot landings inside the production 70 ms window are first
 * combined into BOTH, matching [PoseSpeedLandingClassifier].
 */
object VideoTestCandidateCounter {
    private const val SIMULTANEOUS_WINDOW_MILLIS = 70L

    fun evaluate(
        fixedReferenceEvents: List<VideoTestLandingEvent>,
        goTimestampMillis: Long,
    ): VideoTestCandidateCounterResult {
        val detector = SpeedStepDetector()
        detector.start(goTimestampMillis)
        val decisions = mergeSimultaneousLandings(fixedReferenceEvents)
            .map { landing ->
                val step = detector.processLanding(landing.foot, landing.videoTimestampMillis)
                VideoTestCandidateDecision(
                    videoTimestampMillis = landing.videoTimestampMillis,
                    landing = landing.foot,
                    landingMethod = landing.landingMethod,
                    countedRightStep = step.countedRightStep,
                    countAfter = step.count,
                    stateBefore = step.stateBefore,
                    stateAfter = step.stateAfter,
                    rejectReason = step.rejectReason,
                )
            }
        val diagnostics = detector.diagnostics()
        return VideoTestCandidateCounterResult(
            countedRightSteps = diagnostics.countedRightTimestampsMillis.size,
            acceptedEvents = decisions.count { it.rejectReason == SpeedRejectReason.NONE },
            rejectedEvents = decisions.count { it.rejectReason != SpeedRejectReason.NONE },
            repeatedLeftRejects = diagnostics.repeatedLeftRejects,
            repeatedRightRejects = diagnostics.repeatedRightRejects,
            bothFeetRejects = diagnostics.bothFeetRejects,
            unclearLandingRejects = diagnostics.unclearLandingRejects,
            decisions = decisions,
        )
    }

    private fun mergeSimultaneousLandings(
        events: List<VideoTestLandingEvent>,
    ): List<VideoTestLandingEvent> {
        val output = mutableListOf<VideoTestLandingEvent>()
        var pending: VideoTestLandingEvent? = null
        events.asSequence()
            .filter { it.detectorSource == VideoTestDetectorSource.FIXED_REFERENCE_SHADOW }
            .sortedBy { it.videoTimestampMillis }
            .forEach { event ->
                val previous = pending
                if (
                    previous != null &&
                    previous.foot != event.foot &&
                    event.videoTimestampMillis - previous.videoTimestampMillis <=
                    SIMULTANEOUS_WINDOW_MILLIS
                ) {
                    output += event.copy(
                        foot = SpeedLanding.BOTH,
                        landingMethod = combineMethods(
                            previous.landingMethod,
                            event.landingMethod,
                        ),
                    )
                    pending = null
                } else {
                    if (previous != null) output += previous
                    pending = event
                }
            }
        pending?.let(output::add)
        return output
    }

    private fun combineMethods(
        first: SpeedLandingDetectionMethod,
        second: SpeedLandingDetectionMethod,
    ) = if (first == second) first else SpeedLandingDetectionMethod.MIXED
}
