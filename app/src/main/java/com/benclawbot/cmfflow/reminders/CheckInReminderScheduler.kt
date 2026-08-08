package com.benclawbot.cmfflow.reminders

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object CheckInReminderScheduler {
    private const val workName = "flow-check-in-reminders"
    private const val preferencesName = "flow-reminder-state"
    private const val lastCheckInKey = "last-check-in-epoch-ms"

    fun enable(context: Context) {
        val request = PeriodicWorkRequestBuilder<CheckInReminderWorker>(4, TimeUnit.HOURS)
            .setInitialDelay(2, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            workName,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    fun disable(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(workName)
    }

    fun markCheckIn(context: Context, capturedAtEpochMs: Long) {
        context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
            .edit()
            .putLong(lastCheckInKey, capturedAtEpochMs)
            .apply()
    }

    fun shouldRemind(context: Context, nowEpochMs: Long = System.currentTimeMillis()): Boolean {
        val preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
        val lastCheckIn = if (preferences.contains(lastCheckInKey)) preferences.getLong(lastCheckInKey, 0L) else null
        return shouldSendCheckInReminder(lastCheckIn, nowEpochMs)
    }
}
