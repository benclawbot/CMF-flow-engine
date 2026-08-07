package com.benclawbot.cmfflow.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "recommendation_events",
    indices = [Index("taskId"), Index("outcomeSelfReportId")],
)
data class RecommendationEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long,
    val taskTitle: String,
    val taskDomain: String,
    val presentedAtEpochMs: Long = System.currentTimeMillis(),
    val score: Double,
    val reasonsSnapshot: String,
    val response: String? = null,
    val respondedAtEpochMs: Long? = null,
    val outcomeSelfReportId: Long? = null,
)
