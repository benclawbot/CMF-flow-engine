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
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.benclawbot.cmfflow.ui.FlowTheme
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

            FlowTheme {
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
                    endSession = { database.sessionDao().end(it, System.currentTimeMillis()) },
                    recordRecommendation = { database.recommendationEventDao().insert(it) },
                    recordRecommendationResponse = { id, response -> database.recommendationEventDao().recordResponse(id, response, System.currentTimeMillis()) },
                    recordIntervention = { database.interventionEventDao().insert(it) },
                    recordInterventionResponse = { id, response -> database.interventionEventDao().recordResponse(id, response, System.currentTimeMillis()) },
                    save = { report ->
                        val reportId = database.selfReportDao().insert(report)
                        database.contextSnapshotDao().insert(contextCollector.collect(reportId, report.capturedAtEpochMs))
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
    var showCheckIn by remember { mutableStateOf(recentReports.isEmpty()) }
    var showTaskForm by remember { mutableStateOf(false) }
    var showInsights by remember { mutableStateOf(false) }
    var showHealth by remember { mutableStateOf(false) }
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

    val healthPermissionLauncher = rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) { granted ->
        status = if (granted.containsAll(probe.permissions)) "Health access connected" else "Some health access is still off"
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            CheckInReminderScheduler.enable(context)
            status = "Check-in reminders enabled"
        } else status = "Reminders need notification permission"
    }

    val latest = recentReports.firstOrNull()
    val sessionState = sessionSignals(activeSession)
    val fragmentationEvidence = fragmentationEvidenceFor(recentContexts.firstOrNull(), recentReports, recentContexts)
    val intervention = recommendIntervention(
        latestReport = latest,
        minutesOnCurrentTask = sessionState.minutesOnCurrentTask,
        repeatedStruggle = sessionState.repeatedStruggle,
        learnedFragmentationHarm = fragmentationEvidence.harmfulAssociation,
        currentlyHighFragmentation = fragmentationEvidence.currentlyHigh,
    )
    LaunchedEffect(intervention.action, intervention.reasons, activeSession?.id, activeSession?.struggleCount) {
        activeInterventionEventId = recordIntervention(InterventionEventEntity(action = intervention.action.name, reasonsSnapshot = intervention.reasons.joinToString("|")))
        interventionResponse = null
    }

    val ranked = rankTasks(
        tasks = openTasks,
        latestReport = latest,
        historicalReports = recentReports,
        currentContext = recentContexts.firstOrNull(),
        historicalContexts = recentContexts,
        recommendationEvents = recentRecommendations,
    )
    val topTask = ranked.firstOrNull()
    LaunchedEffect(topTask?.task?.id, topTask?.score) {
        if (topTask != null) {
            activeRecommendationEventId = recordRecommendation(
                RecommendationEventEntity(
                    taskId = topTask.task.id,
                    taskTitle = topTask.task.title,
                    taskDomain = topTask.task.domain,
                    score = topTask.score,
                    reasonsSnapshot = topTask.reasons.joinToString("|"),
                ),
            )
            recommendationResponse = null
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("CMF Flow", style = MaterialTheme.typography.headlineMedium)
        Text("One useful next step, not more noise.", color = MaterialTheme.colorScheme.onSurfaceVariant)

        StateCard(latest, activeSession, sessionState.minutesOnCurrentTask)

        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Right now", style = MaterialTheme.typography.titleLarge)
                Text(friendlyAction(intervention.action.name), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                Text(friendlyReason(intervention.reasons), color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (activeSession != null) {
                    Text("${activeSession.taskTitle ?: "Focus session"} · ${sessionState.minutesOnCurrentTask ?: 0} min · ${activeSession.struggleCount} struggle marks")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { scope.launch { recordStruggle(activeSession.id); status = "Struggle noted" } }) { Text("I'm stuck") }
                        OutlinedButton(onClick = { scope.launch { endSession(activeSession.id); status = "Session ended" } }) { Text("End") }
                    }
                }
                if (interventionResponse == null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { scope.launch { activeInterventionEventId?.let { recordInterventionResponse(it, "accepted") }; interventionResponse = "accepted" } }) { Text("Do it") }
                        TextButton(onClick = { scope.launch { activeInterventionEventId?.let { recordInterventionResponse(it, "dismissed") }; interventionResponse = "dismissed" } }) { Text("Not now") }
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Best next task", style = MaterialTheme.typography.titleLarge)
                if (topTask == null) {
                    Text("No tasks yet. Add one when you want Flow to help choose what to do next.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedButton(onClick = { showTaskForm = true }) { Text("Add a task") }
                } else {
                    Text(topTask.task.title, style = MaterialTheme.typography.titleMedium)
                    Text(topTask.reasons.take(2).joinToString(" · "), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (recommendationResponse == null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                scope.launch {
                                    activeRecommendationEventId?.let { recordRecommendationResponse(it, "accepted") }
                                    recommendationResponse = "accepted"
                                    if (activeSession == null) startSession(SessionEntity(taskId = topTask.task.id, taskTitle = topTask.task.title, taskDomain = topTask.task.domain, startedAtEpochMs = System.currentTimeMillis()))
                                }
                            }) { Text("Start focus") }
                            TextButton(onClick = { scope.launch { activeRecommendationEventId?.let { recordRecommendationResponse(it, "rejected") }; recommendationResponse = "rejected" } }) { Text("Another") }
                        }
                    }
                }
            }
        }

        Button(onClick = { showCheckIn = !showCheckIn }, modifier = Modifier.fillMaxWidth()) {
            Text(if (showCheckIn) "Hide check-in" else "Quick check-in")
        }
        if (showCheckIn) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("How did that feel?", style = MaterialTheme.typography.titleLarge)
                    Text("Takes about 20 seconds. This is the signal Flow learns from.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    CompactScore("Flow", flow) { flow = it }
                    CompactScore("Absorption", absorption) { absorption = it }
                    CompactScore("Effortless control", effortless) { effortless = it }
                    CompactScore("Enjoyment", reward) { reward = it }
                    CompactScore("Presence", presence) { presence = it }
                    CompactScore("Fatigue", fatigue) { fatigue = it }
                    OutlinedTextField(activity, { activity = it }, Modifier.fillMaxWidth(), label = { Text("What were you doing? (optional)") }, singleLine = true)
                    OutlinedTextField(domain, { domain = it }, Modifier.fillMaxWidth(), label = { Text("Area of life (optional)") }, singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Switch(advanced, { advanced = it })
                        Text("More context")
                    }
                    if (advanced) {
                        CompactScore("Difficulty", difficulty) { difficulty = it }
                        CompactScore("Goal clarity", goalClarity) { goalClarity = it }
                        CompactScore("Control", perceivedControl) { perceivedControl = it }
                    }
                    Button(onClick = {
                        scope.launch {
                            save(SelfReportEntity(
                                capturedAtEpochMs = System.currentTimeMillis(), flowScore = flow.toInt(), absorption = absorption.toInt(),
                                effortlessControl = effortless.toInt(), intrinsicReward = reward.toInt(), presence = presence.toInt(), fatigue = fatigue.toInt(),
                                activityLabel = activity.trim().ifBlank { null }, domain = domain.trim().ifBlank { null },
                                taskDifficulty = difficulty.toInt().takeIf { advanced }, goalClarity = goalClarity.toInt().takeIf { advanced },
                                perceivedControl = perceivedControl.toInt().takeIf { advanced }, notes = null,
                            ))
                            status = "Check-in saved"
                            showCheckIn = false
                        }
                    }, modifier = Modifier.fillMaxWidth()) { Text("Save check-in") }
                }
            }
        }

        SectionToggle("Tasks", "${openTasks.size} open", showTaskForm) { showTaskForm = !showTaskForm }
        if (showTaskForm) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(taskTitle, { taskTitle = it }, Modifier.fillMaxWidth(), label = { Text("Task") }, singleLine = true)
                    OutlinedTextField(taskDomain, { taskDomain = it }, Modifier.fillMaxWidth(), label = { Text("Area") }, singleLine = true)
                    CompactScore("Value", taskValue) { taskValue = it }
                    CompactScore("Urgency", taskUrgency) { taskUrgency = it }
                    CompactScore("Difficulty", taskDifficulty) { taskDifficulty = it }
                    OutlinedTextField(taskMinutes, { taskMinutes = it.filter(Char::isDigit) }, Modifier.fillMaxWidth(), label = { Text("Minutes") }, singleLine = true)
                    Button(enabled = taskTitle.isNotBlank(), onClick = {
                        scope.launch {
                            addTask(TaskEntity(title = taskTitle.trim(), domain = taskDomain.trim().ifBlank { "other" }, valueScore = taskValue.toInt(), urgencyScore = taskUrgency.toInt(), difficultyScore = taskDifficulty.toInt(), estimatedMinutes = taskMinutes.toIntOrNull()?.coerceAtLeast(1) ?: 30))
                            taskTitle = ""
                            status = "Task added"
                        }
                    }) { Text("Add task") }
                    ranked.take(4).forEach { item ->
                        HorizontalDivider()
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Text(item.task.title)
                                Text(item.task.domain, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            TextButton(onClick = { scope.launch { markTaskDone(item.task.id) } }) { Text("Done") }
                        }
                    }
                }
            }
        }

        SectionToggle("Insights", insightSubtitle(recentReports), showInsights) { showInsights = !showInsights }
        if (showInsights) LearningCard(recentReports)

        SectionToggle("Health & settings", if (results.isEmpty()) "Connect sensors and diagnostics" else "Probe complete", showHealth) { showHealth = !showHealth }
        if (showHealth) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Health Connect", style = MaterialTheme.typography.titleMedium)
                    Text("CMF Flow reads health data locally to understand context. Nothing is uploaded.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { healthPermissionLauncher.launch(probe.permissions) }) { Text("Manage access") }
                        Button(onClick = { scope.launch { status = "Checking health data…"; results = probe.probe(); status = "Health check complete" } }) { Text("Check sensors") }
                    }
                    if (results.isNotEmpty()) results.forEach { ProbeResultView(it) }
                    HorizontalDivider()
                    Text("Check-in reminders", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            if (Build.VERSION.SDK_INT >= 33) notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            else { CheckInReminderScheduler.enable(context); status = "Reminders enabled" }
                        }) { Text("Enable") }
                        TextButton(onClick = { CheckInReminderScheduler.disable(context); status = "Reminders disabled" }) { Text("Disable") }
                    }
                }
            }
        }

        Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun StateCard(report: SelfReportEntity?, session: SessionEntity?, minutes: Long?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(if (session != null) "Focus in progress" else "Your current state", style = MaterialTheme.typography.titleLarge)
            if (session != null) {
                Text(session.taskTitle ?: "Focused session", style = MaterialTheme.typography.headlineSmall)
                Text("${minutes ?: 0} minutes", color = MaterialTheme.colorScheme.primary)
            } else if (report == null) {
                Text("Start with a quick check-in so Flow can understand today.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text("Flow ${report.flowScore}/5  ·  Presence ${report.presence}/5  ·  Fatigue ${report.fatigue}/5", style = MaterialTheme.typography.titleMedium)
                Text("Based on your latest check-in", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SectionToggle(title: String, subtitle: String, expanded: Boolean, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth()) {
            Text(title)
            Text(subtitle + if (expanded) " · Hide" else " · Open", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun LearningCard(reports: List<SelfReportEntity>) {
    val summary = summarize(reports)
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("What Flow is learning", style = MaterialTheme.typography.titleLarge)
            if (summary.sampleCount < 5) {
                Text("${summary.sampleCount}/5 check-ins collected. A few more will unlock your first personal patterns.")
            } else {
                Text("Average flow ${formatScore(summary.averageFlow)} / 5")
                Text("Average presence ${formatScore(summary.averagePresence)} / 5")
                Text("Average fatigue ${formatScore(summary.averageFatigue)} / 5")
                summary.strongestDomain?.let { Text("Strongest area so far: $it") }
            }
            Text("Insights stay descriptive until there is enough evidence.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun insightSubtitle(reports: List<SelfReportEntity>): String = if (reports.size < 5) "${reports.size}/5 check-ins" else "Personal patterns available"
private fun formatScore(value: Double?): String = value?.let { "%.1f".format(it) } ?: "n/a"
private fun friendlyAction(action: String): String = when (action) {
    "CONTINUE" -> "Keep going"
    "SWITCH_TASK" -> "Switch gears"
    "REDUCE_DIFFICULTY" -> "Make it easier"
    "ASK_AI" -> "Get a little help"
    "BREAK" -> "Take a short break"
    "EXERCISE" -> "Move for a few minutes"
    "STOP" -> "Call it for now"
    "REDUCE_INTERRUPTION" -> "Protect your attention"
    else -> action.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
}
private fun friendlyReason(reasons: List<String>): String = reasons.firstOrNull()?.replace('_', ' ')?.replaceFirstChar { it.uppercase() } ?: "Based on your latest state"

@Composable
private fun ProbeResultView(result: ProbeResult) {
    val label = when (result.type) {
        "heart_rate" -> "Heart rate"
        "oxygen_saturation" -> "Blood oxygen"
        else -> result.type.replace('_', ' ').replaceFirstChar { it.uppercase() }
    }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.titleSmall)
        if (result.error != null) Text("Unavailable: ${result.error}", color = MaterialTheme.colorScheme.error)
        else if (result.recordCount == 0) Text("No recent data", color = MaterialTheme.colorScheme.onSurfaceVariant)
        else {
            Text("${result.recordCount} records · latest ${formatEpoch(result.latestEpochMs)}")
            Text(result.origins.joinToString().ifBlank { "Unknown source" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun formatEpoch(epochMs: Long?): String {
    if (epochMs == null) return "none"
    return DateTimeFormatter.ofPattern("MMM d, HH:mm").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(epochMs))
}

@Composable
private fun CompactScore(label: String, value: Float, onValueChange: (Float) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label)
            Text("${value.toInt()}/5", color = MaterialTheme.colorScheme.primary)
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = 0f..5f, steps = 4)
    }
}
