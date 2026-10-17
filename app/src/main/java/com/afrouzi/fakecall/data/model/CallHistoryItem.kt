package com.afrouzi.fakecall.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "call_history")
data class CallHistoryItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val callerName: String,
    val callerNumber: String,
    val scheduledFor: Long,
    val firedAt: Long,
    val outcome: CallOutcome,
    val durationSec: Int = 0
)
