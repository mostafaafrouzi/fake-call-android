package com.afrouzi.fakecall.data.repository

import com.afrouzi.fakecall.data.db.ScheduledCallDao
import com.afrouzi.fakecall.data.model.ScheduledCall
import kotlinx.coroutines.flow.Flow

class ScheduledCallRepository(
    private val dao: ScheduledCallDao
) {
    val allScheduledCalls: Flow<List<ScheduledCall>> = dao.getAllScheduledCalls()

    fun getFutureScheduledCalls(now: Long = System.currentTimeMillis()): Flow<List<ScheduledCall>> {
        return dao.getFutureScheduledCalls(now)
    }

    fun getNextUpcomingCall(now: Long = System.currentTimeMillis()): Flow<ScheduledCall?> {
        return dao.getNextUpcomingCall(now)
    }

    suspend fun getScheduledCall(id: Long): ScheduledCall? {
        return dao.getScheduledCallById(id)
    }

    suspend fun getFutureScheduledCallsList(now: Long = System.currentTimeMillis()): List<ScheduledCall> {
        return dao.getFutureScheduledCallsList(now)
    }

    suspend fun scheduleCall(call: ScheduledCall): Long {
        return dao.insertScheduledCall(call)
    }

    suspend fun deleteScheduledCall(id: Long) {
        dao.deleteScheduledCallById(id)
    }

    suspend fun deleteAll() {
        dao.deleteAllScheduledCalls()
    }
}
