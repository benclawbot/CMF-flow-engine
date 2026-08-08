package com.benclawbot.cmfflow

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.benclawbot.cmfflow.attention.AttentionAccess
import com.benclawbot.cmfflow.ui.FlowTheme

class SetupActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val preferences = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE)
        if (preferences.getBoolean(KEY_ONBOARDING_COMPLETE, false)) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContent {
            FlowTheme {
                AttentionSetup {
                    preferences.edit().putBoolean(KEY_ONBOARDING_COMPLETE, true).apply()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
            }
        }
    }

    companion object {
        private const val PREFERENCES_NAME = "cmf_flow_preferences"
        private const val KEY_ONBOARDING_COMPLETE = "onboarding_complete_v1"
    }
}

@Composable
private fun AttentionSetup(onContinue: () -> Unit) {
    val context = LocalContext.current
    val usageGranted = AttentionAccess.hasUsageAccess(context)
    val notificationGranted = AttentionAccess.hasNotificationListenerAccess(context)
    val gradient = Brush.linearGradient(listOf(Color(0xFF5D3BE8), Color(0xFF9E85FF)))

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(horizontal = 22.dp, vertical = 30.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Surface(shape = RoundedCornerShape(32.dp), color = Color.Transparent, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.background(gradient).padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Surface(shape = CircleShape, color = Color.White.copy(alpha = .16f), modifier = Modifier.size(64.dp)) {
                    Box(contentAlignment = Alignment.Center) { Text("✦", color = Color.White, style = MaterialTheme.typography.headlineMedium) }
                }
                Text("Welcome to CMF Flow", color = Color.White, style = MaterialTheme.typography.headlineLarge)
                Text("A private assistant that learns the conditions where you focus best.", color = Color.White.copy(alpha = .82f), style = MaterialTheme.typography.bodyLarge)
            }
        }

        Text("Optional sensing", style = MaterialTheme.typography.titleLarge)
        Text("You stay in control. These permissions improve context awareness but are not required.", color = MaterialTheme.colorScheme.onSurfaceVariant)

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

        Spacer(Modifier.weight(1f))
        Text("Local-first · No cloud account · No notification content stored", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) { Text("Continue to CMF Flow") }
    }
}

@Composable
private fun PermissionCard(title: String, description: String, enabled: Boolean, onEnable: () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = if (enabled) Color(0xFFE5F4EC) else MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(46.dp)) {
                Box(contentAlignment = Alignment.Center) { Text(if (enabled) "✓" else "○", color = if (enabled) Color(0xFF238257) else MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) }
            }
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(if (enabled) "Enabled" else description, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            if (!enabled) OutlinedButton(onClick = onEnable) { Text("Enable") }
        }
    }
}
