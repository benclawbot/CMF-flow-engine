package com.benclawbot.cmfflow.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.benclawbot.cmfflow.ProductActivity
import com.benclawbot.cmfflow.R
import java.time.ZonedDateTime

class CheckInReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : Worker(appContext, params) {
    override fun doWork(): Result {
        val hour = ZonedDateTime.now().hour
        if (hour !in 8..21) return Result.success()
        if (!CheckInReminderScheduler.shouldRemind(applicationContext)) return Result.success()

        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        val channelId = "flow_check_ins"
        manager.createNotificationChannel(
            NotificationChannel(
                channelId,
                "Flow check-ins",
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )

        val intent = Intent(applicationContext, ProductActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_flow_foreground)
            .setContentTitle("Has your state changed?")
            .setContentText("If it has, a quick check-in helps Flow learn without asking for data it already has.")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        manager.notify(1001, notification)
        return Result.success()
    }
}
