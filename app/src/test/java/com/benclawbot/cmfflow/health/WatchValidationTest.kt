package com.benclawbot.cmfflow.health

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WatchValidationTest {
    private fun result(
        type: String,
        latestByOrigin: Map<String, Long> = emptyMap(),
        sessions: List<WatchExerciseSession> = emptyList(),
    ) = ProbeResult(
        type = type,
        recordCount = 1,
        dataPointCount = 1,
        origins = latestByOrigin.keys + sessions.map { it.originPackage },
        earliestEpochMs = null,
        latestEpochMs = null,
        error = null,
        latestByOriginEpochMs = latestByOrigin,
        exerciseSessions = sessions,
    )

    @Test fun recencyUsesOnlyNothingXSamplesNotOtherApps() {
        val now = 1_800_000_000_000L
        val sample = result("heart_rate", mapOf(
            "com.example.other" to now - 20_000L,
            WatchValidation.NOTHING_X to now - 600_000L,
        ))
        assertEquals(10L, WatchValidation.heartRateRecencyMinutes(sample, now))
        assertNull(WatchValidation.heartRateRecencyMinutes(
            result("heart_rate", mapOf("com.example.other" to now)), now))
    }

    @Test fun recencyRejectsFutureClockSkewAndWrongSignal() {
        val now = 1_800_000_000_000L
        assertNull(WatchValidation.heartRateRecencyMinutes(
            result("heart_rate", mapOf(WatchValidation.NOTHING_X to now + 60_000L)), now))
        assertNull(WatchValidation.heartRateRecencyMinutes(
            result("steps", mapOf(WatchValidation.NOTHING_X to now)), now))
    }

    @Test fun knownWorkoutRequiresMatchingNothingXStartAndEnd() {
        val begin = 1_800_000_000_000L
        val end = begin + 900_000L
        val wrongSource = WatchExerciseSession("com.example.other", begin, end)
        val unmatched = WatchExerciseSession(WatchValidation.NOTHING_X, begin - 3_600_000L, end)
        val matching = WatchExerciseSession(WatchValidation.NOTHING_X, begin + 60_000L, end - 30_000L)
        assertFalse(WatchValidation.matchesKnownWorkout(
            result("exercise", sessions = listOf(wrongSource, unmatched)), begin, end))
        assertTrue(WatchValidation.matchesKnownWorkout(
            result("exercise", sessions = listOf(wrongSource, matching)), begin, end))
        assertFalse(WatchValidation.matchesKnownWorkout(
            result("sleep", sessions = listOf(matching)), begin, end))
    }
}
