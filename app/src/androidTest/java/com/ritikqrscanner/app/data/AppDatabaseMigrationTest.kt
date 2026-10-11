package com.ritikqrscanner.app.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    @Test
    fun migration1To2PreservesHistoryAndAddsPinning() {
        helper.createDatabase(TEST_DATABASE, 1).apply {
            execSQL(
                """
                INSERT INTO history_table (id, content, format, type, timestamp)
                VALUES (1, 'preserved value', 'QR_CODE', 'Scanned', 1234)
                """.trimIndent()
            )
            close()
        }

        helper.runMigrationsAndValidate(
            TEST_DATABASE,
            2,
            true,
            AppDatabase.MIGRATION_1_2
        ).use { database ->
            database.query("SELECT content, isPinned FROM history_table WHERE id = 1").use { cursor ->
                cursor.moveToFirst()
                assertEquals("preserved value", cursor.getString(0))
                assertEquals(0, cursor.getInt(1))
            }
        }
    }

    @Test
    fun migration1To2AcceptsEarlyVersionOneSchemaThatAlreadyHadPinning() {
        helper.createDatabase(TEST_DATABASE_WITH_PIN, 1).apply {
            execSQL("ALTER TABLE history_table ADD COLUMN isPinned INTEGER NOT NULL DEFAULT 0")
            execSQL(
                """
                INSERT INTO history_table (id, content, format, type, timestamp, isPinned)
                VALUES (2, 'already pinned', 'QR_CODE', 'Generated', 5678, 1)
                """.trimIndent()
            )
            close()
        }

        helper.runMigrationsAndValidate(
            TEST_DATABASE_WITH_PIN,
            2,
            true,
            AppDatabase.MIGRATION_1_2
        ).use { database ->
            database.query("SELECT content, isPinned FROM history_table WHERE id = 2").use { cursor ->
                cursor.moveToFirst()
                assertEquals("already pinned", cursor.getString(0))
                assertEquals(1, cursor.getInt(1))
            }
        }
    }

    private companion object {
        const val TEST_DATABASE = "history-migration-test"
        const val TEST_DATABASE_WITH_PIN = "history-migration-test-with-pin"
    }
}
