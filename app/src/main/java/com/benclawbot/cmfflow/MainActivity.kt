package com.benclawbot.cmfflow

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import com.benclawbot.cmfflow.analytics.summarize
import com.benclawbot.cmfflow.data.ContextSnapshotEntity
import com.benclawbot.cmfflow.data.InterventionEventEntity
import com.benclawbot.cmfflow.data.RecommendationEventEntity
import com.benclawbot.cmfflow.data.SelfReportEntity
import com.benclawbot.cmfflow.data.SessionEntity
import com.benclawbot.cmfflow.data.TaskEntity
import com.benclawbot.cmfflow.health.HealthConnectProbe
import com.benclawbot.cmfflow.health.HealthContextCollector
import com.benclawbot.cmfflow.health.ProbeResult
import com.benclawbot.cmfflow.interventions.recommendIntervention
import com.benclawbot.cmfflow.interventions.sessionSignals
import com.benclawbot.cmfflow.ranking.fragmentationEvidenceFor
import com.benclawbot.cmfflow.ranking.rankTasks
import com.benclawbot.cmfflow.reminders.CheckInReminderScheduler
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
            val recentReports by database.selfReportDao().observeRecent().collectAsState(initial = emptyList())
            val recentContexts by database.contextSnapshotDao().observeRecent().collectAsState(initial = emptyList())
            val recentRecommendations by database.recommendationEventDao().observeRecent().collectAsState(initial = emptyList())
            val openTasks by database.taskDao().observeOpen().collectAsState(initial = emptyList())
            val activeSession by database.sessionDao().observeActive().collectAsState(initial = null)
            MaterialTheme {
                FlowHome(
                    probe = probe,
                    recentReports = recentReports,
                    recentContexts = recentContexts,
                    recentRecommendations = recentRecommendations,
                    openTasks = openTasks,
                    activeSession = activeSession,
                    addTask = { database.taskDao().insert(it) },
                    markTaskDone = { database.taskDao().markDone(it) },
                    startSession = { database.sessionDao().insert(it) },
                    recordStruggle = { database.sessionDao().recordStruggle(it) },
                    endSession = { sessionId -> database.sessionDao().end(sessionId, System.currentTimeMillis()) },
                    recordRecommendation = { database.recommendationEventDao().insert(it) },
                    recordRecommendationResponse = { eventId, response ->
                        database.recommendationEventDao().recordResponse(eventId, response, System.currentTimeMillis())
                    },
                    recordIntervention = { database.interventionEventDao().insert(it) },
                    recordInterventionResponse = { eventId, response ->
                        database.interventionEventDao().recordResponse(eventId, response, System.currentTimeMillis())
                    },
                    save = { report ->
                        val reportId = database.selfReportDao().insert(report)
                        val snapshot = contextCollector.collect(reportId, report.capturedAtEpochMs)
                        database.contextSnapshotDao().insert(snapshot)
                        database.recommendationEventDao().attachOutcomeToLatestResponded(reportId)
                        database.interventionEventDao().attachOutcomeToLatestResponded(reportId)
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
    recentReports: List<SelfReportEntity>,
    recentContexts: List<ContextSnapshotEntity>,
    recentRecommendations: List<RecommendationEventEntity>,
    openTasks: List<TaskEntity>,
    activeSession: SessionEntity?,
    addTask: suspend (TaskEntity) -> Long,
    markTaskDone: suspend (Long) -> Unit,
    startSession: suspend (SessionEntity) -> Long,
    recordStruggle: suspend (Long) -> Unit,
    endSession: suspend (Long) -> Unit,
    recordRecommendation: suspend (RecommendationEventEntity) -> Long,
    recordRecommendationResponse: suspend (Long, String) -> Unit,
    recordIntervention: suspend (InterventionEventEntity) -> Long,
    recordInterventionResponse: suspend (Long, String) -> Unit,
    save: suspend (SelfReportEntity) -> Long,
) {
    val context = LocalContext.current
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
    var taskTitle by remember { mutableStateOf("") }
    var taskDomain by remember { mutableStateOf("work") }
    var taskValue by remember { mutableStateOf(3f) }
    var taskUrgency by remember { mutableStateOf(3f) }
    var taskDifficulty by remember { mutableStateOf(3f) }
    var taskMinutes by remember { mutableStateOf("30") }
    var activeRecommendationEventId by remember { mutableStateOf<Long?>(null) }
    var recommendationResponse by remember { mutableStateOf<String?>(null) }
    var activeInterventionEventId by remember { mutableStateOf<Long?>(null) }
    var interventionResponse by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf("Ready") }
    var results by remember { mutableStateOf<List<ProbeResult>>(emptyList()) }

    val healthPermissionLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract(),
    ) { granted ->
        status = if (granted.containsAll(probe.permissions)) "Health permissions granted" else "Some health permissions were denied"
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            CheckInReminderScheduler.enable(context)
            status = "Check-in reminders enabled"
        } else {
            status = "Notification permission denied; reminders not enabled"
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
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

        OutlinedTextField(value = activity, onValueChange = { activity = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Activity (optional)") }, singleLine = true)
        OutlinedTextField(value = domain, onValueChange = { domain = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Domain (optional, e.g. work, cooking, family)") }, singleLine = true)

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

        Button(onClick = {
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
                status = "Report + context saved; eligible recommendation/intervention outcomes linked"
            }
        }) { Text("Save report") }

        LearningPreview(recentReports)

        val sessionState = sessionSignals(activeSession)
        val fragmentationEvidence = fragmentationEvidenceFor(
            current = recentContexts.firstOrNull(),
            reports = recentReports,
            snapshots = recentContexts,
        )
        val intervention = recommendIntervention(
            latestReport = recentReports.firstOrNull(),
            minutesOnCurrentTask = sessionState.minutesOnCurrentTask,
            repeatedStruggle = sessionState.repeatedStruggle,
            learnedFragmentationHarm = fragmentationEvidence.harmfulAssociation,
            currentlyHighFragmentation = fragmentationEvidence.currentlyHigh,
        )
        LaunchedEffect(intervention.action, intervention.reasons, activeSession?.id, activeSession?.struggleCount) {
            activeInterventionEventId = recordIntervention(
                InterventionEventEntity(
                    action = intervention.action.name,
                    reasonsSnapshot = intervention.reasons.joinToString("|"),
                ),
            )
            interventionResponse = null
        }
        Text("Suggested intervention", style = MaterialTheme.typography.titleLarge)
        Text(intervention.action.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() })
        Text(intervention.reasons.joinToString(" · "))
        activeSession?.let { session ->
            Text("Active session: ${session.taskTitle ?: "unassigned"} · ${sessionState.minutesOnCurrentTask ?: 0} min · struggles ${session.struggleCount}")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    scope.launch {
                        recordStruggle(session.id)
                        status = "Struggle marked; repeated struggle can trigger AI assistance"
                    }
                }) { Text("I'm stuck") }
                Button(onClick = {
                    scope.launch {
                        endSession(session.id)
                        status = "Session ended"
                    }
                }) { Text("End session") }
            }
        }
        if (interventionResponse == null) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    scope.launch {
                        activeInterventionEventId?.let { recordInterventionResponse(it, "accepted") }
                        interventionResponse = "accepted"
                        status = "Intervention accepted; next check-in will be linked as outcome"
                    }
                }) { Text("Accept") }
                Button(onClick = {
                    scope.launch {
                        activeInterventionEventId?.let { recordInterventionResponse(it, "dismissed") }
                        interventionResponse = "dismissed"
                        status = "Intervention dismissed; next check-in will still record the outcome"
                    }
                }) { Text("Dismiss") }
            }
        } else {
            Text("Intervention response: $interventionResponse")
        }

        Text("Tasks", style = MaterialTheme.typography.titleLarge)
        Text("Ranking is local and transparent. Personal history, paired context, and recommendation outcomes are used only after minimum evidence thresholds are met.")
        OutlinedTextField(value = taskTitle, onValueChange = { taskTitle = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Task title") }, singleLine = true)
        OutlinedTextField(value = taskDomain, onValueChange = { taskDomain = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Task domain") }, singleLine = true)
        Score("Task value", taskValue) { taskValue = it }
        Score("Task urgency", taskUrgency) { taskUrgency = it }
        Score("Task difficulty", taskDifficulty) { taskDifficulty = it }
        OutlinedTextField(value = taskMinutes, onValueChange = { taskMinutes = it.filter(Char::isDigit) }, modifier = Modifier.fillMaxWidth(), label = { Text("Estimated minutes") }, singleLine = true)
        Button(enabled = taskTitle.isNotBlank(), onClick = {
            scope.launch {
                addTask(
                    TaskEntity(
                        title = taskTitle.trim(),
                        domain = taskDomain.trim().ifBlank { "other" },
                        valueScore = taskValue.toInt(),
                        urgencyScore = taskUrgency.toInt(),
                        difficultyScore = taskDifficulty.toInt(),
                        estimatedMinutes = taskMinutes.toIntOrNull()?.coerceAtLeast(1) ?: 30,
                    ),
                )
                taskTitle = ""
                status = "Task saved locally"
            }
        }) { Text("Add task") }

        val ranked = rankTasks(
            tasks = openTasks,
            latestReport = recentReports.firstOrNull(),
            historicalReports = recentReports,
            currentContext = recentContexts.firstOrNull(),
            historicalContexts = recentContexts,
            recommendationEvents = recentRecommendations,
        )
        ranked.firstOrNull()?.let { recommendation ->
            LaunchedEffect(recommendation.task.id, recommendation.score) {
                activeRecommendationEventId = recordRecommendation(
                    RecommendationEventEntity(
                        taskId = recommendation.task.id,
                        taskTitle = recommendation.task.title,
                        taskDomain = recommendation.task.domain,
                        score = recommendation.score,
                        reasonsSnapshot = recommendation.reasons.joinToString("|"),
                    ),
                )
                recommendationResponse = null
            }

            Text("Suggested now", style = MaterialTheme.typography.titleMedium)
            Text(recommendation.task.title)
            Text("Score %.1f · %s".format(recommendation.score, recommendation.reasons.joinToString(" · ")))
            if (recommendationResponse == null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        scope.launch {
                            activeRecommendationEventId?.let { recordRecommendationResponse(it, "accepted") }
                            recommendationResponse = "accepted"
                            if (activeSession == null) {
                                startSession(
                                    SessionEntity(
                                        taskId = recommendation.task.id,
                                        taskTitle = recommendation.task.title,
                                        taskDomain = recommendation.task.domain,
                                        startedAtEpochMs = System.currentTimeMillis(),
                                    ),
                                )
                                status = "Recommendation accepted; focus session started"
                            } else {
                                status = "Recommendation accepted; active session already running"
                            }
                        }
                    }) { Text("Accept + start") }
                    Button(onClick = {
                        scope.launch {
                            activeRecommendationEventId?.let { recordRecommendationResponse(it, "rejected") }
                            recommendationResponse = "rejected"
                            status = "Recommendation rejected; next check-in will be linked as outcome"
                        }
                    }) { Text("Reject") }
                    Button(onClick = {
                        scope.launch {
                            activeRecommendationEventId?.let { recordRecommendationResponse(it, "ignored") }
                            recommendationResponse = "ignored"
                            status = "Recommendation ignored; next check-in will be linked as outcome"
                        }
                    }) { Text("Ignore") }
                }
            } else {
                Text("Response recorded: $recommendationResponse")
            }
            Button(onClick = { scope.launch { markTaskDone(recommendation.task.id) } }) { Text("Mark suggested task done") }
        } ?: Text("No open tasks yet.")

        if (ranked.size > 1) {
            Text("Next alternatives")
            ranked.drop(1).take(3).forEach { item -> Text("${item.task.title} · ${"%.1f".format(item.score)}") }
        }

        Text("Sampling reminders", style = MaterialTheme.typography.titleLarge)
        Text("Optional local reminders run about every 4 hours during 08:00–21:59. They can be disabled at any time.")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = {
                if (Build.VERSION.SDK_INT >= 33) notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                else {
                    CheckInReminderScheduler.enable(context)
                    status = "Check-in reminders enabled"
                }
            }) { Text("Enable") }
            Button(onClick = {
                CheckInReminderScheduler.disable(context)
                status = "Check-in reminders disabled"
            }) { Text("Disable") }
        }

        Text("Health Connect probe", style = MaterialTheme.typography.titleLarge)
        Text("Reads the last 7 days and reports record origin plus time coverage. No health data is uploaded.")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { healthPermissionLauncher.launch(probe.permissions) }) { Text("Grant access") }
            Button(onClick = {
                scope.launch {
                    status = "Probing last 7 days…"
                    results = probe.probe()
                    status = "Probe complete"
                }
            }) { Text("Run probe") }
        }

        Text(status)
        results.forEach { result -> ProbeResultView(result) }
    }
}

@Composable
private fun LearningPreview(reports: List<SelfReportEntity>) {
    val summary = summarize(reports)
    Text("Learning preview", style = MaterialTheme.typography.titleLarge)
    if (summary.sampleCount < 5) {
        Text("${summary.sampleCount}/5 reports collected. No personal pattern claims yet.")
        return
    }
    Text("Based on ${summary.sampleCount} local reports; descriptive only, not a prediction.")
    Text("Average flow: ${formatScore(summary.averageFlow)} / 5")
    Text("Average presence: ${formatScore(summary.averagePresence)} / 5")
    Text("Average fatigue: ${formatScore(summary.averageFatigue)} / 5")
    summary.strongestDomain?.let { Text("Highest-flow domain with ≥3 samples: $it") }
}

private fun formatScore(value: Double?): String = value?.let { "%.1f".format(it) } ?: "n/a"

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
    return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(epochMs))
}

@Composable
private fun Score(label: String, value: Float, onValueChange: (Float) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("$label: ${value.toInt()}")
        Slider(value = value, onValueChange = onValueChange, valueRange = 0f..5f, steps = 4)
    }
}