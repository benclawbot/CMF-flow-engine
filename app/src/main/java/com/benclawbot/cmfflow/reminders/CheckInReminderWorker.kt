package com.benclawbot.cmfflow.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.benclawbot.cmfflow.MainActivity
import java.time.ZonedDateTime

class CheckInReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : Worker(appContext, params) {
    override fun doWork(): Result {
        val hour = ZonedDateTime.now().hour
        if (hour !in 8..21) return Result.success()

        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        val channelId = "flow_check_ins"
        manager.createNotificationChannel(
            NotificationChannel(
                channelId,
                "Flow check-ins",
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )

        val intent = Intent(applicationContext, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("How is your flow right now?")
            .setContentText("A quick check-in helps the engine learn your patterns.")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        manager.notify(1001, notification)
        return Result.success()
    }
}
