package com.modernqr.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "history_table")
data class HistoryItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val content: String,
    val format: String,
    val type: String, // "Scanned" or "Generated"
    val timestamp: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false
)
