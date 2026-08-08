package com.benclawbot.cmfflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.benclawbot.cmfflow.ui.FlowTheme

class RationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FlowTheme {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text("Your health data stays yours", style = MaterialTheme.typography.headlineMedium)
                    Text("CMF Flow uses selected Health Connect signals to understand the context around your focus, energy, and fatigue.")
                    Text("This can include heart rate, sleep, steps, blood oxygen, and exercise. Health data is processed locally and is not uploaded.")
                    Text("You stay in control: every permission can be granted, denied, or revoked from Android settings at any time.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
