package com.benclawbot.cmfflow.interventions

import com.benclawbot.cmfflow.data.SessionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionStateTest {
    @Test
    fun derivesElapsedMinutesAndRepeatedStruggle() {
        val session = SessionEntity(
            id = 1,
            taskId = 10,
            taskTitle = "Deep work",
            taskDomain = "work",
            startedAtEpochMs = 1_000L,
            struggleCount = 2,
        )

        val signals = sessionSignals(session, nowEpochMs = 1_000L + 52L * 60_000L)

        assertEquals(52, signals.minutesOnCurrentTask)
        assertTrue(signals.repeatedStruggle)
    }

    @Test
    fun endedSessionDoesNotDrivePolicy() {
        val session = SessionEntity(
            id = 1,
            taskId = null,
            taskTitle = null,
            taskDomain = null,
            startedAtEpochMs = 1_000L,
            endedAtEpochMs = 2_000L,
            struggleCount = 3,
            status = "ended",
        )

        val signals = sessionSignals(session, nowEpochMs = 5_000L)

        assertEquals(null, signals.minutesOnCurrentTask)
        assertFalse(signals.repeatedStruggle)
    }
}
