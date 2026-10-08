package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tool_run_history")
data class ToolRunHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val toolId: Long,
    val input: String,
    val output: String,
    val timestamp: Long = System.currentTimeMillis()
)
