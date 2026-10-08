package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ai_tools")
data class AiTool(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String,
    val systemPrompt: String,
    val inputPlaceholder: String,
    val iconName: String,
    val colorHex: String,
    val isPreset: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
