package com.benclawbot.cmfflow.reminders

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object CheckInReminderScheduler {
    private const val workName = "flow-check-in-reminders"

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
}
