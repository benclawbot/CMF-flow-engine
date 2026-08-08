package com.benclawbot.cmfflow.reminders

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderPolicyTest {
    @Test
    fun `reminds when no prior check-in exists`() {
        assertTrue(shouldSendCheckInReminder(null, nowEpochMs = 10_000L))
    }

    @Test
    fun `suppresses reminders inside minimum gap`() {
        val last = 1_000L
        assertFalse(shouldSendCheckInReminder(last, last + MINIMUM_CHECK_IN_GAP_MS - 1L))
    }

    @Test
    fun `reminds once minimum gap has elapsed`() {
        val last = 1_000L
        assertTrue(shouldSendCheckInReminder(last, last + MINIMUM_CHECK_IN_GAP_MS))
    }

    @Test
    fun `suppresses reminder if clock moved backwards`() {
        assertFalse(shouldSendCheckInReminder(lastCheckInEpochMs = 20_000L, nowEpochMs = 10_000L))
    }
}
