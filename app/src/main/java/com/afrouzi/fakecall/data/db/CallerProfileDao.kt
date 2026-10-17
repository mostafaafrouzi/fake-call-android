package com.afrouzi.fakecall.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.afrouzi.fakecall.data.model.CallerProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface CallerProfileDao {
    @Query("SELECT * FROM caller_profiles ORDER BY createdAt DESC")
    fun getAllProfiles(): Flow<List<CallerProfile>>

    @Query("SELECT * FROM caller_profiles WHERE id = :id")
    suspend fun getProfileById(id: Long): CallerProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: CallerProfile): Long

    @Update
    suspend fun updateProfile(profile: CallerProfile)

    @Delete
    suspend fun deleteProfile(profile: CallerProfile)

    @Query("DELETE FROM caller_profiles WHERE id = :id")
    suspend fun deleteProfileById(id: Long)
}
