package com.modernqr.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.modernqr.app.data.AppDatabase
import com.modernqr.app.data.HistoryItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).historyDao()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filterType = MutableStateFlow("All") // All, Scanned, Generated, Pinned
    val filterType: StateFlow<String> = _filterType.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val history: StateFlow<List<HistoryItem>> = _searchQuery
        .combine(_filterType) { query, filter -> Pair(query, filter) }
        .flatMapLatest { (query, filter) ->
            val baseFlow = if (query.isBlank()) dao.getAllHistory() else dao.searchHistory(query)
            baseFlow.map { items ->
                when (filter) {
                    "Scanned" -> items.filter { it.type == "Scanned" }
                    "Generated" -> items.filter { it.type == "Generated" }
                    "Pinned" -> items.filter { it.isPinned }
                    else -> items
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilterType(filter: String) {
        _filterType.value = filter
    }

    fun addHistoryItem(content: String, format: String, type: String) {
        viewModelScope.launch {
            dao.insert(HistoryItem(content = content, format = format, type = type))
        }
    }

    fun updateHistoryItem(item: HistoryItem) {
        viewModelScope.launch {
            dao.update(item)
        }
    }

    fun deleteHistoryItem(item: HistoryItem) {
        viewModelScope.launch {
            dao.delete(item)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            dao.clearHistory()
        }
    }
}
