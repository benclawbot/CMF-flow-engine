package com.benclawbot.cmfflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import com.benclawbot.cmfflow.data.SelfReportEntity
import com.benclawbot.cmfflow.health.HealthConnectProbe
import com.benclawbot.cmfflow.health.ProbeResult
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = (application as FlowApplication).database
        val probe = HealthConnectProbe(this)

        setContent {
            MaterialTheme {
                FlowHome(
                    probe = probe,
                    save = { database.selfReportDao().insert(it) },
                )
            }
        }
    }
}

@Composable
private fun FlowHome(
    probe: HealthConnectProbe,
    save: suspend (SelfReportEntity) -> Long,
) {
    val scope = rememberCoroutineScope()
    var flow by remember { mutableStateOf(3f) }
    var absorption by remember { mutableStateOf(3f) }
    var effortless by remember { mutableStateOf(3f) }
    var reward by remember { mutableStateOf(3f) }
    var presence by remember { mutableStateOf(3f) }
    var fatigue by remember { mutableStateOf(2f) }
    var status by remember { mutableStateOf("Ready") }
    var results by remember { mutableStateOf<List<ProbeResult>>(emptyList()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract(),
    ) { granted ->
        status = if (granted.containsAll(probe.permissions)) "Health permissions granted" else "Some health permissions were denied"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("CMF Flow Engine", style = MaterialTheme.typography.headlineMedium)
        Text("Ground truth first: quick subjective report, then context.")

        Score("Overall flow", flow) { flow = it }
        Score("Absorption", absorption) { absorption = it }
        Score("Effortless control", effortless) { effortless = it }
        Score("Intrinsic reward", reward) { reward = it }
        Score("Presence", presence) { presence = it }
        Score("Fatigue", fatigue) { fatigue = it }

        Button(
            onClick = {
                scope.launch {
                    save(
                        SelfReportEntity(
                            capturedAtEpochMs = System.currentTimeMillis(),
                            flowScore = flow.toInt(),
                            absorption = absorption.toInt(),
                            effortlessControl = effortless.toInt(),
                            intrinsicReward = reward.toInt(),
                            presence = presence.toInt(),
                            fatigue = fatigue.toInt(),
                            activityLabel = null,
                            domain = null,
                            taskDifficulty = null,
                            goalClarity = null,
                            perceivedControl = null,
                            notes = null,
                        ),
                    )
                    status = "Self-report saved locally"
                }
            },
        ) { Text("Save report") }

        Text("Health Connect probe", style = MaterialTheme.typography.titleLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { permissionLauncher.launch(probe.permissions) }) {
                Text("Grant access")
            }
            Button(onClick = {
                scope.launch {
                    status = "Probing last 7 days…"
                    results = probe.probe()
                    status = "Probe complete"
                }
            }) {
                Text("Run probe")
            }
        }

        Text(status)
        results.forEach { result ->
            val originText = if (result.origins.isEmpty()) "no origins" else result.origins.joinToString()
            Text("${result.type}: ${result.recordCount} records · $originText${result.error?.let { " · $it" } ?: ""}")
        }
    }
}

@Composable
private fun Score(label: String, value: Float, onValueChange: (Float) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("$label: ${value.toInt()}")
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..5f,
            steps = 4,
        )
    }
}
