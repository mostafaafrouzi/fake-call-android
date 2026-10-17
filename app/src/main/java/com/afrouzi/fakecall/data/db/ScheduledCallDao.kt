package com.afrouzi.fakecall.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.afrouzi.fakecall.data.model.ScheduledCall
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduledCallDao {
    @Query("SELECT * FROM scheduled_calls ORDER BY triggerAtMillis ASC")
    fun getAllScheduledCalls(): Flow<List<ScheduledCall>>

    @Query("SELECT * FROM scheduled_calls WHERE triggerAtMillis > :now ORDER BY triggerAtMillis ASC")
    fun getFutureScheduledCalls(now: Long): Flow<List<ScheduledCall>>

    @Query("SELECT * FROM scheduled_calls WHERE triggerAtMillis > :now ORDER BY triggerAtMillis ASC LIMIT 1")
    fun getNextUpcomingCall(now: Long): Flow<ScheduledCall?>

    @Query("SELECT * FROM scheduled_calls WHERE triggerAtMillis > :now ORDER BY triggerAtMillis ASC")
    suspend fun getFutureScheduledCallsList(now: Long): List<ScheduledCall>

    @Query("SELECT * FROM scheduled_calls WHERE id = :id")
    suspend fun getScheduledCallById(id: Long): ScheduledCall?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheduledCall(call: ScheduledCall): Long

    @Query("DELETE FROM scheduled_calls WHERE id = :id")
    suspend fun deleteScheduledCallById(id: Long)

    @Query("DELETE FROM scheduled_calls")
    suspend fun deleteAllScheduledCalls()
}
