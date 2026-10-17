package com.afrouzi.fakecall.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.afrouzi.fakecall.data.model.CallHistoryItem
import com.afrouzi.fakecall.data.model.CallerProfile
import com.afrouzi.fakecall.data.model.ScheduledCall

@Database(
    entities = [
        CallerProfile::class,
        ScheduledCall::class,
        CallHistoryItem::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class FakeCallDatabase : RoomDatabase() {

    abstract fun callerProfileDao(): CallerProfileDao
    abstract fun scheduledCallDao(): ScheduledCallDao
    abstract fun callHistoryDao(): CallHistoryDao

    companion object {
        private const val DATABASE_NAME = "fake_call.db"

        @Volatile
        private var instance: FakeCallDatabase? = null

        fun getInstance(context: Context): FakeCallDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    FakeCallDatabase::class.java,
                    DATABASE_NAME
                ).build().also { instance = it }
            }
        }
    }
}
