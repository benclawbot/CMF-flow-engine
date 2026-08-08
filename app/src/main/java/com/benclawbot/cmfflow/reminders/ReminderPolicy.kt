package com.benclawbot.cmfflow.reminders

internal const val MINIMUM_CHECK_IN_GAP_MS: Long = 3L * 60L * 60L * 1000L

internal fun shouldSendCheckInReminder(
    lastCheckInEpochMs: Long?,
    nowEpochMs: Long,
): Boolean {
    if (lastCheckInEpochMs == null) return true
    if (nowEpochMs < lastCheckInEpochMs) return false
    return nowEpochMs - lastCheckInEpochMs >= MINIMUM_CHECK_IN_GAP_MS
}
