package com.ropeskill.app

/** A landing classification produced by [PoseSpeedLandingClassifier]. */
enum class SpeedLanding {
    NONE,
    LEFT,
    RIGHT,
    BOTH,
    UNCLEAR,
}

enum class SpeedAlternationState {
    RIGHT_ELIGIBLE,
    NEED_LEFT,
}

enum class SpeedRejectReason {
    NONE,
    NOT_STARTED,
    BEFORE_GO,
    AFTER_TIME_LIMIT,
    OUT_OF_ORDER_TIMESTAMP,
    REPEATED_LEFT,
    REPEATED_RIGHT,
    BOTH_FEET,
    UNCLEAR_LANDING,
}

data class SpeedStepResult(
    val countedRightStep: Boolean,
    val count: Int,
    val landing: SpeedLanding,
    val timestampMillis: Long,
    val stateBefore: SpeedAlternationState,
    val stateAfter: SpeedAlternationState,
    val rejectReason: SpeedRejectReason = SpeedRejectReason.NONE,
)

data class SpeedStepDiagnostics(
    val goTimestampMillis: Long?,
    val endTimestampExclusiveMillis: Long?,
    val leftLandingTimestampsMillis: List<Long>,
    val rightLandingTimestampsMillis: List<Long>,
    val countedRightTimestampsMillis: List<Long>,
    val repeatedLeftRejects: Int,
    val repeatedRightRejects: Int,
    val bothFeetRejects: Int,
    val unclearLandingRejects: Int,
    val outOfWindowRejects: Int,
    val outOfOrderTimestampRejects: Int,
    val trackingLossEvents: Int,
    val trackingLossDurationMillis: Long,
)

/**
 * Applies the IJRU right-foot alternation rule to already-classified landing events.
 *
 * This T-752 core deliberately does not infer landings from pose landmarks. Keeping motion
 * classification separate makes the counting rule deterministic and independently testable.
 * Tracking loss never unlocks a repeated right landing.
 */
class SpeedStepDetector(
    private val durationMillis: Long = SPEED_30_DURATION_MILLIS,
) {
    private var goTimestampMillis: Long? = null
    private var alternationState = SpeedAlternationState.RIGHT_ELIGIBLE
    private var count = 0
    private var lastProcessedTimestampMillis: Long? = null
    private var trackingLossStartedAtMillis: Long? = null

    private val leftLandingTimestampsMillis = mutableListOf<Long>()
    private val rightLandingTimestampsMillis = mutableListOf<Long>()
    private val countedRightTimestampsMillis = mutableListOf<Long>()
    private var repeatedLeftRejects = 0
    private var repeatedRightRejects = 0
    private var bothFeetRejects = 0
    private var unclearLandingRejects = 0
    private var outOfWindowRejects = 0
    private var outOfOrderTimestampRejects = 0
    private var trackingLossEvents = 0
    private var trackingLossDurationMillis = 0L

    init {
        require(durationMillis > 0L) { "durationMillis must be positive" }
    }

    fun start(goTimestampMillis: Long) {
        reset()
        this.goTimestampMillis = goTimestampMillis
    }

    fun processLanding(
        landing: SpeedLanding,
        timestampMillis: Long,
    ): SpeedStepResult {
        val stateBefore = alternationState
        val goTimestamp = goTimestampMillis
            ?: return result(
                landing,
                timestampMillis,
                stateBefore,
                SpeedRejectReason.NOT_STARTED,
            )

        val lastTimestamp = lastProcessedTimestampMillis
        if (lastTimestamp != null && timestampMillis < lastTimestamp) {
            outOfOrderTimestampRejects += 1
            return result(
                landing,
                timestampMillis,
                stateBefore,
                SpeedRejectReason.OUT_OF_ORDER_TIMESTAMP,
            )
        }
        lastProcessedTimestampMillis = timestampMillis

        if (timestampMillis < goTimestamp) {
            outOfWindowRejects += 1
            return result(
                landing,
                timestampMillis,
                stateBefore,
                SpeedRejectReason.BEFORE_GO,
            )
        }
        if (timestampMillis >= goTimestamp + durationMillis) {
            outOfWindowRejects += 1
            return result(
                landing,
                timestampMillis,
                stateBefore,
                SpeedRejectReason.AFTER_TIME_LIMIT,
            )
        }

        return when (landing) {
            SpeedLanding.NONE -> result(landing, timestampMillis, stateBefore)
            SpeedLanding.LEFT -> processLeft(timestampMillis, stateBefore)
            SpeedLanding.RIGHT -> processRight(timestampMillis, stateBefore)
            SpeedLanding.BOTH -> {
                bothFeetRejects += 1
                result(
                    landing,
                    timestampMillis,
                    stateBefore,
                    SpeedRejectReason.BOTH_FEET,
                )
            }
            SpeedLanding.UNCLEAR -> {
                unclearLandingRejects += 1
                result(
                    landing,
                    timestampMillis,
                    stateBefore,
                    SpeedRejectReason.UNCLEAR_LANDING,
                )
            }
        }
    }

    fun onTrackingLost(timestampMillis: Long) {
        if (trackingLossStartedAtMillis == null) {
            trackingLossStartedAtMillis = timestampMillis
            trackingLossEvents += 1
        }
    }

    fun onTrackingRestored(timestampMillis: Long) {
        val startedAt = trackingLossStartedAtMillis ?: return
        if (timestampMillis >= startedAt) {
            trackingLossDurationMillis += timestampMillis - startedAt
        }
        trackingLossStartedAtMillis = null
    }

    fun diagnostics(): SpeedStepDiagnostics {
        val goTimestamp = goTimestampMillis
        return SpeedStepDiagnostics(
            goTimestampMillis = goTimestamp,
            endTimestampExclusiveMillis = goTimestamp?.plus(durationMillis),
            leftLandingTimestampsMillis = leftLandingTimestampsMillis.toList(),
            rightLandingTimestampsMillis = rightLandingTimestampsMillis.toList(),
            countedRightTimestampsMillis = countedRightTimestampsMillis.toList(),
            repeatedLeftRejects = repeatedLeftRejects,
            repeatedRightRejects = repeatedRightRejects,
            bothFeetRejects = bothFeetRejects,
            unclearLandingRejects = unclearLandingRejects,
            outOfWindowRejects = outOfWindowRejects,
            outOfOrderTimestampRejects = outOfOrderTimestampRejects,
            trackingLossEvents = trackingLossEvents,
            trackingLossDurationMillis = trackingLossDurationMillis,
        )
    }

    fun reset() {
        goTimestampMillis = null
        alternationState = SpeedAlternationState.RIGHT_ELIGIBLE
        count = 0
        lastProcessedTimestampMillis = null
        trackingLossStartedAtMillis = null
        leftLandingTimestampsMillis.clear()
        rightLandingTimestampsMillis.clear()
        countedRightTimestampsMillis.clear()
        repeatedLeftRejects = 0
        repeatedRightRejects = 0
        bothFeetRejects = 0
        unclearLandingRejects = 0
        outOfWindowRejects = 0
        outOfOrderTimestampRejects = 0
        trackingLossEvents = 0
        trackingLossDurationMillis = 0L
    }

    private fun processLeft(
        timestampMillis: Long,
        stateBefore: SpeedAlternationState,
    ): SpeedStepResult {
        leftLandingTimestampsMillis += timestampMillis
        return if (alternationState == SpeedAlternationState.NEED_LEFT) {
            alternationState = SpeedAlternationState.RIGHT_ELIGIBLE
            result(SpeedLanding.LEFT, timestampMillis, stateBefore)
        } else {
            repeatedLeftRejects += 1
            result(
                SpeedLanding.LEFT,
                timestampMillis,
                stateBefore,
                SpeedRejectReason.REPEATED_LEFT,
            )
        }
    }

    private fun processRight(
        timestampMillis: Long,
        stateBefore: SpeedAlternationState,
    ): SpeedStepResult {
        rightLandingTimestampsMillis += timestampMillis
        return if (alternationState == SpeedAlternationState.RIGHT_ELIGIBLE) {
            count += 1
            countedRightTimestampsMillis += timestampMillis
            alternationState = SpeedAlternationState.NEED_LEFT
            result(
                SpeedLanding.RIGHT,
                timestampMillis,
                stateBefore,
                countedRightStep = true,
            )
        } else {
            repeatedRightRejects += 1
            result(
                SpeedLanding.RIGHT,
                timestampMillis,
                stateBefore,
                SpeedRejectReason.REPEATED_RIGHT,
            )
        }
    }

    private fun result(
        landing: SpeedLanding,
        timestampMillis: Long,
        stateBefore: SpeedAlternationState,
        rejectReason: SpeedRejectReason = SpeedRejectReason.NONE,
        countedRightStep: Boolean = false,
    ) = SpeedStepResult(
        countedRightStep = countedRightStep,
        count = count,
        landing = landing,
        timestampMillis = timestampMillis,
        stateBefore = stateBefore,
        stateAfter = alternationState,
        rejectReason = rejectReason,
    )

    companion object {
        const val SPEED_30_DURATION_MILLIS = 30_000L
    }
}
