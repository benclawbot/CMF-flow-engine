package com.benclawbot.cmfflow.health

import android.content.Context
import android.os.BatteryManager
import android.os.PowerManager
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.benclawbot.cmfflow.data.ContextSnapshotEntity
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class HealthContextCollector(private val context: Context) {
    suspend fun collect(selfReportId: Long, capturedAtEpochMs: Long): ContextSnapshotEntity {
        val center = Instant.ofEpochMilli(capturedAtEpochMs)
        val windowStart = center.minus(30, ChronoUnit.MINUTES)
        val windowEnd = center.plus(5, ChronoUnit.MINUTES)
        val sleepStart = center.minus(24, ChronoUnit.HOURS)
        val local = center.atZone(ZoneId.systemDefault())
        val batteryManager = context.getSystemService(BatteryManager::class.java)
        val powerManager = context.getSystemService(PowerManager::class.java)
        val batteryPercent = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)?.takeIf { it in 0..100 }
        val charging = batteryManager?.isCharging
        val interactive = powerManager?.isInteractive

        if (HealthConnectClient.getSdkStatus(context) != HealthConnectClient.SDK_AVAILABLE) {
            return emptySnapshot(
                selfReportId,
                capturedAtEpochMs,
                windowStart,
                windowEnd,
                local.hour,
                local.dayOfWeek.value,
                batteryPercent,
                charging,
                interactive,
                "Health Connect unavailable",
            )
        }

        return runCatching {
            val client = HealthConnectClient.getOrCreate(context)
            val contextRange = TimeRangeFilter.between(windowStart, windowEnd)
            val sleepRange = TimeRangeFilter.between(sleepStart, center)

            val heartRate = client.readRecords(ReadRecordsRequest(HeartRateRecord::class, contextRange)).records
            val steps = client.readRecords(ReadRecordsRequest(StepsRecord::class, contextRange)).records
            val sleep = client.readRecords(ReadRecordsRequest(SleepSessionRecord::class, sleepRange)).records

            val hrSamples = heartRate.flatMap { it.samples }
            val bpms = hrSamples.map { it.beatsPerMinute.toDouble() }
            val origins = buildSet {
                heartRate.mapTo(this) { it.metadata.dataOrigin.packageName }
                steps.mapTo(this) { it.metadata.dataOrigin.packageName }
                sleep.mapTo(this) { it.metadata.dataOrigin.packageName }
            }
                .filter { it.isNotBlank() }
                .toSortedSet()
                .joinToString(",")

            ContextSnapshotEntity(
                selfReportId = selfReportId,
                capturedAtEpochMs = capturedAtEpochMs,
                windowStartEpochMs = windowStart.toEpochMilli(),
                windowEndEpochMs = windowEnd.toEpochMilli(),
                localHour = local.hour,
                localDayOfWeek = local.dayOfWeek.value,
                batteryPercent = batteryPercent,
                isCharging = charging,
                isPhoneInteractive = interactive,
                heartRateRecordCount = heartRate.size,
                heartRateSampleCount = hrSamples.size,
                heartRateMinBpm = bpms.minOrNull(),
                heartRateMaxBpm = bpms.maxOrNull(),
                heartRateMeanBpm = bpms.takeIf { it.isNotEmpty() }?.average(),
                stepCount = steps.sumOf { it.count },
                sleepMinutesPrevious24h = sleep.sumOf { session ->
                    ChronoUnit.MINUTES.between(session.startTime, session.endTime).coerceAtLeast(0)
                },
                healthDataOrigins = origins,
                collectionError = null,
            )
        }.getOrElse { error ->
            emptySnapshot(
                selfReportId,
                capturedAtEpochMs,
                windowStart,
                windowEnd,
                local.hour,
                local.dayOfWeek.value,
                batteryPercent,
                charging,
                interactive,
                error.javaClass.simpleName + ": " + (error.message ?: "unknown"),
            )
        }
    }

    private fun emptySnapshot(
        selfReportId: Long,
        capturedAtEpochMs: Long,
        windowStart: Instant,
        windowEnd: Instant,
        localHour: Int,
        localDayOfWeek: Int,
        batteryPercent: Int?,
        isCharging: Boolean?,
        isPhoneInteractive: Boolean?,
        error: String,
    ) = ContextSnapshotEntity(
        selfReportId = selfReportId,
        capturedAtEpochMs = capturedAtEpochMs,
        windowStartEpochMs = windowStart.toEpochMilli(),
        windowEndEpochMs = windowEnd.toEpochMilli(),
        localHour = localHour,
        localDayOfWeek = localDayOfWeek,
        batteryPercent = batteryPercent,
        isCharging = isCharging,
        isPhoneInteractive = isPhoneInteractive,
        heartRateRecordCount = 0,
        heartRateSampleCount = 0,
        heartRateMinBpm = null,
        heartRateMaxBpm = null,
        heartRateMeanBpm = null,
        stepCount = null,
        sleepMinutesPrevious24h = null,
        healthDataOrigins = "",
        collectionError = error,
    )
}
