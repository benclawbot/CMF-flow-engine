package com.benclawbot.cmfflow.ranking

import com.benclawbot.cmfflow.data.ContextSnapshotEntity
import com.benclawbot.cmfflow.data.SelfReportEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContextEvidenceTest {
    @Test
    fun sparseContextDoesNotAdjust() {
        val reports = (1L..4L).map { report(it, flow = 4) }
        val snapshots = (1L..4L).map { snapshot(it, sleep = 480) }
        val result = contextEvidenceFor(snapshots.first(), reports, snapshots)
        assertEquals(0.0, result.adjustment, 0.0001)
        assertTrue(result.reasons.isEmpty())
    }

    @Test
    fun contextAdjustmentIsBounded() {
        val reports = (1L..12L).map { id -> report(id, flow = if (id <= 6) 5 else 0) }
        val snapshots = (1L..12L).map { id ->
            snapshot(
                id = id,
                sleep = if (id <= 6) 500 else 250,
                hr = if (id <= 6) 70.0 else 100.0,
                steps = if (id <= 6) 500 else 0,
                charging = id <= 6,
                interactive = id <= 6,
            )
        }
        val result = contextEvidenceFor(snapshots.first(), reports, snapshots)
        assertTrue(result.adjustment <= 1.5)
        assertTrue(result.adjustment >= -1.5)
        assertTrue(result.reasons.any { it.startsWith("context_sleep_high_n=") })
    }

    private fun report(id: Long, flow: Int) = SelfReportEntity(
        id = id,
        capturedAtEpochMs = 1_700_000_000_000L + id,
        flowScore = flow,
        absorption = flow,
        effortlessControl = flow,
        intrinsicReward = flow,
        presence = flow,
        fatigue = 2,
        activityLabel = null,
        domain = "work",
        taskDifficulty = null,
        goalClarity = null,
        perceivedControl = null,
        notes = null,
    )

    private fun snapshot(
        id: Long,
        sleep: Long,
        hr: Double = 70.0,
        steps: Long = 500,
        charging: Boolean = true,
        interactive: Boolean = true,
    ) = ContextSnapshotEntity(
        id = id,
        selfReportId = id,
        capturedAtEpochMs = 1_700_000_000_000L + id,
        windowStartEpochMs = 0,
        windowEndEpochMs = 0,
        localHour = 10,
        localDayOfWeek = 1,
        batteryPercent = 80,
        isCharging = charging,
        isPhoneInteractive = interactive,
        heartRateRecordCount = 1,
        heartRateSampleCount = 1,
        heartRateMinBpm = hr,
        heartRateMaxBpm = hr,
        heartRateMeanBpm = hr,
        stepCount = steps,
        sleepMinutesPrevious24h = sleep,
        healthDataOrigins = "test",
        collectionError = null,
    )
}
