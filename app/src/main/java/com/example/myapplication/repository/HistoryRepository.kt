package com.example.myapplication.repository

import android.content.Context
import com.example.myapplication.model.HistoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * Modern Room-backed history repository.
 */
class HistoryRepository(context: Context) {

    private val historyDao = AppDatabase.getDatabase(context).historyDao()

    fun getAllFlow(): Flow<List<HistoryEntity>> = historyDao.getAllFlow()

    suspend fun save(expression: String, result: String, degreeMode: Boolean) {
        if (expression.isBlank()) return
        val entry = HistoryEntity(
            expression = expression,
            result = result,
            degreeMode = degreeMode
        )
        historyDao.insert(entry)
        historyDao.trimHistory() // Keep only top 100
    }

    suspend fun clearAll() {
        historyDao.deleteAll()
    }
}
