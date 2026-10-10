package com.modernqr.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.modernqr.app.data.AppDatabase
import com.modernqr.app.data.HistoryRepository
import com.modernqr.app.data.HistoryItem
import com.modernqr.app.data.SettingsManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = HistoryRepository(AppDatabase.getDatabase(application).historyDao())
    private val settings = SettingsManager(application)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filterType = MutableStateFlow("All") // All, Scanned, Generated, Pinned
    val filterType: StateFlow<String> = _filterType.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val history: StateFlow<List<HistoryItem>> = _searchQuery
        .combine(_filterType) { query, filter -> Pair(query, filter) }
        .flatMapLatest { (query, filter) -> repository.observeHistory(query, filter) }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val historyCount: StateFlow<Int> = repository.observeCount()
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    init {
        viewModelScope.launch { repository.removePreviouslyStoredSensitiveEntries() }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilterType(filter: String) {
        _filterType.value = filter
    }

    fun addHistoryItem(content: String, format: String, type: String) {
        viewModelScope.launch {
            if (settings.saveHistoryFlow.first()) repository.add(content, format, type)
        }
    }

    fun updateHistoryItem(item: HistoryItem) {
        viewModelScope.launch {
            repository.update(item)
        }
    }

    fun deleteHistoryItem(item: HistoryItem) {
        viewModelScope.launch {
            repository.delete(item)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clear()
        }
    }
}
