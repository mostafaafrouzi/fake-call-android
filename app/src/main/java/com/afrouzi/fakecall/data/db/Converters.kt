package com.afrouzi.fakecall.data.db

import androidx.room.TypeConverter
import com.afrouzi.fakecall.data.model.CallOutcome

class Converters {
    @TypeConverter
    fun fromCallOutcome(outcome: CallOutcome?): String {
        return outcome?.name ?: CallOutcome.MISSED.name
    }

    @TypeConverter
    fun toCallOutcome(value: String?): CallOutcome {
        if (value == null) return CallOutcome.MISSED
        return runCatching {
            CallOutcome.valueOf(value)
        }.getOrDefault(CallOutcome.MISSED)
    }
}
