package com.ropeskill.app

enum class VideoTestAlternationGuardEvidence {
    NONE,
    CONFIRMED_SEQUENCE,
    RECENT_LEFT,
    CADENCE_BRIDGE,
}

enum class VideoTestAlternationGuardRejectReason {
    NONE,
    BEFORE_GO,
    REFRACTORY,
    UNCONFIRMED_ALTERNATION,
    MISSING_ALTERNATION,
    BRIDGE_LIMIT,
    TRANSITION_TOO_FAST,
}

data class VideoTestAlternationGuardDecision(
    val videoTimestampMillis: Long,
    val landingMethod: SpeedLandingDetectionMethod,
    val accepted: Boolean,
    val countAfter: Int,
    val intervalSinceAcceptedMillis: Long?,
    val evidence: VideoTestAlternationGuardEvidence,
    val rejectReason: VideoTestAlternationGuardRejectReason,
)

data class VideoTestAlternationGuardCounterResult(
    val countedRightSteps: Int,
    val acceptedRightEvents: Int,
    val rejectedRightEvents: Int,
    val confirmedSequenceAccepts: Int,
    val recentLeftAccepts: Int,
    val cadenceBridgeAccepts: Int,
    val refractoryRejects: Int,
    val unconfirmedAlternationRejects: Int,
    val missingAlternationRejects: Int,
    val bridgeLimitRejects: Int,
    val transitionTooFastRejects: Int,
    val decisions: List<VideoTestAlternationGuardDecision>,
)

/**
 * Evaluates a guarded fixed-reference right counter for Video Test evidence only.
 *
 * The session must first contain a bounded alternating sequence (R-L-R or L-R-L). After that,
 * right events normally require a new recent left event. Up to two cadence-consistent right events
 * may bridge missing left evidence, matching the two consecutive left misses observed in the V9
 * reference CSV. This evaluator never drives the production counter or History.
 */
object VideoTestAlternationGuardCounter {
    const val REFRACTORY_MILLIS = VideoTestRightPrimaryCounter.REFRACTORY_MILLIS
    const val MAX_ALTERNATION_GAP_MILLIS = 900L
    const val MAX_CONSECUTIVE_CADENCE_BRIDGES = 2

    fun evaluate(
        fixedReferenceEvents: List<VideoTestLandingEvent>,
        goTimestampMillis: Long,
    ): VideoTestAlternationGuardCounterResult = evaluateWithMinimumTransitionGap(
        fixedReferenceEvents = fixedReferenceEvents,
        goTimestampMillis = goTimestampMillis,
        minimumTransitionGapMillis = 0L,
    )

    internal fun evaluateWithMinimumTransitionGap(
        fixedReferenceEvents: List<VideoTestLandingEvent>,
        goTimestampMillis: Long,
        minimumTransitionGapMillis: Long,
    ): VideoTestAlternationGuardCounterResult {
        require(minimumTransitionGapMillis >= 0L)
        val events = fixedReferenceEvents.asSequence()
            .filter {
                it.detectorSource == VideoTestDetectorSource.FIXED_REFERENCE_SHADOW &&
                    (it.foot == SpeedLanding.LEFT || it.foot == SpeedLanding.RIGHT)
            }
            .sortedBy { it.videoTimestampMillis }
            .toList()
            .mapIndexed(::IndexedEvent)
        val provisional = arrayOfNulls<ProvisionalDecision>(events.size)

        events.filter {
            it.event.foot == SpeedLanding.RIGHT &&
                it.event.videoTimestampMillis < goTimestampMillis
        }
            .forEach { event ->
                provisional[event.index] = ProvisionalDecision(
                    accepted = false,
                    evidence = VideoTestAlternationGuardEvidence.NONE,
                    rejectReason = VideoTestAlternationGuardRejectReason.BEFORE_GO,
                )
            }

        val afterGo = events.filter { it.event.videoTimestampMillis >= goTimestampMillis }
        val establishment = findEstablishment(afterGo, minimumTransitionGapMillis)
        if (establishment == null) {
            afterGo.filter { it.event.foot == SpeedLanding.RIGHT }.forEach { event ->
                provisional[event.index] = ProvisionalDecision(
                    accepted = false,
                    evidence = VideoTestAlternationGuardEvidence.NONE,
                    rejectReason = VideoTestAlternationGuardRejectReason.UNCONFIRMED_ALTERNATION,
                )
            }
        } else {
            val confirmedRights = establishment.confirmedEvents
                .filter { it.event.foot == SpeedLanding.RIGHT }
                .map { it.index }
                .toSet()
            afterGo.take(establishment.endIndexInclusive + 1)
                .filter { it.event.foot == SpeedLanding.RIGHT }
                .forEach { event ->
                    provisional[event.index] = if (event.index in confirmedRights) {
                        ProvisionalDecision(
                            accepted = true,
                            evidence = VideoTestAlternationGuardEvidence.CONFIRMED_SEQUENCE,
                            rejectReason = VideoTestAlternationGuardRejectReason.NONE,
                        )
                    } else {
                        ProvisionalDecision(
                            accepted = false,
                            evidence = VideoTestAlternationGuardEvidence.NONE,
                            rejectReason = VideoTestAlternationGuardRejectReason.UNCONFIRMED_ALTERNATION,
                        )
                    }
                }

            var lastAcceptedRight = events.filter { it.index in confirmedRights }
                .maxOf { it.event.videoTimestampMillis }
            var lastLeft = afterGo.take(establishment.endIndexInclusive + 1)
                .filter { it.event.foot == SpeedLanding.LEFT }
                .maxOfOrNull { it.event.videoTimestampMillis }
            var consecutiveBridges = 0

            afterGo.drop(establishment.endIndexInclusive + 1).forEach { event ->
                if (event.event.foot == SpeedLanding.LEFT) {
                    lastLeft = event.event.videoTimestampMillis
                    return@forEach
                }

                val interval = event.event.videoTimestampMillis - lastAcceptedRight
                val latestLeft = lastLeft
                val transitionFromLatestLeftMillis = latestLeft?.let {
                    event.event.videoTimestampMillis - it
                }
                val hasRecentNewLeft = latestLeft != null &&
                    latestLeft > lastAcceptedRight &&
                    transitionFromLatestLeftMillis != null &&
                    transitionFromLatestLeftMillis in
                    minimumTransitionGapMillis..MAX_ALTERNATION_GAP_MILLIS
                val hasTooFastNewLeft = latestLeft != null &&
                    latestLeft > lastAcceptedRight &&
                    transitionFromLatestLeftMillis != null &&
                    transitionFromLatestLeftMillis < minimumTransitionGapMillis
                val provisionalDecision = when {
                    interval < REFRACTORY_MILLIS -> ProvisionalDecision(
                        accepted = false,
                        evidence = VideoTestAlternationGuardEvidence.NONE,
                        rejectReason = VideoTestAlternationGuardRejectReason.REFRACTORY,
                    )
                    hasRecentNewLeft -> ProvisionalDecision(
                        accepted = true,
                        evidence = VideoTestAlternationGuardEvidence.RECENT_LEFT,
                        rejectReason = VideoTestAlternationGuardRejectReason.NONE,
                    )
                    hasTooFastNewLeft -> ProvisionalDecision(
                        accepted = false,
                        evidence = VideoTestAlternationGuardEvidence.NONE,
                        rejectReason = VideoTestAlternationGuardRejectReason.TRANSITION_TOO_FAST,
                    )
                    interval > MAX_ALTERNATION_GAP_MILLIS -> ProvisionalDecision(
                        accepted = false,
                        evidence = VideoTestAlternationGuardEvidence.NONE,
                        rejectReason = VideoTestAlternationGuardRejectReason.MISSING_ALTERNATION,
                    )
                    consecutiveBridges >= MAX_CONSECUTIVE_CADENCE_BRIDGES -> ProvisionalDecision(
                        accepted = false,
                        evidence = VideoTestAlternationGuardEvidence.NONE,
                        rejectReason = VideoTestAlternationGuardRejectReason.BRIDGE_LIMIT,
                    )
                    else -> ProvisionalDecision(
                        accepted = true,
                        evidence = VideoTestAlternationGuardEvidence.CADENCE_BRIDGE,
                        rejectReason = VideoTestAlternationGuardRejectReason.NONE,
                    )
                }
                provisional[event.index] = provisionalDecision
                if (provisionalDecision.accepted) {
                    lastAcceptedRight = event.event.videoTimestampMillis
                    if (hasRecentNewLeft) {
                        consecutiveBridges = 0
                    } else {
                        consecutiveBridges += 1
                    }
                }
            }
        }

        var count = 0
        var lastAcceptedTimestampMillis: Long? = null
        val decisions = events.filter { it.event.foot == SpeedLanding.RIGHT }
            .map { event ->
                val decision = checkNotNull(provisional[event.index])
                val interval = lastAcceptedTimestampMillis?.let {
                    event.event.videoTimestampMillis - it
                }
                if (decision.accepted) {
                    count += 1
                    lastAcceptedTimestampMillis = event.event.videoTimestampMillis
                }
                VideoTestAlternationGuardDecision(
                    videoTimestampMillis = event.event.videoTimestampMillis,
                    landingMethod = event.event.landingMethod,
                    accepted = decision.accepted,
                    countAfter = count,
                    intervalSinceAcceptedMillis = interval,
                    evidence = decision.evidence,
                    rejectReason = decision.rejectReason,
                )
            }

        return VideoTestAlternationGuardCounterResult(
            countedRightSteps = count,
            acceptedRightEvents = decisions.count { it.accepted },
            rejectedRightEvents = decisions.count { !it.accepted },
            confirmedSequenceAccepts = decisions.count {
                it.evidence == VideoTestAlternationGuardEvidence.CONFIRMED_SEQUENCE
            },
            recentLeftAccepts = decisions.count {
                it.evidence == VideoTestAlternationGuardEvidence.RECENT_LEFT
            },
            cadenceBridgeAccepts = decisions.count {
                it.evidence == VideoTestAlternationGuardEvidence.CADENCE_BRIDGE
            },
            refractoryRejects = decisions.count {
                it.rejectReason == VideoTestAlternationGuardRejectReason.REFRACTORY
            },
            unconfirmedAlternationRejects = decisions.count {
                it.rejectReason == VideoTestAlternationGuardRejectReason.UNCONFIRMED_ALTERNATION
            },
            missingAlternationRejects = decisions.count {
                it.rejectReason == VideoTestAlternationGuardRejectReason.MISSING_ALTERNATION
            },
            bridgeLimitRejects = decisions.count {
                it.rejectReason == VideoTestAlternationGuardRejectReason.BRIDGE_LIMIT
            },
            transitionTooFastRejects = decisions.count {
                it.rejectReason == VideoTestAlternationGuardRejectReason.TRANSITION_TOO_FAST
            },
            decisions = decisions,
        )
    }

    private fun findEstablishment(
        events: List<IndexedEvent>,
        minimumTransitionGapMillis: Long,
    ): Establishment? {
        val transitions = mutableListOf<IndexedEvent>()
        events.forEachIndexed { localIndex, indexedEvent ->
            val previous = transitions.lastOrNull()
            if (previous?.event?.foot == indexedEvent.event.foot) {
                transitions[transitions.lastIndex] = indexedEvent
            } else {
                transitions += indexedEvent
            }
            if (transitions.size >= 3) {
                val candidate = transitions.takeLast(3)
                val firstGap = candidate[1].event.videoTimestampMillis -
                    candidate[0].event.videoTimestampMillis
                val secondGap = candidate[2].event.videoTimestampMillis -
                    candidate[1].event.videoTimestampMillis
                val confirmedRightTimestamps = candidate
                    .filter { it.event.foot == SpeedLanding.RIGHT }
                    .map { it.event.videoTimestampMillis }
                val rightSpacingValid = confirmedRightTimestamps.zipWithNext().all {
                    (first, second) -> second - first >= REFRACTORY_MILLIS
                }
                if (
                    firstGap >= minimumTransitionGapMillis &&
                    secondGap >= minimumTransitionGapMillis &&
                    firstGap <= MAX_ALTERNATION_GAP_MILLIS &&
                    secondGap <= MAX_ALTERNATION_GAP_MILLIS &&
                    rightSpacingValid
                ) {
                    return Establishment(
                        endIndexInclusive = localIndex,
                        confirmedEvents = candidate,
                    )
                }
            }
        }
        return null
    }

    private data class IndexedEvent(
        val index: Int,
        val event: VideoTestLandingEvent,
    )

    private data class Establishment(
        val endIndexInclusive: Int,
        val confirmedEvents: List<IndexedEvent>,
    )

    private data class ProvisionalDecision(
        val accepted: Boolean,
        val evidence: VideoTestAlternationGuardEvidence,
        val rejectReason: VideoTestAlternationGuardRejectReason,
    )
}

/**
 * V13 diagnostic-only refinement of V12. It rejects side transitions that occur within one or two
 * sampled frames and therefore cannot represent a genuine alternating step. V12 remains available
 * unchanged as the comparison baseline.
 */
object VideoTestMinimumTransitionGapCounter {
    const val MIN_TRANSITION_GAP_MILLIS = 100L

    fun evaluate(
        fixedReferenceEvents: List<VideoTestLandingEvent>,
        goTimestampMillis: Long,
    ): VideoTestAlternationGuardCounterResult =
        VideoTestAlternationGuardCounter.evaluateWithMinimumTransitionGap(
            fixedReferenceEvents = fixedReferenceEvents,
            goTimestampMillis = goTimestampMillis,
            minimumTransitionGapMillis = MIN_TRANSITION_GAP_MILLIS,
        )
}
