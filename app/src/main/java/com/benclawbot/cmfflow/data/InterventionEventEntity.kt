package com.benclawbot.cmfflow.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "intervention_events",
    indices = [Index("outcomeSelfReportId")],
)
data class InterventionEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val action: String,
    val presentedAtEpochMs: Long = System.currentTimeMillis(),
    val reasonsSnapshot: String,
    val response: String? = null,
    val respondedAtEpochMs: Long? = null,
    val outcomeSelfReportId: Long? = null,
)
