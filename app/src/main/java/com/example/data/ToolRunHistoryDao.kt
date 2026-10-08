package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ToolRunHistoryDao {
    @Query("SELECT * FROM tool_run_history WHERE toolId = :toolId ORDER BY timestamp DESC")
    fun getHistoryForTool(toolId: Long): Flow<List<ToolRunHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: ToolRunHistory): Long

    @Query("DELETE FROM tool_run_history WHERE toolId = :toolId")
    suspend fun deleteHistoryForTool(toolId: Long)

    @Query("DELETE FROM tool_run_history WHERE id = :id")
    suspend fun deleteHistoryItem(id: Long)
}
