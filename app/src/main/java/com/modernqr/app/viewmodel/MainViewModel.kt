package com.modernqr.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.modernqr.app.data.AppDatabase
import com.modernqr.app.data.HistoryItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).historyDao()

    val history: StateFlow<List<HistoryItem>> = dao.getAllHistory()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

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
