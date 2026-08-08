package com.benclawbot.cmfflow

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.benclawbot.cmfflow.attention.AttentionAccess
import com.benclawbot.cmfflow.data.ContextSnapshotEntity

@Composable
fun AttentionAccessPanel(latestContext: ContextSnapshotEntity?) {
    val context = LocalContext.current
    val usageGranted = AttentionAccess.hasUsageAccess(context)
    val notificationGranted = AttentionAccess.hasNotificationListenerAccess(context)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Attention sensing", style = MaterialTheme.typography.titleLarge)
        Text("Only local aggregate counts are retained; app names and notification content are not stored.")
        Text("Usage access: ${if (usageGranted) "enabled" else "disabled"}")
        Text("Notification listener: ${if (notificationGranted) "enabled" else "disabled"}")
        latestContext?.let { snapshot ->
            Text(
                "Latest window: switches ${snapshot.appSwitchCount ?: "n/a"} · " +
                    "unlocks ${snapshot.unlockCount ?: "n/a"} · " +
                    "screen transitions ${snapshot.screenInteractiveTransitions ?: "n/a"} · " +
                    "notifications ${snapshot.notificationCount ?: "n/a"}",
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!usageGranted) {
                Button(onClick = {
                    context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }) { Text("Enable usage access") }
            }
            if (!notificationGranted) {
                Button(onClick = {
                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }) { Text("Enable notification access") }
            }
        }
        if (usageGranted && notificationGranted) {
            Text("Attention sensing is fully enabled.")
        } else {
            Text("Missing access stays explicitly unavailable and is never imputed as zero activity.")
        }
    }
}
