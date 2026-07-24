package com.ropeskill.app

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class TrainingSession(
    val id: Long,
    val exerciseType: String,
    val startedAtEpochMillis: Long,
    val completedAtEpochMillis: Long,
    val durationMillis: Long,
    val jumpCount: Int,
)

internal data class NewTrainingSession(
    val exerciseType: String = BASIC_BOUNCE_EXERCISE,
    val startedAtEpochMillis: Long,
    val completedAtEpochMillis: Long,
    val durationMillis: Long,
    val jumpCount: Int,
)

@Entity(tableName = "training_sessions")
internal data class TrainingSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val exerciseType: String,
    val startedAtEpochMillis: Long,
    val completedAtEpochMillis: Long,
    val durationMillis: Long,
    val jumpCount: Int,
)

@Dao
internal interface TrainingSessionDao {
    @Insert
    suspend fun insert(session: TrainingSessionEntity): Long

    @Query("SELECT * FROM training_sessions ORDER BY completedAtEpochMillis DESC, id DESC LIMIT 1")
    fun observeLatest(): Flow<TrainingSessionEntity?>

    @Query("SELECT * FROM training_sessions ORDER BY completedAtEpochMillis DESC, id DESC")
    fun observeAll(): Flow<List<TrainingSessionEntity>>
}

@Database(
    entities = [TrainingSessionEntity::class],
    version = 1,
    exportSchema = false,
)
internal abstract class RopeSkillDatabase : RoomDatabase() {
    abstract fun trainingSessionDao(): TrainingSessionDao

    companion object {
        @Volatile
        private var instance: RopeSkillDatabase? = null

        fun getInstance(context: Context): RopeSkillDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    RopeSkillDatabase::class.java,
                    DATABASE_NAME,
                ).build().also { instance = it }
            }

        private const val DATABASE_NAME = "ropeskill.db"
    }
}

internal class SessionRepository(
    private val dao: TrainingSessionDao,
) {
    val latestSession: Flow<TrainingSession?> =
        dao.observeLatest().map { entity -> entity?.toTrainingSession() }

    val sessions: Flow<List<TrainingSession>> =
        dao.observeAll().map { entities -> entities.map { it.toTrainingSession() } }

    suspend fun save(session: NewTrainingSession): Long =
        dao.insert(session.toEntity())

    companion object {
        fun create(context: Context): SessionRepository =
            SessionRepository(RopeSkillDatabase.getInstance(context).trainingSessionDao())
    }
}

internal fun NewTrainingSession.toEntity(): TrainingSessionEntity =
    TrainingSessionEntity(
        exerciseType = exerciseType,
        startedAtEpochMillis = startedAtEpochMillis,
        completedAtEpochMillis = completedAtEpochMillis,
        durationMillis = durationMillis.coerceAtLeast(0L),
        jumpCount = jumpCount.coerceAtLeast(0),
    )

private fun TrainingSessionEntity.toTrainingSession(): TrainingSession =
    TrainingSession(
        id = id,
        exerciseType = exerciseType,
        startedAtEpochMillis = startedAtEpochMillis,
        completedAtEpochMillis = completedAtEpochMillis,
        durationMillis = durationMillis,
        jumpCount = jumpCount,
    )

internal const val BASIC_BOUNCE_EXERCISE = "BASIC_BOUNCE"
