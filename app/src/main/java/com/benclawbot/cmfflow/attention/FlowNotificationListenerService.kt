package com.benclawbot.cmfflow.attention

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class FlowNotificationListenerService : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null || sbn.packageName == packageName) return
        NotificationEventStore.record(this, sbn.postTime)
    }
}
