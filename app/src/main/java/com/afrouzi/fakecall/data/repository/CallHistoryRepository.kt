package com.afrouzi.fakecall.data.repository

import com.afrouzi.fakecall.data.db.CallHistoryDao
import com.afrouzi.fakecall.data.model.CallHistoryItem
import kotlinx.coroutines.flow.Flow

class CallHistoryRepository(
    private val dao: CallHistoryDao
) {
    val history: Flow<List<CallHistoryItem>> = dao.getAllHistory()

    suspend fun addHistory(item: CallHistoryItem): Long {
        return dao.insertHistoryItem(item)
    }

    suspend fun deleteHistory(id: Long) {
        dao.deleteHistoryById(id)
    }

    suspend fun clearHistory() {
        dao.clearAllHistory()
    }
}
