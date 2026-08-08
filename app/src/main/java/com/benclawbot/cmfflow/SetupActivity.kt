package com.benclawbot.cmfflow

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.benclawbot.cmfflow.attention.AttentionAccess
import com.benclawbot.cmfflow.ui.FlowTheme

class SetupActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FlowTheme {
                AttentionSetup(
                    onContinue = {
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    },
                )
            }
        }
    }
}

@Composable
private fun AttentionSetup(onContinue: () -> Unit) {
    val context = LocalContext.current
    val usageGranted = AttentionAccess.hasUsageAccess(context)
    val notificationGranted = AttentionAccess.hasNotificationListenerAccess(context)

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 30.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text("Welcome to CMF Flow", style = MaterialTheme.typography.headlineMedium)
        Text(
            "A private assistant that learns the conditions where you focus best.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        PermissionCard(
            title = "Usage access",
            description = "Helps estimate context switching. App names are not stored.",
            enabled = usageGranted,
            onEnable = { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) },
        )
        PermissionCard(
            title = "Notification access",
            description = "Counts interruptions only. Notification content is never stored.",
            enabled = notificationGranted,
            onEnable = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
        )

        Text(
            "Both are optional. CMF Flow works without them and keeps your data on this device.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
            Text("Continue")
        }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    description: String,
    enabled: Boolean,
    onEnable: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                if (enabled) "Enabled" else description,
                color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!enabled) {
                OutlinedButton(onClick = onEnable) { Text("Enable") }
            }
        }
    }
}
