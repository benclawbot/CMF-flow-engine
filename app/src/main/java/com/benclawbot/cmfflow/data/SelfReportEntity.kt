package com.benclawbot.cmfflow.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "self_reports")
data class SelfReportEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val capturedAtEpochMs: Long,
    val flowScore: Int,
    val absorption: Int,
    val effortlessControl: Int,
    val intrinsicReward: Int,
    val presence: Int,
    val fatigue: Int,
    val activityLabel: String?,
    val domain: String?,
    val taskDifficulty: Int?,
    val goalClarity: Int?,
    val perceivedControl: Int?,
    val notes: String?,
)
