package com.ropeskill.app

import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
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

    @Test
    fun delete_removesOnlyRequestedSession() = runBlocking {
        val first = storedSession(id = 1, jumpCount = 10)
        val second = storedSession(id = 2, jumpCount = 20)
        val dao = FakeTrainingSessionDao(listOf(first, second))
        val repository = SessionRepository(dao)

        assertTrue(repository.delete(first.id))
        assertEquals(listOf(second), dao.currentSessions())
        assertFalse(repository.delete(999))
        assertEquals(listOf(second), dao.currentSessions())
    }

    private fun storedSession(
        id: Long,
        jumpCount: Int,
    ) = TrainingSessionEntity(
        id = id,
        exerciseType = BASIC_BOUNCE_EXERCISE,
        startedAtEpochMillis = 1_000L,
        completedAtEpochMillis = 2_000L,
        durationMillis = 1_000L,
        jumpCount = jumpCount,
    )
}

private class FakeTrainingSessionDao(
    initialSessions: List<TrainingSessionEntity>,
) : TrainingSessionDao {
    private val sessions = MutableStateFlow(initialSessions)

    override suspend fun insert(session: TrainingSessionEntity): Long {
        sessions.value += session
        return session.id
    }

    override fun observeLatest(): Flow<TrainingSessionEntity?> =
        MutableStateFlow(sessions.value.maxByOrNull { it.id })

    override fun observeAll(): Flow<List<TrainingSessionEntity>> = sessions

    override suspend fun deleteById(sessionId: Long): Int {
        val updated = sessions.value.filterNot { it.id == sessionId }
        val deletedCount = sessions.value.size - updated.size
        sessions.value = updated
        return deletedCount
    }

    fun currentSessions(): List<TrainingSessionEntity> = sessions.value
}
