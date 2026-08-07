package com.benclawbot.cmfflow.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val domain: String,
    val valueScore: Int,
    val urgencyScore: Int,
    val difficultyScore: Int,
    val estimatedMinutes: Int,
    val status: String = "open",
    val dueAtEpochMs: Long? = null,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
)
