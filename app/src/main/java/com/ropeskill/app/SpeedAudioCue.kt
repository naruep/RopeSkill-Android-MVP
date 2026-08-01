package com.ropeskill.app

internal enum class SpeedAudioCue(
    val triggerMillis: Long,
    val spokenText: String?,
) {
    TEN_SECONDS(10_000L, "Ten"),
    TWENTY_SECONDS(20_000L, "Twenty"),
    FIVE_SECONDS_LEFT(25_000L, "Five"),
    FOUR_SECONDS_LEFT(26_000L, "Four"),
    THREE_SECONDS_LEFT(27_000L, "Three"),
    TWO_SECONDS_LEFT(28_000L, "Two"),
    ONE_SECOND_LEFT(29_000L, "One"),
    COMPLETE(30_000L, null),
}

/**
 * Emits each Speed 30 cue once when elapsed time crosses its threshold.
 *
 * This is intentionally independent from pose frames so inference latency or skipped frames cannot
 * suppress, repeat, or shift a time cue.
 */
internal class SpeedAudioCueScheduler(
    private val schedule: List<SpeedAudioCue> = SpeedAudioCue.entries,
) {
    private var previousElapsedMillis = 0L
    private val emitted = mutableSetOf<SpeedAudioCue>()

    init {
        require(schedule.zipWithNext().all { (left, right) ->
            left.triggerMillis < right.triggerMillis
        })
    }

    fun reset(elapsedMillis: Long = 0L) {
        require(elapsedMillis >= 0L)
        previousElapsedMillis = elapsedMillis
        emitted.clear()
    }

    fun advanceTo(elapsedMillis: Long): List<SpeedAudioCue> {
        require(elapsedMillis >= previousElapsedMillis) {
            "Speed audio cue time must be monotonic"
        }
        val due = schedule.filter { cue ->
            cue !in emitted &&
                previousElapsedMillis < cue.triggerMillis &&
                elapsedMillis >= cue.triggerMillis
        }
        emitted += due
        previousElapsedMillis = elapsedMillis
        return due
    }
}
