package com.afrouzi.fakecall.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "caller_profiles")
data class CallerProfile(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val number: String,
    val photoPath: String? = null,
    val voicePath: String? = null,
    val ringtoneUri: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
