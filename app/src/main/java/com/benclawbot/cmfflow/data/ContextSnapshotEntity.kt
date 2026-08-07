package com.benclawbot.cmfflow.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "context_snapshots",
    foreignKeys = [
        ForeignKey(
            entity = SelfReportEntity::class,
            parentColumns = ["id"],
            childColumns = ["selfReportId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("selfReportId")],
)
data class ContextSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val selfReportId: Long,
    val capturedAtEpochMs: Long,
    val windowStartEpochMs: Long,
    val windowEndEpochMs: Long,
    val localHour: Int,
    val localDayOfWeek: Int,
    val batteryPercent: Int?,
    val isCharging: Boolean?,
    val isPhoneInteractive: Boolean?,
    val usageAccessGranted: Boolean = false,
    val appSwitchCount: Int? = null,
    val unlockCount: Int? = null,
    val screenInteractiveTransitions: Int? = null,
    val notificationCount: Int? = null,
    val heartRateRecordCount: Int,
    val heartRateSampleCount: Int,
    val heartRateMinBpm: Double?,
    val heartRateMaxBpm: Double?,
    val heartRateMeanBpm: Double?,
    val stepCount: Long?,
    val sleepMinutesPrevious24h: Long?,
    val healthDataOrigins: String,
    val collectionError: String?,
)
