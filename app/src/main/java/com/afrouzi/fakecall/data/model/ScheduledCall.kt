package com.afrouzi.fakecall.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scheduled_calls")
data class ScheduledCall(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val profileId: Long? = null,
    val callerName: String,
    val callerNumber: String,
    val photoPath: String? = null,
    val voicePath: String? = null,
    val ringtoneUri: String? = null,
    val triggerAtMillis: Long,
    val createdAt: Long = System.currentTimeMillis()
)

fun CallerProfile.toScheduledCall(triggerAtMillis: Long): ScheduledCall {
    return ScheduledCall(
        profileId = if (this.id > 0) this.id else null,
        callerName = this.name,
        callerNumber = this.number,
        photoPath = this.photoPath,
        voicePath = this.voicePath,
        ringtoneUri = this.ringtoneUri,
        triggerAtMillis = triggerAtMillis,
        createdAt = System.currentTimeMillis()
    )
}
