package com.benclawbot.cmfflow.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sessions",
    indices = [Index("taskId"), Index("startedAtEpochMs")],
)
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long?,
    val taskTitle: String?,
    val taskDomain: String?,
    val startedAtEpochMs: Long,
    val endedAtEpochMs: Long? = null,
    val struggleCount: Int = 0,
    val status: String = "active",
)
