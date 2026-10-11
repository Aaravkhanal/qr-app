package com.ritikqrscanner.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query(
        """
        SELECT * FROM history_table
        WHERE (:escapedQuery = '' OR content LIKE '%' || :escapedQuery || '%' ESCAPE '\')
          AND (:entryType IS NULL OR type = :entryType)
          AND (:pinnedOnly = 0 OR isPinned = 1)
        ORDER BY isPinned DESC, timestamp DESC
        """
    )
    fun observeHistory(
        escapedQuery: String,
        entryType: String?,
        pinnedOnly: Boolean
    ): Flow<List<HistoryItem>>

    @Query("SELECT COUNT(*) FROM history_table")
    fun observeCount(): Flow<Int>

    @Query("SELECT * FROM history_table ORDER BY timestamp DESC, id DESC LIMIT 1")
    suspend fun getMostRecent(): HistoryItem?

    @Query("SELECT * FROM history_table")
    suspend fun getAllOnce(): List<HistoryItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: HistoryItem): Long

    @Transaction
    suspend fun insertUnlessConsecutiveDuplicate(item: HistoryItem): Boolean {
        val latest = getMostRecent()
        if (latest?.content == item.content && latest.format == item.format && latest.type == item.type) {
            return false
        }
        insert(item)
        return true
    }

    @Update
    suspend fun update(item: HistoryItem)

    @Delete
    suspend fun delete(item: HistoryItem)

    @Query("DELETE FROM history_table WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Int>)

    @Query("DELETE FROM history_table")
    suspend fun clearHistory()
}
