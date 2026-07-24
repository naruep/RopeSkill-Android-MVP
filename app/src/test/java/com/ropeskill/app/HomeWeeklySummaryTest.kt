package com.ropeskill.app

import java.time.LocalDateTime
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeWeeklySummaryTest {
    @Test
    fun summarizeCurrentWeek_includesOnlyMondayThroughNow() {
        val now = epochMillis(2026, 7, 24, 12)
        val sessions = listOf(
            session(id = 1, completedAt = epochMillis(2026, 7, 20, 8), jumps = 100, duration = 60_000),
            session(id = 2, completedAt = epochMillis(2026, 7, 24, 10), jumps = 210, duration = 180_000),
            session(id = 3, completedAt = epochMillis(2026, 7, 19, 23), jumps = 999, duration = 999_000),
            session(id = 4, completedAt = epochMillis(2026, 7, 24, 13), jumps = 999, duration = 999_000),
        )

        val result = summarizeCurrentWeek(
            sessions = sessions,
            nowEpochMillis = now,
            zoneId = ZoneOffset.UTC,
        )

        assertEquals(310, result.jumpCount)
        assertEquals(240_000, result.durationMillis)
        assertEquals(2, result.sessionCount)
    }

    @Test
    fun summarizeCurrentWeek_returnsZerosWhenHistoryIsEmpty() {
        val result = summarizeCurrentWeek(
            sessions = emptyList(),
            nowEpochMillis = epochMillis(2026, 7, 24, 12),
            zoneId = ZoneOffset.UTC,
        )

        assertEquals(WeeklyTrainingSummary(0, 0, 0), result)
    }

    private fun session(
        id: Long,
        completedAt: Long,
        jumps: Int,
        duration: Long,
    ) = TrainingSession(
        id = id,
        exerciseType = BASIC_BOUNCE_EXERCISE,
        startedAtEpochMillis = completedAt - duration,
        completedAtEpochMillis = completedAt,
        durationMillis = duration,
        jumpCount = jumps,
    )

    private fun epochMillis(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
    ): Long = LocalDateTime.of(year, month, day, hour, 0)
        .toInstant(ZoneOffset.UTC)
        .toEpochMilli()
}
