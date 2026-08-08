package com.benclawbot.cmfflow

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.benclawbot.cmfflow.attention.AttentionAccess

class SetupActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
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
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("CMF Flow Engine setup", style = MaterialTheme.typography.headlineMedium)
        Text("Attention sensing is optional. The engine stores aggregate counts only—never app names, notification titles, message text, or notification payloads.")

        Text("Usage access: ${if (usageGranted) "enabled" else "disabled"}")
        if (!usageGranted) {
            Button(onClick = {
                context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            }) { Text("Enable usage access") }
        }

        Text("Notification listener: ${if (notificationGranted) "enabled" else "disabled"}")
        if (!notificationGranted) {
            Button(onClick = {
                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }) { Text("Enable notification access") }
        }

        Text("You can continue without either permission. Missing signals stay unavailable rather than being treated as zero fragmentation.")
        Button(onClick = onContinue) { Text("Continue to Flow Engine") }
    }
}
