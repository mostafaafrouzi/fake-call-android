package com.afrouzi.fakecall.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.afrouzi.fakecall.data.model.CallHistoryItem
import kotlinx.coroutines.flow.Flow

@Dao
interface CallHistoryDao {
    @Query("SELECT * FROM call_history ORDER BY firedAt DESC")
    fun getAllHistory(): Flow<List<CallHistoryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistoryItem(item: CallHistoryItem): Long

    @Query("DELETE FROM call_history WHERE id = :id")
    suspend fun deleteHistoryById(id: Long)

    @Query("DELETE FROM call_history")
    suspend fun clearAllHistory()
}
