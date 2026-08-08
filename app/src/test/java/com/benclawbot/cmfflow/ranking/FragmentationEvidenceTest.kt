package com.benclawbot.cmfflow.ranking

import com.benclawbot.cmfflow.data.ContextSnapshotEntity
import com.benclawbot.cmfflow.data.SelfReportEntity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FragmentationEvidenceTest {
    @Test
    fun sparseHistoryDoesNotClaimHarmOrCurrentHigh() {
        val reports = (1L..6L).map { report(it, 4, 4, 2) }
        val snapshots = (1L..6L).map { snapshot(it, switches = it.toInt()) }

        val result = fragmentationEvidenceFor(snapshots.last(), reports, snapshots)

        assertFalse(result.harmfulAssociation)
        assertFalse(result.currentlyHigh)
        assertTrue(result.adjustment == 0.0)
    }

    @Test
    fun highFragmentationCanBecomeLearnedNegativeEvidence() {
        val reports = (1L..12L).map { id ->
            if (id <= 6) report(id, 5, 5, 1) else report(id, 1, 1, 4)
        }
        val snapshots = (1L..12L).map { id ->
            snapshot(id, switches = if (id <= 6) 1 else 12, notifications = if (id <= 6) 1 else 10)
        }

        val result = fragmentationEvidenceFor(snapshots.last(), reports, snapshots)

        assertTrue(result.harmfulAssociation)
        assertTrue(result.currentlyHigh)
        assertTrue(result.adjustment < 0.0)
        assertTrue(result.adjustment >= -0.8)
    }

    private fun report(id: Long, flow: Int, presence: Int, fatigue: Int) = SelfReportEntity(
        id = id,
        capturedAtEpochMs = 1_700_000_000_000L + id,
        flowScore = flow,
        absorption = flow,
        effortlessControl = flow,
        intrinsicReward = flow,
        presence = presence,
        fatigue = fatigue,
        activityLabel = null,
        domain = "work",
        taskDifficulty = null,
        goalClarity = null,
        perceivedControl = null,
        notes = null,
    )

    private fun snapshot(id: Long, switches: Int, notifications: Int = 1) = ContextSnapshotEntity(
        id = id,
        selfReportId = id,
        capturedAtEpochMs = 1_700_000_000_000L + id,
        windowStartEpochMs = 0,
        windowEndEpochMs = 0,
        localHour = 10,
        localDayOfWeek = 1,
        batteryPercent = 80,
        isCharging = false,
        isPhoneInteractive = true,
        usageAccessGranted = true,
        appSwitchCount = switches,
        unlockCount = 1,
        screenInteractiveTransitions = 1,
        notificationCount = notifications,
        heartRateRecordCount = 0,
        heartRateSampleCount = 0,
        heartRateMinBpm = null,
        heartRateMaxBpm = null,
        heartRateMeanBpm = null,
        stepCount = null,
        sleepMinutesPrevious24h = null,
        healthDataOrigins = "",
        collectionError = null,
    )
}
