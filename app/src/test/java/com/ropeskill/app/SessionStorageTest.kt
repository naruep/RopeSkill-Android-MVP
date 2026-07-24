package com.ropeskill.app

import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionStorageTest {
    @Test
    fun newTrainingSession_mapsAllMinimumFields() {
        val entity = NewTrainingSession(
            startedAtEpochMillis = 1_000L,
            completedAtEpochMillis = 11_000L,
            durationMillis = 10_000L,
            jumpCount = 42,
        ).toEntity()

        assertEquals(BASIC_BOUNCE_EXERCISE, entity.exerciseType)
        assertEquals(1_000L, entity.startedAtEpochMillis)
        assertEquals(11_000L, entity.completedAtEpochMillis)
        assertEquals(10_000L, entity.durationMillis)
        assertEquals(42, entity.jumpCount)
    }

    @Test
    fun invalidNegativeSummaryValues_areClampedBeforeStorage() {
        val entity = NewTrainingSession(
            startedAtEpochMillis = 1_000L,
            completedAtEpochMillis = 1_000L,
            durationMillis = -1L,
            jumpCount = -1,
        ).toEntity()

        assertEquals(0L, entity.durationMillis)
        assertEquals(0, entity.jumpCount)
    }

    @Test
    fun onlyStartedSession_isEligibleForStorage() {
        assertFalse(shouldPersistSession(0L))
        assertTrue(shouldPersistSession(1L))
    }

    @Test
    fun sessionDateTime_usesExpectedHistoryFormat() {
        val epochMillis = Instant.parse("2026-07-24T03:41:00Z").toEpochMilli()

        assertEquals(
            "24 Jul 2026, 03:41",
            formatSessionDateTime(epochMillis, ZoneOffset.UTC),
        )
    }
}
