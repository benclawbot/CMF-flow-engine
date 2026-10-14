package com.benclawbot.cmfflow.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.reflect.KClass

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
            runCatching {
                val records = readAll(client, HeartRateRecord::class, range)
                result(
                    name = "heart_rate",
                    records = records,
                    instants = records.flatMap { it.samples.map { sample -> sample.time } },
                    dataPointCount = records.sumOf { it.samples.size },
                    perRecordTimes = { record -> record.samples.map { sample -> sample.time } },
                )
            }.getOrElse { errorResult("heart_rate", it) },
            runCatching {
                val records = readAll(client, SleepSessionRecord::class, range)
                result("sleep", records, records.flatMap { listOf(it.startTime, it.endTime) },
                    perRecordTimes = { listOf(it.startTime, it.endTime) })
            }.getOrElse { errorResult("sleep", it) },
            runCatching {
                val records = readAll(client, StepsRecord::class, range)
                result("steps", records, records.flatMap { listOf(it.startTime, it.endTime) },
                    perRecordTimes = { listOf(it.startTime, it.endTime) })
            }.getOrElse { errorResult("steps", it) },
            runCatching {
                val records = readAll(client, OxygenSaturationRecord::class, range)
                result("oxygen_saturation", records, records.map { it.time },
                    perRecordTimes = { listOf(it.time) })
            }.getOrElse { errorResult("oxygen_saturation", it) },
            runCatching {
                val records = readAll(client, ExerciseSessionRecord::class, range)
                result("exercise", records, records.flatMap { listOf(it.startTime, it.endTime) },
                    perRecordTimes = { listOf(it.startTime, it.endTime) })
            }.getOrElse { errorResult("exercise", it) },
        )
    }

    private suspend fun <T : Record> readAll(
        client: HealthConnectClient,
        recordType: KClass<T>,
        range: TimeRangeFilter,
    ): List<T> {
        val records = mutableListOf<T>()
        var pageToken: String? = null
        do {
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = recordType,
                    timeRangeFilter = range,
                    pageToken = pageToken,
                ),
            )
            records += response.records
            pageToken = response.pageToken
        } while (pageToken != null)
        return records
    }

    private fun <T : Record> result(
        name: String,
        records: List<T>,
        instants: List<Instant>,
        dataPointCount: Int = records.size,
        perRecordTimes: (T) -> List<Instant>,
    ): ProbeResult {
        // Do not attribute a recent HR sample from another app to Nothing X.
        val latestByOrigin = records.groupBy { it.metadata.dataOrigin.packageName }
            .mapNotNull { (origin, entries) ->
                entries.flatMap(perRecordTimes).maxOrNull()?.toEpochMilli()?.let { origin to it }
            }
            .toMap()
        // Keep the actual exercise windows so a known watch workout can be matched
        // by start/end time, rather than inferring export from an arbitrary record.
        val sessions = records.filterIsInstance<ExerciseSessionRecord>().map {
            WatchExerciseSession(
                originPackage = it.metadata.dataOrigin.packageName,
                startEpochMs = it.startTime.toEpochMilli(),
                endEpochMs = it.endTime.toEpochMilli(),
            )
        }
        return ProbeResult(
            type = name,
            recordCount = records.size,
            dataPointCount = dataPointCount,
            origins = records.map { it.metadata.dataOrigin.packageName }.toSet(),
            earliestEpochMs = instants.minOrNull()?.toEpochMilli(),
            latestEpochMs = instants.maxOrNull()?.toEpochMilli(),
            error = null,
            latestByOriginEpochMs = latestByOrigin,
            exerciseSessions = sessions,
        )
    }

    private fun errorResult(name: String, throwable: Throwable) = ProbeResult(
        type = name,
        recordCount = 0,
        dataPointCount = 0,
        origins = emptySet(),
        earliestEpochMs = null,
        latestEpochMs = null,
        error = throwable.javaClass.simpleName + ": " + (throwable.message ?: "unknown"),
    )
}

data class ProbeResult(
    val type: String,
    val recordCount: Int,
    val dataPointCount: Int,
    val origins: Set<String>,
    val earliestEpochMs: Long?,
    val latestEpochMs: Long?,
    val error: String?,
    val latestByOriginEpochMs: Map<String, Long> = emptyMap(),
    val exerciseSessions: List<WatchExerciseSession> = emptyList(),
)

data class WatchExerciseSession(
    val originPackage: String,
    val startEpochMs: Long,
    val endEpochMs: Long,
)
