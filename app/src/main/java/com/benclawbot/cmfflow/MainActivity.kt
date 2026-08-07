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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
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
import com.benclawbot.cmfflow.health.HealthContextCollector
import com.benclawbot.cmfflow.health.ProbeResult
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = (application as FlowApplication).database
        val probe = HealthConnectProbe(this)
        val contextCollector = HealthContextCollector(this)

        setContent {
            MaterialTheme {
                FlowHome(
                    probe = probe,
                    save = { report ->
                        val reportId = database.selfReportDao().insert(report)
                        val snapshot = contextCollector.collect(reportId, report.capturedAtEpochMs)
                        database.contextSnapshotDao().insert(snapshot)
                        reportId
                    },
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
    var activity by remember { mutableStateOf("") }
    var domain by remember { mutableStateOf("") }
    var advanced by remember { mutableStateOf(false) }
    var difficulty by remember { mutableStateOf(3f) }
    var goalClarity by remember { mutableStateOf(3f) }
    var perceivedControl by remember { mutableStateOf(3f) }
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
        Text("Quick subjective label first. Context is optional and health context is captured automatically.")

        Score("Overall flow", flow) { flow = it }
        Score("Absorption", absorption) { absorption = it }
        Score("Effortless control", effortless) { effortless = it }
        Score("Intrinsic reward", reward) { reward = it }
        Score("Presence", presence) { presence = it }
        Score("Fatigue", fatigue) { fatigue = it }

        OutlinedTextField(
            value = activity,
            onValueChange = { activity = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Activity (optional)") },
            singleLine = true,
        )
        OutlinedTextField(
            value = domain,
            onValueChange = { domain = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Domain (optional, e.g. work, cooking, family)") },
            singleLine = true,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Switch(checked = advanced, onCheckedChange = { advanced = it })
            Text("Add antecedent context")
        }
        if (advanced) {
            Text("These are predictors, not part of the flow label.")
            Score("Task difficulty", difficulty) { difficulty = it }
            Score("Goal clarity", goalClarity) { goalClarity = it }
            Score("Perceived control", perceivedControl) { perceivedControl = it }
        }

        Button(
            onClick = {
                scope.launch {
                    val capturedAt = System.currentTimeMillis()
                    status = "Saving report and context…"
                    save(
                        SelfReportEntity(
                            capturedAtEpochMs = capturedAt,
                            flowScore = flow.toInt(),
                            absorption = absorption.toInt(),
                            effortlessControl = effortless.toInt(),
                            intrinsicReward = reward.toInt(),
                            presence = presence.toInt(),
                            fatigue = fatigue.toInt(),
                            activityLabel = activity.trim().ifBlank { null },
                            domain = domain.trim().ifBlank { null },
                            taskDifficulty = difficulty.toInt().takeIf { advanced },
                            goalClarity = goalClarity.toInt().takeIf { advanced },
                            perceivedControl = perceivedControl.toInt().takeIf { advanced },
                            notes = null,
                        ),
                    )
                    status = "Report + context snapshot saved locally"
                }
            },
        ) { Text("Save report") }

        Text("Health Connect probe", style = MaterialTheme.typography.titleLarge)
        Text("Reads the last 7 days and reports record origin plus time coverage. No health data is uploaded.")
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
        results.forEach { result -> ProbeResultView(result) }
    }
}

@Composable
private fun ProbeResultView(result: ProbeResult) {
    val originText = if (result.origins.isEmpty()) "none" else result.origins.joinToString()
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(result.type, style = MaterialTheme.typography.titleMedium)
        Text("Records: ${result.recordCount} · data points: ${result.dataPointCount}")
        Text("Origins: $originText")
        Text("Coverage: ${formatEpoch(result.earliestEpochMs)} → ${formatEpoch(result.latestEpochMs)}")
        result.error?.let { Text("Error: $it") }
    }
}

private fun formatEpoch(epochMs: Long?): String {
    if (epochMs == null) return "none"
    return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        .withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochMilli(epochMs))
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
