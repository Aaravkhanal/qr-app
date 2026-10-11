package com.ritikqrscanner.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HistoryDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var dao: HistoryDao

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.historyDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun crudOrderingAndPinning() = runBlocking {
        val olderId = dao.insert(history("older", timestamp = 10))
        val newerId = dao.insert(history("newer", timestamp = 20))
        val pinnedId = dao.insert(history("pinned", timestamp = 5, isPinned = true))

        assertEquals(
            listOf("pinned", "newer", "older"),
            dao.observeHistory("", null, false).first().map(HistoryItem::content)
        )
        assertEquals(listOf("pinned"), dao.observeHistory("", null, true).first().map(HistoryItem::content))

        dao.update(history("newer", timestamp = 20, id = newerId.toInt(), isPinned = true))
        assertEquals(2, dao.observeHistory("", null, true).first().size)

        dao.delete(history("older", timestamp = 10, id = olderId.toInt()))
        assertEquals(2, dao.observeCount().first())
        dao.deleteByIds(listOf(pinnedId.toInt(), newerId.toInt()))
        assertEquals(0, dao.observeCount().first())
    }

    @Test
    fun literalSearchAndTypeFiltersUseDaoQueries() = runBlocking {
        dao.insert(history("100%_complete", type = "Scanned", timestamp = 30))
        dao.insert(history("100-percent", type = "Generated", timestamp = 20))
        dao.insert(history("ordinary", type = "Scanned", timestamp = 10))

        val literal = HistoryPrivacyPolicy.escapeLikeQuery("%_")
        assertEquals(listOf("100%_complete"), dao.observeHistory(literal, null, false).first().map(HistoryItem::content))
        assertEquals(2, dao.observeHistory("", "Scanned", false).first().size)
        assertEquals(1, dao.observeHistory("", "Generated", false).first().size)
    }

    @Test
    fun repositorySkipsSensitiveAndConsecutiveDuplicateValues() = runBlocking {
        val repository = HistoryRepository(dao)

        assertEquals(HistorySaveResult.SAVED, repository.add("hello", "QR_CODE", "Scanned"))
        assertEquals(HistorySaveResult.DUPLICATE_SKIPPED, repository.add("hello", "QR_CODE", "Scanned"))
        assertEquals(
            HistorySaveResult.SENSITIVE_SKIPPED,
            repository.add("WIFI:T:WPA;S:Home;P:password8;;", "QR_CODE", "Scanned")
        )
        assertEquals(1, dao.observeCount().first())
    }

    @Test
    fun privacyCleanupRemovesSensitiveLegacyRowsAndClearRemovesEverything() = runBlocking {
        dao.insert(history("hello"))
        dao.insert(history("tel:+15551234567", timestamp = 20))
        dao.insert(history("smsto:+15551234567:private", timestamp = 30))

        val repository = HistoryRepository(dao)
        repository.removePreviouslyStoredSensitiveEntries()
        val remaining = dao.getAllOnce()
        assertEquals(listOf("hello"), remaining.map(HistoryItem::content))
        assertTrue(remaining.none { it.content.startsWith("tel:") })

        repository.clear()
        assertFalse(dao.observeHistory("", null, false).first().isNotEmpty())
    }

    private fun history(
        content: String,
        type: String = "Scanned",
        timestamp: Long = 10,
        id: Int = 0,
        isPinned: Boolean = false
    ) = HistoryItem(
        id = id,
        content = content,
        format = "QR_CODE",
        type = type,
        timestamp = timestamp,
        isPinned = isPinned
    )
}
