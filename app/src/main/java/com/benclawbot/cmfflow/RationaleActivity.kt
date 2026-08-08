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

class RationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text("CMF Flow Engine health data", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "CMF Flow Engine reads selected Health Connect data only on this device to learn which contexts are associated with your self-reported flow and fatigue.",
                    )
                    Text(
                        "Requested read access may include heart rate, sleep, steps, oxygen saturation, and exercise. The app does not upload Health Connect data.",
                    )
                    Text(
                        "You can grant, deny, or revoke each Health Connect permission at any time in Android settings.",
                    )
                }
            }
        }
    }
}
