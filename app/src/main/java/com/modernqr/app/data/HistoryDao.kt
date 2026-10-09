package com.modernqr.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history_table ORDER BY isPinned DESC, timestamp DESC")
    fun getAllHistory(): Flow<List<HistoryItem>>

    @Query("SELECT * FROM history_table WHERE content LIKE '%' || :query || '%' ORDER BY isPinned DESC, timestamp DESC")
    fun searchHistory(query: String): Flow<List<HistoryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: HistoryItem)

    @Update
    suspend fun update(item: HistoryItem)

    @Delete
    suspend fun delete(item: HistoryItem)

    @Query("DELETE FROM history_table")
    suspend fun clearHistory()
}
