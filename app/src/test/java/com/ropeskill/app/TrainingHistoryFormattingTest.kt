package com.ropeskill.app

import org.junit.Assert.assertEquals
import org.junit.Test

class TrainingHistoryFormattingTest {
    @Test
    fun speedSession_usesRightStepLabels() {
        assertEquals("SPEED 30", formatSessionExerciseName(SPEED_30_EXERCISE))
        assertEquals("1 RIGHT STEP", formatSessionCount(session(SPEED_30_EXERCISE, 1)))
        assertEquals("2 RIGHT STEPS", formatSessionCount(session(SPEED_30_EXERCISE, 2)))
    }

    @Test
    fun basicBounceSession_usesJumpLabels() {
        assertEquals("1 JUMP", formatSessionCount(session(BASIC_BOUNCE_EXERCISE, 1)))
        assertEquals("2 JUMPS", formatSessionCount(session(BASIC_BOUNCE_EXERCISE, 2)))
    }

    private fun session(exerciseType: String, count: Int) = TrainingSession(
        id = 1,
        exerciseType = exerciseType,
        startedAtEpochMillis = 1_000L,
        completedAtEpochMillis = 2_000L,
        durationMillis = 1_000L,
        jumpCount = count,
    )
}
