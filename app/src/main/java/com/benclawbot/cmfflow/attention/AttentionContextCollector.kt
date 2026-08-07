package com.benclawbot.cmfflow.attention

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process

data class AttentionContext(
    val usageAccessGranted: Boolean,
    val appSwitchCount: Int?,
    val unlockCount: Int?,
    val screenInteractiveTransitions: Int?,
    val notificationCount: Int?,
)

class AttentionContextCollector(private val context: Context) {
    fun collect(windowStartEpochMs: Long, windowEndEpochMs: Long): AttentionContext {
        val usageGranted = hasUsageAccess()
        val usage = if (usageGranted) collectUsage(windowStartEpochMs, windowEndEpochMs) else null
        return AttentionContext(
            usageAccessGranted = usageGranted,
            appSwitchCount = usage?.appSwitchCount,
            unlockCount = usage?.unlockCount,
            screenInteractiveTransitions = usage?.screenInteractiveTransitions,
            notificationCount = NotificationEventStore.countBetween(context, windowStartEpochMs, windowEndEpochMs),
        )
    }

    private fun hasUsageAccess(): Boolean {
        val appOps = context.getSystemService(AppOpsManager::class.java) ?: return false
        return appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
        ) == AppOpsManager.MODE_ALLOWED
    }

    private fun collectUsage(start: Long, end: Long): UsageAggregate {
        val manager = context.getSystemService(UsageStatsManager::class.java)
            ?: return UsageAggregate(0, 0, 0)
        val events = manager.queryEvents(start, end)
        val event = UsageEvents.Event()
        var lastForegroundPackage: String? = null
        var appSwitches = 0
        var unlocks = 0
        var screenTransitions = 0

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    val packageName = event.packageName
                    if (!packageName.isNullOrBlank() && packageName != context.packageName) {
                        if (lastForegroundPackage != null && lastForegroundPackage != packageName) appSwitches++
                        lastForegroundPackage = packageName
                    }
                }
                UsageEvents.Event.KEYGUARD_HIDDEN -> unlocks++
                UsageEvents.Event.SCREEN_INTERACTIVE,
                UsageEvents.Event.SCREEN_NON_INTERACTIVE -> screenTransitions++
            }
        }
        return UsageAggregate(appSwitches, unlocks, screenTransitions)
    }

    private data class UsageAggregate(
        val appSwitchCount: Int,
        val unlockCount: Int,
        val screenInteractiveTransitions: Int,
    )
}
