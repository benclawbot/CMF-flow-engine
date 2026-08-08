package com.benclawbot.cmfflow

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import com.benclawbot.cmfflow.analytics.summarize
import com.benclawbot.cmfflow.attention.AttentionAccess
import com.benclawbot.cmfflow.data.ContextSnapshotEntity
import com.benclawbot.cmfflow.data.ExperimentEntity
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
            val reports by database.selfReportDao().observeRecent().collectAsState(initial = emptyList())
            val contexts by database.contextSnapshotDao().observeRecent().collectAsState(initial = emptyList())
            val recommendations by database.recommendationEventDao().observeRecent().collectAsState(initial = emptyList())
            val tasks by database.taskDao().observeOpen().collectAsState(initial = emptyList())
            val session by database.sessionDao().observeActive().collectAsState(initial = null)
            val experiments by database.experimentDao().observeActive().collectAsState(initial = emptyList())

            FlowTheme {
                FlowApp(
                    probe = probe,
                    reports = reports,
                    contexts = contexts,
                    recommendations = recommendations,
                    tasks = tasks,
                    session = session,
                    experiments = experiments,
                    saveReport = { report ->
                        val reportId = database.selfReportDao().insert(report)
                        database.contextSnapshotDao().insert(contextCollector.collect(reportId, report.capturedAtEpochMs))
                        database.recommendationEventDao().attachOutcomeToLatestResponded(reportId)
                        database.interventionEventDao().attachOutcomeToLatestResponded(reportId)
                        database.experimentAssignmentDao().attachOutcomeToLatestOpen(reportId, System.currentTimeMillis())
                    },
                    addTask = { database.taskDao().insert(it) },
                    completeTask = { database.taskDao().markDone(it) },
                    startSession = { database.sessionDao().insert(it) },
                    endSession = { database.sessionDao().end(it, System.currentTimeMillis()) },
                    markStruggle = { database.sessionDao().recordStruggle(it) },
                    recordRecommendation = { database.recommendationEventDao().insert(it) },
                    respondRecommendation = { id, response -> database.recommendationEventDao().recordResponse(id, response, System.currentTimeMillis()) },
                    recordIntervention = { database.interventionEventDao().insert(it) },
                    respondIntervention = { id, response -> database.interventionEventDao().recordResponse(id, response, System.currentTimeMillis()) },
                    addExperiment = { database.experimentDao().insert(it) },
                    completeExperiment = { database.experimentDao().complete(it) },
                )
            }
        }
    }
}

private enum class AppTab(val label: String) { Home("Home"), Insights("Insights"), Tasks("Tasks"), Experiments("Experiments"), Settings("Settings") }

@Composable
private fun FlowApp(
    probe: HealthConnectProbe,
    reports: List<SelfReportEntity>,
    contexts: List<ContextSnapshotEntity>,
    recommendations: List<RecommendationEventEntity>,
    tasks: List<TaskEntity>,
    session: SessionEntity?,
    experiments: List<ExperimentEntity>,
    saveReport: suspend (SelfReportEntity) -> Unit,
    addTask: suspend (TaskEntity) -> Long,
    completeTask: suspend (Long) -> Unit,
    startSession: suspend (SessionEntity) -> Long,
    endSession: suspend (Long) -> Unit,
    markStruggle: suspend (Long) -> Unit,
    recordRecommendation: suspend (RecommendationEventEntity) -> Long,
    respondRecommendation: suspend (Long, String) -> Unit,
    recordIntervention: suspend (InterventionEventEntity) -> Long,
    respondIntervention: suspend (Long, String) -> Unit,
    addExperiment: suspend (ExperimentEntity) -> Long,
    completeExperiment: suspend (Long) -> Unit,
) {
    var tab by remember { mutableStateOf(AppTab.Home) }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val notify: (String) -> Unit = { message -> scope.launch { snackbar.showSnackbar(message) } }

    val latest = reports.firstOrNull()
    val sessionState = sessionSignals(session)
    val fragmentation = fragmentationEvidenceFor(contexts.firstOrNull(), reports, contexts)
    val intervention = recommendIntervention(
        latestReport = latest,
        minutesOnCurrentTask = sessionState.minutesOnCurrentTask,
        repeatedStruggle = sessionState.repeatedStruggle,
        learnedFragmentationHarm = fragmentation.harmfulAssociation,
        currentlyHighFragmentation = fragmentation.currentlyHigh,
    )
    val ranked = rankTasks(
        tasks = tasks,
        latestReport = latest,
        historicalReports = reports,
        currentContext = contexts.firstOrNull(),
        historicalContexts = contexts,
        recommendationEvents = recommendations,
    )
    val topTask = ranked.firstOrNull()

    var interventionEventId by remember { mutableStateOf<Long?>(null) }
    var recommendationEventId by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(intervention.action, intervention.reasons, session?.id, session?.struggleCount) {
        interventionEventId = recordIntervention(InterventionEventEntity(action = intervention.action.name, reasonsSnapshot = intervention.reasons.joinToString("|")))
    }
    LaunchedEffect(topTask?.task?.id, topTask?.score) {
        recommendationEventId = if (topTask == null) null else recordRecommendation(
            RecommendationEventEntity(
                taskId = topTask.task.id,
                taskTitle = topTask.task.title,
                taskDomain = topTask.task.domain,
                score = topTask.score,
                reasonsSnapshot = topTask.reasons.joinToString("|"),
            ),
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar {
                AppTab.entries.forEach { item ->
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = { tab = item },
                        icon = { Icon(tabIcon(item), contentDescription = item.label) },
                        label = { Text(item.label, maxLines = 1) },
                    )
                }
            }
        },
    ) { inner ->
        Box(Modifier.fillMaxSize().padding(inner)) {
            when (tab) {
                AppTab.Home -> HomeScreen(
                    latest = latest,
                    session = session,
                    sessionMinutes = sessionState.minutesOnCurrentTask,
                    interventionTitle = friendlyAction(intervention.action.name),
                    interventionReason = friendlyReason(intervention.reasons),
                    topTask = topTask?.task,
                    onCheckIn = saveReport,
                    onDoIntervention = { scope.launch { interventionEventId?.let { respondIntervention(it, "accepted") }; notify("Action accepted") } },
                    onDismissIntervention = { scope.launch { interventionEventId?.let { respondIntervention(it, "dismissed") } } },
                    onStartTask = {
                        val task = topTask?.task
                        if (task != null) {
                            scope.launch {
                                recommendationEventId?.let { respondRecommendation(it, "accepted") }
                                if (session == null) startSession(SessionEntity(taskId = task.id, taskTitle = task.title, taskDomain = task.domain, startedAtEpochMs = System.currentTimeMillis()))
                                notify("Focus session started")
                            }
                        }
                    },
                    onOpenTasks = { tab = AppTab.Tasks },
                    onEndSession = { id -> scope.launch { endSession(id); notify("Session ended") } },
                    onStruggle = { id -> scope.launch { markStruggle(id); notify("Struggle noted") } },
                )
                AppTab.Insights -> InsightsScreen(reports, contexts)
                AppTab.Tasks -> TasksScreen(
                    rankedTasks = ranked.map { it.task to it.reasons },
                    onAdd = { task -> scope.launch { addTask(task); notify("Task added") } },
                    onDone = { id -> scope.launch { completeTask(id); notify("Task completed") } },
                    onStart = { task -> scope.launch { if (session == null) startSession(SessionEntity(taskId = task.id, taskTitle = task.title, taskDomain = task.domain, startedAtEpochMs = System.currentTimeMillis())); tab = AppTab.Home; notify("Focus session started") } },
                )
                AppTab.Experiments -> ExperimentsScreen(
                    experiments = experiments,
                    onAdd = { exp -> scope.launch { addExperiment(exp); notify("Experiment created") } },
                    onComplete = { id -> scope.launch { completeExperiment(id); notify("Experiment completed") } },
                )
                AppTab.Settings -> SettingsScreen(probe = probe, notify = notify)
            }
        }
    }
}

@Composable
private fun HomeScreen(
    latest: SelfReportEntity?, session: SessionEntity?, sessionMinutes: Int?, interventionTitle: String, interventionReason: String, topTask: TaskEntity?,
    onCheckIn: suspend (SelfReportEntity) -> Unit, onDoIntervention: () -> Unit, onDismissIntervention: () -> Unit, onStartTask: () -> Unit,
    onOpenTasks: () -> Unit, onEndSession: (Long) -> Unit, onStruggle: (Long) -> Unit,
) {
    var checkIn by remember { mutableStateOf(latest == null) }
    ScreenColumn {
        Text(greeting(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Make the next hour easier to enter.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        ElevatedCard(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), shape = RoundedCornerShape(28.dp)) {
            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(if (session != null) "Current focus" else "Your state", color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(if (session != null) session.taskTitle ?: "Focus session" else stateLabel(latest), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                if (session != null) {
                    Text("${sessionMinutes ?: 0} min in focus · ${session.struggleCount} struggle marks")
                    LinearProgressIndicator(progress = { ((sessionMinutes ?: 0) / 60f).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onStruggle(session.id) }) { Text("I'm stuck") }
                        TextButton(onClick = { onEndSession(session.id) }) { Text("Finish") }
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        Metric("Flow", latest?.flowScore); Metric("Presence", latest?.presence); Metric("Fatigue", latest?.fatigue)
                    }
                }
            }
        }
        SectionTitle("Right now")
        ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(interventionTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(interventionReason, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Button(onClick = onDoIntervention) { Text("Do it") }; TextButton(onClick = onDismissIntervention) { Text("Not now") } }
            }
        }
        SectionTitle("Best next task")
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (topTask == null) {
                    Text("Nothing queued", style = MaterialTheme.typography.titleMedium)
                    Text("Add a few tasks and Flow will choose based on your current state.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedButton(onClick = onOpenTasks) { Text("Add tasks") }
                } else {
                    Text(topTask.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text("${topTask.domain.replaceFirstChar { it.uppercase() }} · ${topTask.estimatedMinutes} min", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = onStartTask) { Icon(Icons.Filled.PlayArrow, null); Spacer(Modifier.size(6.dp)); Text("Start focus") }
                }
            }
        }
        Button(onClick = { checkIn = !checkIn }, modifier = Modifier.fillMaxWidth()) { Text(if (checkIn) "Close check-in" else "Quick check-in") }
        if (checkIn) CheckInCard(onSave = { report -> onCheckIn(report); checkIn = false })
    }
}

@Composable
private fun InsightsScreen(reports: List<SelfReportEntity>, contexts: List<ContextSnapshotEntity>) {
    val summary = summarize(reports)
    ScreenColumn {
        ScreenHeader("Insights", "Patterns from your own data — descriptive until evidence is strong enough.")
        if (reports.isEmpty()) {
            EmptyCard("No insights yet", "A few quick check-ins will turn this into your personal flow map.")
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                InsightMetric("Flow", formatScore(summary.averageFlow), Modifier.weight(1f)); InsightMetric("Presence", formatScore(summary.averagePresence), Modifier.weight(1f)); InsightMetric("Fatigue", formatScore(summary.averageFatigue), Modifier.weight(1f))
            }
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Recent flow", style = MaterialTheme.typography.titleLarge); Sparkline(reports.take(20).reversed().map { it.flowScore.toFloat() }); Text("${reports.size.coerceAtMost(100)} local check-ins available", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("What Flow is learning", style = MaterialTheme.typography.titleLarge)
                    val strongest = summary.strongestDomain
                    if (strongest != null) Text("Your strongest sampled domain so far: ${strongest.replaceFirstChar { it.uppercase() }}") else Text("Domain patterns unlock after at least 3 comparable check-ins.")
                    contexts.firstOrNull()?.let { Text("Recent context"); Text(contextSummary(it), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("Evidence policy", style = MaterialTheme.typography.titleMedium); Text("Flow waits for minimum sample thresholds and caps learned adjustments, so early coincidences cannot dominate recommendations.", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
        }
    }
}

@Composable
private fun TasksScreen(rankedTasks: List<Pair<TaskEntity, List<String>>>, onAdd: (TaskEntity) -> Unit, onDone: (Long) -> Unit, onStart: (TaskEntity) -> Unit) {
    var adding by remember { mutableStateOf(false) }; var title by remember { mutableStateOf("") }; var domain by remember { mutableStateOf("work") }; var value by remember { mutableStateOf(3) }; var urgency by remember { mutableStateOf(3) }; var difficulty by remember { mutableStateOf(3) }; var minutes by remember { mutableStateOf("30") }
    ScreenColumn {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) { Column(Modifier.weight(1f)) { ScreenHeader("Tasks", "Prioritized for your state, not just a static to-do list.") }; IconButton(onClick = { adding = !adding }) { Icon(Icons.Filled.Add, "Add task") } }
        if (adding) Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), label = { Text("Task") }, singleLine = true); OutlinedTextField(domain, { domain = it }, Modifier.fillMaxWidth(), label = { Text("Area of life") }, singleLine = true)
                Stepper("Value", value) { value = it }; Stepper("Urgency", urgency) { urgency = it }; Stepper("Difficulty", difficulty) { difficulty = it }
                OutlinedTextField(minutes, { minutes = it.filter(Char::isDigit) }, Modifier.fillMaxWidth(), label = { Text("Minutes") }, singleLine = true)
                Button(enabled = title.isNotBlank(), onClick = { onAdd(TaskEntity(title = title.trim(), domain = domain.trim().ifBlank { "other" }, valueScore = value, urgencyScore = urgency, difficultyScore = difficulty, estimatedMinutes = minutes.toIntOrNull()?.coerceAtLeast(1) ?: 30)); title = ""; adding = false }, modifier = Modifier.fillMaxWidth()) { Text("Add task") }
            }
        }
        if (rankedTasks.isEmpty()) EmptyCard("Your task list is clear", "Add something when you want help choosing the right next move.")
        rankedTasks.forEachIndexed { index, (task, reasons) ->
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.secondaryContainer) { Text("${index + 1}", Modifier.padding(horizontal = 10.dp, vertical = 6.dp), fontWeight = FontWeight.Bold) }; Spacer(Modifier.size(10.dp))
                        Column(Modifier.weight(1f)) { Text(task.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold); Text("${task.domain} · ${task.estimatedMinutes} min", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                    if (reasons.isNotEmpty()) Text(reasons.take(2).joinToString(" · "), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Button(onClick = { onStart(task) }) { Text("Focus") }; OutlinedButton(onClick = { onDone(task.id) }) { Text("Done") } }
                }
            }
        }
    }
}

@Composable
private fun ExperimentsScreen(experiments: List<ExperimentEntity>, onAdd: (ExperimentEntity) -> Unit, onComplete: (Long) -> Unit) {
    var adding by remember { mutableStateOf(false) }; var hypothesis by remember { mutableStateOf("") }; var a by remember { mutableStateOf("") }; var b by remember { mutableStateOf("") }
    ScreenColumn {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) { Column(Modifier.weight(1f)) { ScreenHeader("Experiments", "Small N-of-1 tests to learn what actually works for you.") }; IconButton(onClick = { adding = !adding }) { Icon(Icons.Filled.Add, "Add experiment") } }
        if (adding) Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(hypothesis, { hypothesis = it }, Modifier.fillMaxWidth(), label = { Text("Hypothesis") }); OutlinedTextField(a, { a = it }, Modifier.fillMaxWidth(), label = { Text("Condition A") }); OutlinedTextField(b, { b = it }, Modifier.fillMaxWidth(), label = { Text("Condition B") })
                Button(enabled = hypothesis.isNotBlank() && a.isNotBlank() && b.isNotBlank(), onClick = { onAdd(ExperimentEntity(hypothesis = hypothesis.trim(), conditionA = a.trim(), conditionB = b.trim())); hypothesis = ""; a = ""; b = ""; adding = false }, modifier = Modifier.fillMaxWidth()) { Text("Create experiment") }
            }
        }
        if (experiments.isEmpty()) EmptyCard("No active experiments", "Try questions like “Do 5-minute walks improve my next focus block?” or “Does AI help earlier reduce fatigue?”")
        experiments.forEach { experiment ->
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(experiment.hypothesis, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) { ConditionCard("A", experiment.conditionA, Modifier.weight(1f)); ConditionCard("B", experiment.conditionB, Modifier.weight(1f)) }
                    Text("Randomized assignment and outcome linkage use your next check-ins; results stay local.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { onComplete(experiment.id) }) { Text("Mark complete") }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(probe: HealthConnectProbe, notify: (String) -> Unit) {
    val context = LocalContext.current; val scope = rememberCoroutineScope(); var probeResults by remember { mutableStateOf<List<ProbeResult>>(emptyList()) }; var probing by remember { mutableStateOf(false) }; var permissionRefresh by remember { mutableStateOf(0) }
    val usage = remember(permissionRefresh) { AttentionAccess.hasUsageAccess(context) }; val notifications = remember(permissionRefresh) { AttentionAccess.hasNotificationListenerAccess(context) }
    val healthPermissionLauncher = rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) { granted -> notify(if (granted.containsAll(probe.permissions)) "Health Connect connected" else "Some Health Connect permissions remain off") }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> if (granted) { CheckInReminderScheduler.enable(context); notify("Check-in reminders enabled") } else notify("Notification permission not granted") }
    ScreenColumn {
        ScreenHeader("Settings", "Privacy, sensing, reminders and diagnostics.")
        SettingsCard("Attention sensing", "Only aggregate counts are stored — never notification text or app content.") {
            PermissionRow("Usage access", usage) { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }; HorizontalDivider(); PermissionRow("Notification access", notifications) { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }; TextButton(onClick = { permissionRefresh++ }) { Text("Refresh status") }
        }
        SettingsCard("Health Connect", "Heart rate, sleep, steps and SpO₂ can provide context around your check-ins.") {
            Button(onClick = { healthPermissionLauncher.launch(probe.permissions) }) { Text("Manage health access") }
            OutlinedButton(onClick = { scope.launch { probing = true; probeResults = probe.probe(); probing = false } }) { Text(if (probing) "Checking…" else "Run diagnostics") }
            if (probeResults.isNotEmpty()) probeResults.forEach { HealthResultRow(it) }
        }
        SettingsCard("Check-in reminders", "Optional reminders run roughly every four hours between 08:00 and 21:59.") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { if (Build.VERSION.SDK_INT >= 33) notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) else { CheckInReminderScheduler.enable(context); notify("Check-in reminders enabled") } }) { Text("Enable") }
                OutlinedButton(onClick = { CheckInReminderScheduler.disable(context); notify("Check-in reminders disabled") }) { Text("Disable") }
            }
        }
        SettingsCard("Privacy", "CMF Flow is local-first. Health and attention context stay on this device in the current MVP.") { Text("No cloud account required", color = MaterialTheme.colorScheme.primary); Text("No notification content stored", color = MaterialTheme.colorScheme.primary); Text("No raw app-switch history persisted", color = MaterialTheme.colorScheme.primary) }
    }
}

@Composable
private fun CheckInCard(onSave: suspend (SelfReportEntity) -> Unit) {
    val scope = rememberCoroutineScope(); var flow by remember { mutableStateOf(3) }; var absorption by remember { mutableStateOf(3) }; var effortless by remember { mutableStateOf(3) }; var reward by remember { mutableStateOf(3) }; var presence by remember { mutableStateOf(3) }; var fatigue by remember { mutableStateOf(2) }; var activity by remember { mutableStateOf("") }; var domain by remember { mutableStateOf("") }; var more by remember { mutableStateOf(false) }; var difficulty by remember { mutableStateOf(3) }; var clarity by remember { mutableStateOf(3) }; var control by remember { mutableStateOf(3) }
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("How are you doing?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold); Text("About 20 seconds. This is the ground truth Flow learns from.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Stepper("Flow", flow) { flow = it }; Stepper("Absorption", absorption) { absorption = it }; Stepper("Effortless control", effortless) { effortless = it }; Stepper("Enjoyment", reward) { reward = it }; Stepper("Presence", presence) { presence = it }; Stepper("Fatigue", fatigue) { fatigue = it }
            OutlinedTextField(activity, { activity = it }, Modifier.fillMaxWidth(), label = { Text("What are you doing? (optional)") }, singleLine = true); OutlinedTextField(domain, { domain = it }, Modifier.fillMaxWidth(), label = { Text("Area of life (optional)") }, singleLine = true)
            Row(verticalAlignment = Alignment.CenterVertically) { Switch(more, { more = it }); Spacer(Modifier.size(8.dp)); Text("More context") }
            if (more) { Stepper("Difficulty", difficulty) { difficulty = it }; Stepper("Goal clarity", clarity) { clarity = it }; Stepper("Control", control) { control = it } }
            Button(onClick = { scope.launch { onSave(SelfReportEntity(capturedAtEpochMs = System.currentTimeMillis(), flowScore = flow, absorption = absorption, effortlessControl = effortless, intrinsicReward = reward, presence = presence, fatigue = fatigue, activityLabel = activity.trim().ifBlank { null }, domain = domain.trim().ifBlank { null }, taskDifficulty = difficulty.takeIf { more }, goalClarity = clarity.takeIf { more }, perceivedControl = control.takeIf { more }, notes = null)) } }, modifier = Modifier.fillMaxWidth()) { Text("Save check-in") }
        }
    }
}

@Composable
private fun Stepper(label: String, value: Int, onChange: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) { Text(label, Modifier.weight(1f)); Text("$value / 5", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold) }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            (1..5).forEach { n ->
                val selected = value == n
                Surface(onClick = { onChange(n) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant, contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant) { Box(Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) { Text(n.toString(), fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) } }
            }
        }
    }
}

@Composable
private fun Sparkline(values: List<Float>) {
    val lineColor = MaterialTheme.colorScheme.primary; val gridColor = MaterialTheme.colorScheme.outlineVariant
    Canvas(Modifier.fillMaxWidth().height(130.dp)) {
        if (values.size < 2) return@Canvas
        val step = size.width / (values.size - 1).coerceAtLeast(1); val path = Path()
        values.forEachIndexed { index, raw -> val value = raw.coerceIn(0f, 5f); val x = index * step; val y = size.height - (value / 5f) * size.height; if (index == 0) path.moveTo(x, y) else path.lineTo(x, y) }
        drawLine(gridColor, Offset(0f, size.height * .5f), Offset(size.width, size.height * .5f), strokeWidth = 1f); drawPath(path, lineColor, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 5f))
    }
}

@Composable private fun InsightMetric(label: String, value: String, modifier: Modifier = Modifier) { Card(modifier, shape = RoundedCornerShape(20.dp)) { Column(Modifier.padding(14.dp)) { Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall); Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) } } }
@Composable private fun Metric(label: String, value: Int?) { Column { Text(label, style = MaterialTheme.typography.bodySmall); Text(value?.toString() ?: "—", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) } }
@Composable private fun ConditionCard(label: String, text: String, modifier: Modifier) { Surface(modifier, shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceVariant) { Column(Modifier.padding(14.dp)) { Text(label, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold); Text(text) } } }

@Composable
private fun SettingsCard(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold); Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant); content() } }
}

@Composable private fun PermissionRow(label: String, granted: Boolean, onClick: () -> Unit) { Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) { Icon(if (granted) Icons.Filled.CheckCircle else Icons.Filled.Settings, null, tint = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.size(10.dp)); Column(Modifier.weight(1f)) { Text(label, fontWeight = FontWeight.SemiBold); Text(if (granted) "Enabled" else "Not enabled", color = MaterialTheme.colorScheme.onSurfaceVariant) }; TextButton(onClick = onClick) { Text(if (granted) "Manage" else "Enable") } } }
@Composable private fun HealthResultRow(result: ProbeResult) { val ok = result.error == null && result.recordCount > 0; Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) { Icon(if (ok) Icons.Filled.CheckCircle else Icons.Filled.Favorite, null, tint = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.size(10.dp)); Column(Modifier.weight(1f)) { Text(result.type.replace('_', ' ').replaceFirstChar { it.uppercase() }, fontWeight = FontWeight.SemiBold); Text(if (result.error != null) "Unavailable" else "${result.recordCount} records · ${formatEpochShort(result.latestEpochMs)}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) } } }
@Composable private fun EmptyCard(title: String, body: String) { Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) { Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Filled.Science, null, modifier = Modifier.size(34.dp), tint = MaterialTheme.colorScheme.primary); Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold); Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
@Composable private fun ScreenColumn(content: @Composable ColumnScope.() -> Unit) { Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp), content = content) }
@Composable private fun ScreenHeader(title: String, subtitle: String) { Column(verticalArrangement = Arrangement.spacedBy(3.dp)) { Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
@Composable private fun SectionTitle(text: String) { Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }

private fun tabIcon(tab: AppTab): ImageVector = when (tab) { AppTab.Home -> Icons.Filled.Home; AppTab.Insights -> Icons.Filled.BarChart; AppTab.Tasks -> Icons.Filled.TaskAlt; AppTab.Experiments -> Icons.Filled.Science; AppTab.Settings -> Icons.Filled.Settings }
private fun greeting(): String = when (java.time.LocalTime.now().hour) { in 5..11 -> "Good morning"; in 12..17 -> "Good afternoon"; else -> "Good evening" }
private fun stateLabel(report: SelfReportEntity?): String { if (report == null) return "Ready to learn you"; return when { report.fatigue >= 4 -> "Recovery mode"; report.flowScore >= 4 && report.presence >= 4 -> "Flow is available"; report.presence <= 2 -> "Attention is scattered"; else -> "Balanced state" } }
private fun friendlyAction(action: String): String = when (action) { "CONTINUE" -> "Keep going"; "SWITCH_TASK" -> "Switch the task"; "REDUCE_DIFFICULTY" -> "Make it easier"; "ASK_AI" -> "Get AI help"; "TAKE_BREAK" -> "Take a short break"; "EXERCISE" -> "Move for a few minutes"; "STOP" -> "Call it for now"; "REDUCE_INTERRUPTION" -> "Protect your attention"; else -> action.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() } }
private fun friendlyReason(reasons: List<String>): String { if (reasons.isEmpty()) return "Based on your latest state and what Flow has learned so far."; val raw = reasons.first().replace('_', ' ').lowercase(); return raw.replaceFirstChar { it.uppercase() } + "." }
private fun contextSummary(context: ContextSnapshotEntity): String { val parts = mutableListOf<String>(); context.sleepMinutesPrevious24h?.let { parts += "${it / 60}h sleep" }; context.stepCount?.let { parts += "$it recent steps" }; context.appSwitchCount?.let { parts += "$it app switches" }; context.notificationCount?.let { parts += "$it notifications" }; return if (parts.isEmpty()) "Context captured; richer signals are still accumulating." else parts.joinToString(" · ") }
private fun formatScore(value: Double?): String = value?.let { "%.1f".format(it) } ?: "—"
private fun formatEpochShort(epochMs: Long?): String { if (epochMs == null) return "no recent data"; return DateTimeFormatter.ofPattern("MMM d, HH:mm").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(epochMs)) }
