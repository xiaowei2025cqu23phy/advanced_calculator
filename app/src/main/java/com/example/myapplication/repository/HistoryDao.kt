package com.example.myapplication.repository

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.myapplication.model.HistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM calculation_history ORDER BY timestamp DESC")
    fun getAllFlow(): Flow<List<HistoryEntity>>

    @Insert
    suspend fun insert(entry: HistoryEntity)

    @Query("DELETE FROM calculation_history")
    suspend fun deleteAll()

    @Query("DELETE FROM calculation_history WHERE id NOT IN (SELECT id FROM calculation_history ORDER BY timestamp DESC LIMIT 100)")
    suspend fun trimHistory()
}
