package com.ropeskill.app

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomSchemaTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        RopeSkillDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun versionOne_containsExpectedTrainingSessionColumns() {
        val database = helper.createDatabase(TEST_DATABASE_NAME, 1)
        try {
            val columns = buildSet {
                database.query("PRAGMA table_info(`training_sessions`)").use { cursor ->
                    val nameIndex = cursor.getColumnIndexOrThrow("name")
                    while (cursor.moveToNext()) add(cursor.getString(nameIndex))
                }
            }

            assertEquals(
                setOf(
                    "id",
                    "exerciseType",
                    "startedAtEpochMillis",
                    "completedAtEpochMillis",
                    "durationMillis",
                    "jumpCount",
                ),
                columns,
            )
        } finally {
            database.close()
        }
    }

    private companion object {
        const val TEST_DATABASE_NAME = "room-schema-test"
    }
}
