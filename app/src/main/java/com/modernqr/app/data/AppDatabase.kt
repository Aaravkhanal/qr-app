package com.modernqr.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [HistoryItem::class], version = 2, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "modern_qr_database"
                ).addMigrations(MIGRATION_1_2).build()
                INSTANCE = instance
                instance
            }
        }

        /**
         * Version 1 contained the base history columns. Version 2 added pinning. The column check
         * also preserves data from early version-1 builds that shipped the pin column before the
         * database version was updated.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val hasPinnedColumn = db.query("PRAGMA table_info(`history_table`)").use { cursor ->
                    val nameIndex = cursor.getColumnIndex("name")
                    var found = false
                    while (cursor.moveToNext()) {
                        if (cursor.getString(nameIndex) == "isPinned") {
                            found = true
                            break
                        }
                    }
                    found
                }
                if (!hasPinnedColumn) {
                    db.execSQL(
                        """
                        CREATE TABLE `history_table_v2` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `content` TEXT NOT NULL,
                            `format` TEXT NOT NULL,
                            `type` TEXT NOT NULL,
                            `timestamp` INTEGER NOT NULL,
                            `isPinned` INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
                        """
                        INSERT INTO `history_table_v2` (`id`, `content`, `format`, `type`, `timestamp`, `isPinned`)
                        SELECT `id`, `content`, `format`, `type`, `timestamp`, 0 FROM `history_table`
                        """.trimIndent()
                    )
                    db.execSQL("DROP TABLE `history_table`")
                    db.execSQL("ALTER TABLE `history_table_v2` RENAME TO `history_table`")
                }
            }
        }
    }
}
