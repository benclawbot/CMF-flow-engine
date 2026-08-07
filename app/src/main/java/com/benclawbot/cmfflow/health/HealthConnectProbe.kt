package com.benclawbot.cmfflow.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.temporal.ChronoUnit

class HealthConnectProbe(private val context: Context) {
    val permissions: Set<String> = setOf(
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(OxygenSaturationRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
    )

    fun sdkStatus(): Int = HealthConnectClient.getSdkStatus(context)

    suspend fun probe(): List<ProbeResult> {
        if (sdkStatus() != HealthConnectClient.SDK_AVAILABLE) return emptyList()
        val client = HealthConnectClient.getOrCreate(context)
        val end = Instant.now()
        val start = end.minus(7, ChronoUnit.DAYS)
        val range = TimeRangeFilter.between(start, end)

        return listOf(
            probeType("heart_rate") { client.readRecords(ReadRecordsRequest(HeartRateRecord::class, range)).records },
            probeType("sleep") { client.readRecords(ReadRecordsRequest(SleepSessionRecord::class, range)).records },
            probeType("steps") { client.readRecords(ReadRecordsRequest(StepsRecord::class, range)).records },
            probeType("oxygen_saturation") { client.readRecords(ReadRecordsRequest(OxygenSaturationRecord::class, range)).records },
            probeType("exercise") { client.readRecords(ReadRecordsRequest(ExerciseSessionRecord::class, range)).records },
        )
    }

    private suspend fun <T : androidx.health.connect.client.records.Record> probeType(
        name: String,
        read: suspend () -> List<T>,
    ): ProbeResult = runCatching {
        val records = read()
        ProbeResult(
            type = name,
            recordCount = records.size,
            origins = records.map { it.metadata.dataOrigin.packageName }.toSet(),
            error = null,
        )
    }.getOrElse {
        ProbeResult(name, 0, emptySet(), it.javaClass.simpleName + ": " + (it.message ?: "unknown"))
    }
}

data class ProbeResult(
    val type: String,
    val recordCount: Int,
    val origins: Set<String>,
    val error: String?,
)
