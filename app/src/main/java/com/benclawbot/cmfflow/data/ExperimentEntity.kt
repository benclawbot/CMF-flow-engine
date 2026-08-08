package com.benclawbot.cmfflow.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "experiments")
data class ExperimentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hypothesis: String,
    val conditionA: String,
    val conditionB: String,
    val primaryOutcome: String = "flow_presence_fatigue_utility",
    val status: String = "active",
    val createdAtEpochMs: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "experiment_assignments",
    indices = [Index("experimentId"), Index("outcomeSelfReportId")],
)
data class ExperimentAssignmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val experimentId: Long,
    val assignedCondition: String,
    val assignedAtEpochMs: Long = System.currentTimeMillis(),
    val completedAtEpochMs: Long? = null,
    val outcomeSelfReportId: Long? = null,
)
