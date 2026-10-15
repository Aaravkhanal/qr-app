package com.ritikqrscanner.app.viewmodel

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.lifecycle.viewModelScope
import com.ritikqrscanner.app.data.AppDatabase
import com.ritikqrscanner.app.data.HistoryRepository
import com.ritikqrscanner.app.data.SettingsManager
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainViewModelTest {
    @Test
    fun historyCommandsSearchFiltersAndPreferenceAreApplied() = runBlocking {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val database = Room.inMemoryDatabaseBuilder(application, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val settings = SettingsManager(application)
        settings.setSaveHistory(true)
        val viewModel = MainViewModel(
            application,
            HistoryRepository(database.historyDao()),
            settings
        )

        try {
            viewModel.addHistoryItem("alpha", "QR_CODE", "Scanned").join()
            viewModel.addHistoryItem("beta", "QR_CODE", "Generated").join()
            assertEquals(2, withTimeout(2_000) { viewModel.historyCount.filter { it == 2 }.first() })

            viewModel.setSearchQuery("alpha")
            assertEquals(
                listOf("alpha"),
                withTimeout(2_000) {
                    viewModel.history.filter { items -> items.map { it.content } == listOf("alpha") }.first()
                }.map { it.content }
            )

            viewModel.setSearchQuery("")
            viewModel.setFilterType("Generated")
            assertEquals(
                listOf("beta"),
                withTimeout(2_000) {
                    viewModel.history.filter { items -> items.map { it.content } == listOf("beta") }.first()
                }.map { it.content }
            )

            settings.setSaveHistory(false)
            viewModel.addHistoryItem("not retained", "QR_CODE", "Scanned").join()
            assertEquals(2, database.historyDao().observeCount().first())

            viewModel.clearHistory().join()
            assertTrue(database.historyDao().getAllOnce().isEmpty())
        } finally {
            viewModel.viewModelScope.cancel()
            database.close()
        }
    }
}
