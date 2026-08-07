package com.benclawbot.cmfflow.attention

import android.content.Context

object NotificationEventStore {
    private const val PREFS = "attention_notifications"
    private const val KEY_TIMESTAMPS = "timestamps"
    private const val MAX_EVENTS = 512

    fun record(context: Context, epochMs: Long) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val updated = (read(prefs.getString(KEY_TIMESTAMPS, null)) + epochMs)
            .takeLast(MAX_EVENTS)
        prefs.edit().putString(KEY_TIMESTAMPS, updated.joinToString(",")).apply()
    }

    fun countBetween(context: Context, startEpochMs: Long, endEpochMs: Long): Int? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.contains(KEY_TIMESTAMPS)) return null
        return read(prefs.getString(KEY_TIMESTAMPS, null)).count { it in startEpochMs..endEpochMs }
    }

    private fun read(raw: String?): List<Long> = raw
        ?.split(',')
        ?.mapNotNull(String::toLongOrNull)
        ?: emptyList()
}
