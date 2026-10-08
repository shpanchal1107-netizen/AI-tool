package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AiToolDao {
    @Query("SELECT * FROM ai_tools ORDER BY isPreset DESC, createdAt DESC")
    fun getAllTools(): Flow<List<AiTool>>

    @Query("SELECT * FROM ai_tools WHERE id = :id")
    suspend fun getToolById(id: Long): AiTool?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTool(tool: AiTool): Long

    @Update
    suspend fun updateTool(tool: AiTool)

    @Delete
    suspend fun deleteTool(tool: AiTool)

    @Query("SELECT COUNT(*) FROM ai_tools")
    suspend fun getToolsCount(): Int
}
