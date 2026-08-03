package com.ropeskill.app

enum class VideoTestRightPrimaryRejectReason {
    NONE,
    BEFORE_GO,
    REFRACTORY,
}

data class VideoTestRightPrimaryDecision(
    val videoTimestampMillis: Long,
    val landingMethod: SpeedLandingDetectionMethod,
    val accepted: Boolean,
    val countAfter: Int,
    val intervalSinceAcceptedMillis: Long?,
    val rejectReason: VideoTestRightPrimaryRejectReason,
)

data class VideoTestRightPrimaryCounterResult(
    val countedRightSteps: Int,
    val acceptedRightEvents: Int,
    val rejectedRightEvents: Int,
    val refractoryRejects: Int,
    val minimumAcceptedIntervalMillis: Long?,
    val medianAcceptedIntervalMillis: Long?,
    val maximumAcceptedIntervalMillis: Long?,
    val decisions: List<VideoTestRightPrimaryDecision>,
)

/**
 * Counts fixed-reference right landings directly after GO for diagnostic comparison only.
 * A bounded refractory window rejects implausibly close duplicate right events without requiring
 * an intervening left landing. This evaluator never drives the production counter or History.
 */
object VideoTestRightPrimaryCounter {
    const val REFRACTORY_MILLIS = 300L

    fun evaluate(
        fixedReferenceEvents: List<VideoTestLandingEvent>,
        goTimestampMillis: Long,
    ): VideoTestRightPrimaryCounterResult {
        var count = 0
        var lastAcceptedTimestampMillis: Long? = null
        val acceptedIntervals = mutableListOf<Long>()
        val decisions = fixedReferenceEvents.asSequence()
            .filter {
                it.detectorSource == VideoTestDetectorSource.FIXED_REFERENCE_SHADOW &&
                    it.foot == SpeedLanding.RIGHT
            }
            .sortedBy { it.videoTimestampMillis }
            .map { event ->
                val previousAccepted = lastAcceptedTimestampMillis
                val interval = previousAccepted?.let { event.videoTimestampMillis - it }
                val rejectReason = when {
                    event.videoTimestampMillis < goTimestampMillis ->
                        VideoTestRightPrimaryRejectReason.BEFORE_GO
                    interval != null && interval < REFRACTORY_MILLIS ->
                        VideoTestRightPrimaryRejectReason.REFRACTORY
                    else -> VideoTestRightPrimaryRejectReason.NONE
                }
                val accepted = rejectReason == VideoTestRightPrimaryRejectReason.NONE
                if (accepted) {
                    if (interval != null) acceptedIntervals += interval
                    lastAcceptedTimestampMillis = event.videoTimestampMillis
                    count += 1
                }
                VideoTestRightPrimaryDecision(
                    videoTimestampMillis = event.videoTimestampMillis,
                    landingMethod = event.landingMethod,
                    accepted = accepted,
                    countAfter = count,
                    intervalSinceAcceptedMillis = interval,
                    rejectReason = rejectReason,
                )
            }
            .toList()

        val sortedIntervals = acceptedIntervals.sorted()
        return VideoTestRightPrimaryCounterResult(
            countedRightSteps = count,
            acceptedRightEvents = decisions.count { it.accepted },
            rejectedRightEvents = decisions.count { !it.accepted },
            refractoryRejects = decisions.count {
                it.rejectReason == VideoTestRightPrimaryRejectReason.REFRACTORY
            },
            minimumAcceptedIntervalMillis = sortedIntervals.firstOrNull(),
            medianAcceptedIntervalMillis = sortedIntervals.medianOrNull(),
            maximumAcceptedIntervalMillis = sortedIntervals.lastOrNull(),
            decisions = decisions,
        )
    }

    private fun List<Long>.medianOrNull(): Long? {
        if (isEmpty()) return null
        val middle = size / 2
        return if (size % 2 == 1) {
            this[middle]
        } else {
            (this[middle - 1] + this[middle]) / 2L
        }
    }
}
