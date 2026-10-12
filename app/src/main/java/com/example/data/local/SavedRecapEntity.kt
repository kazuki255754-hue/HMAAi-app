package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_recaps")
data class SavedRecapEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val titleOrQuery: String,
    val script: String,
    val toneName: String,
    val toneIcon: String,
    val toneDisplayName: String,
    val isFromUrl: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val characterCount: Int = script.length,
    val wordCount: Int = if (script.isBlank()) 0 else script.trim().split("\\s+".toRegex()).size
)
