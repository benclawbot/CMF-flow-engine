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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

private enum class AppTab(val label: String) { Home("Home"), Insights("Insights"), Tasks("Tasks"), Experiments("Lab"), Settings("Settings") }

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
    val ranked = rankTasks(tasks, latest, reports, contexts.firstOrNull(), contexts, recommendations)
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
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 10.dp) {
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
                    latest, session, sessionState.minutesOnCurrentTask,
                    friendlyAction(intervention.action.name), friendlyReason(intervention.reasons), topTask?.task,
                    saveReport,
                    onDoIntervention = { scope.launch { interventionEventId?.let { respondIntervention(it, "accepted") }; notify("Action accepted") } },
                    onDismissIntervention = { scope.launch { interventionEventId?.let { respondIntervention(it, "dismissed") } } },
                    onStartTask = {
                        topTask?.task?.let { task -> scope.launch {
                            recommendationEventId?.let { respondRecommendation(it, "accepted") }
                            if (session == null) startSession(SessionEntity(taskId = task.id, taskTitle = task.title, taskDomain = task.domain, startedAtEpochMs = System.currentTimeMillis()))
                            notify("Focus session started")
                        } }
                    },
                    onOpenTasks = { tab = AppTab.Tasks },
                    onEndSession = { id -> scope.launch { endSession(id); notify("Session ended") } },
                    onStruggle = { id -> scope.launch { markStruggle(id); notify("Struggle noted") } },
                )
                AppTab.Insights -> InsightsScreen(reports, contexts)
                AppTab.Tasks -> TasksScreen(
                    ranked.map { it.task to it.reasons },
                    onAdd = { task -> scope.launch { addTask(task); notify("Task added") } },
                    onDone = { id -> scope.launch { completeTask(id); notify("Task completed") } },
                    onStart = { task -> scope.launch { if (session == null) startSession(SessionEntity(taskId = task.id, taskTitle = task.title, taskDomain = task.domain, startedAtEpochMs = System.currentTimeMillis())); tab = AppTab.Home; notify("Focus session started") } },
                )
                AppTab.Experiments -> ExperimentsScreen(experiments, { exp -> scope.launch { addExperiment(exp); notify("Experiment created") } }, { id -> scope.launch { completeExperiment(id); notify("Experiment completed") } })
                AppTab.Settings -> SettingsScreen(probe, notify)
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
        BrandHeader(greeting(), "Your day, tuned for flow")
        FlowHero(latest, session, sessionMinutes, onStruggle, onEndSession)

        SectionTitle("Right now", "One move with the highest upside")
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(30.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        ) {
            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(42.dp)) {
                    Box(contentAlignment = Alignment.Center) { Text("✦", color = MaterialTheme.colorScheme.onPrimary) }
                }
                Text(interventionTitle, style = MaterialTheme.typography.headlineSmall)
                Text(interventionReason, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onDoIntervention) { Text("Do it") }
                    TextButton(onClick = onDismissIntervention) { Text("Not now") }
                }
            }
        }

        SectionTitle("Best next task", "Chosen for this state")
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(30.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (topTask == null) {
                    EmptyInline("Nothing queued", "Add a few tasks and Flow will pick the one that fits your energy and attention.")
                    OutlinedButton(onClick = onOpenTasks) { Text("Add tasks") }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(54.dp)) {
                            Box(contentAlignment = Alignment.Center) { Icon(Icons.Filled.TaskAlt, null, tint = MaterialTheme.colorScheme.primary) }
                        }
                        Spacer(Modifier.size(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(topTask.title, style = MaterialTheme.typography.titleLarge)
                            Text("${topTask.domain.replaceFirstChar { it.uppercase() }} · ${topTask.estimatedMinutes} min", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Button(onClick = onStartTask, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Filled.PlayArrow, null); Spacer(Modifier.size(6.dp)); Text("Start focus") }
                }
            }
        }

        Button(onClick = { checkIn = !checkIn }, modifier = Modifier.fillMaxWidth()) { Text(if (checkIn) "Close check-in" else "Quick check-in") }
        if (checkIn) CheckInCard { report -> onCheckIn(report); checkIn = false }
    }
}

@Composable
private fun FlowHero(latest: SelfReportEntity?, session: SessionEntity?, sessionMinutes: Int?, onStruggle: (Long) -> Unit, onEndSession: (Long) -> Unit) {
    val gradient = Brush.linearGradient(listOf(Color(0xFF5D3BE8), Color(0xFF8A6BFF), Color(0xFFC2B3FF)))
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(34.dp), color = Color.Transparent) {
        Row(
            modifier = Modifier.background(gradient).padding(22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            FlowRing(if (session != null) ((sessionMinutes ?: 0) / 60f).coerceIn(0f, 1f) else ((latest?.flowScore ?: 0) / 5f), if (session != null) "${sessionMinutes ?: 0}" else (latest?.flowScore?.toString() ?: "—"), if (session != null) "MIN" else "FLOW")
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (session != null) "CURRENT FOCUS" else "YOUR STATE", color = Color.White.copy(alpha = .72f), style = MaterialTheme.typography.labelLarge)
                Text(if (session != null) session.taskTitle ?: "Focus session" else stateLabel(latest), color = Color.White, style = MaterialTheme.typography.headlineSmall)
                if (session != null) {
                    Text("${session.struggleCount} struggle marks", color = Color.White.copy(alpha = .8f))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = { onStruggle(session.id) }) { Text("I'm stuck") }
                        TextButton(onClick = { onEndSession(session.id) }) { Text("Finish", color = Color.White) }
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MiniStat("Presence", latest?.presence)
                        MiniStat("Fatigue", latest?.fatigue)
                    }
                }
            }
        }
    }
}

@Composable
private fun FlowRing(progress: Float, value: String, label: String) {
    Box(Modifier.size(104.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawArc(Color.White.copy(alpha = .22f), -90f, 360f, false, style = Stroke(10f, cap = StrokeCap.Round))
            drawArc(Color.White, -90f, progress.coerceIn(0f, 1f) * 360f, false, style = Stroke(10f, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = Color.White, style = MaterialTheme.typography.headlineMedium)
            Text(label, color = Color.White.copy(alpha = .75f), style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun MiniStat(label: String, value: Int?) {
    Surface(shape = RoundedCornerShape(14.dp), color = Color.White.copy(alpha = .14f)) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(label, color = Color.White.copy(alpha = .72f), style = MaterialTheme.typography.bodySmall)
            Text(value?.toString() ?: "—", color = Color.White, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun InsightsScreen(reports: List<SelfReportEntity>, contexts: List<ContextSnapshotEntity>) {
    val summary = summarize(reports)
    ScreenColumn {
        BrandHeader("Insights", "Your personal flow map")
        if (reports.isEmpty()) {
            EmptyCard("No insights yet", "A few quick check-ins will turn this into your personal flow map.")
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                InsightMetric("Flow", formatScore(summary.averageFlow), Color(0xFFE9E1FF), Modifier.weight(1f))
                InsightMetric("Presence", formatScore(summary.averagePresence), Color(0xFFE5F4EC), Modifier.weight(1f))
                InsightMetric("Fatigue", formatScore(summary.averageFatigue), Color(0xFFFFE8E8), Modifier.weight(1f))
            }
            ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(30.dp)) {
                Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Flow over time", style = MaterialTheme.typography.titleLarge)
                    Text("Last ${reports.size.coerceAtMost(20)} check-ins", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Sparkline(reports.take(20).reversed().map { it.flowScore.toFloat() })
                }
            }
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(30.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("What Flow is learning", style = MaterialTheme.typography.titleLarge)
                    summary.strongestDomain?.let { Text("You currently report your strongest flow in ${it.replaceFirstChar { c -> c.uppercase() }}.") }
                        ?: Text("Domain patterns unlock after at least 3 comparable check-ins.")
                    contexts.firstOrNull()?.let { Text(contextSummary(it), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = Color.White) {
                Row(Modifier.padding(18.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(42.dp)) { Box(contentAlignment = Alignment.Center) { Text("✓", color = MaterialTheme.colorScheme.primary) } }
                    Column { Text("Evidence-aware", fontWeight = FontWeight.SemiBold); Text("Patterns only influence recommendations after minimum sample thresholds.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
        }
    }
}

@Composable
private fun TasksScreen(rankedTasks: List<Pair<TaskEntity, List<String>>>, onAdd: (TaskEntity) -> Unit, onDone: (Long) -> Unit, onStart: (TaskEntity) -> Unit) {
    var adding by remember { mutableStateOf(false) }; var title by remember { mutableStateOf("") }; var domain by remember { mutableStateOf("work") }; var value by remember { mutableStateOf(3) }; var urgency by remember { mutableStateOf(3) }; var difficulty by remember { mutableStateOf(3) }; var minutes by remember { mutableStateOf("30") }
    ScreenColumn {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) { BrandHeader("Tasks", "Ranked for this version of you") }
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) { IconButton(onClick = { adding = !adding }) { Icon(Icons.Filled.Add, "Add task", tint = MaterialTheme.colorScheme.primary) } }
        }
        if (adding) ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(30.dp)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Add something worth doing", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), label = { Text("Task") }, singleLine = true)
                OutlinedTextField(domain, { domain = it }, Modifier.fillMaxWidth(), label = { Text("Area of life") }, singleLine = true)
                Stepper("Value", value) { value = it }; Stepper("Urgency", urgency) { urgency = it }; Stepper("Difficulty", difficulty) { difficulty = it }
                OutlinedTextField(minutes, { minutes = it.filter(Char::isDigit) }, Modifier.fillMaxWidth(), label = { Text("Minutes") }, singleLine = true)
                Button(enabled = title.isNotBlank(), onClick = { onAdd(TaskEntity(title = title.trim(), domain = domain.trim().ifBlank { "other" }, valueScore = value, urgencyScore = urgency, difficultyScore = difficulty, estimatedMinutes = minutes.toIntOrNull()?.coerceAtLeast(1) ?: 30)); title = ""; adding = false }, modifier = Modifier.fillMaxWidth()) { Text("Add task") }
            }
        }
        if (rankedTasks.isEmpty()) EmptyCard("Your task list is clear", "Add something when you want help choosing the right next move.")
        rankedTasks.forEachIndexed { index, (task, reasons) ->
            ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(26.dp)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = if (index == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(42.dp)) { Box(contentAlignment = Alignment.Center) { Text("${index + 1}", color = if (index == 0) Color.White else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) } }
                        Spacer(Modifier.size(12.dp))
                        Column(Modifier.weight(1f)) { Text(task.title, style = MaterialTheme.typography.titleMedium); Text("${task.domain.replaceFirstChar { it.uppercase() }} · ${task.estimatedMinutes} min", color = MaterialTheme.colorScheme.onSurfaceVariant) }
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
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) { BrandHeader("Flow Lab", "Small experiments, personal evidence") }
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) { IconButton(onClick = { adding = !adding }) { Icon(Icons.Filled.Add, "Add experiment", tint = MaterialTheme.colorScheme.primary) } }
        }
        Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
            Column(Modifier.padding(20.dp)) { Text("Ask one useful question", style = MaterialTheme.typography.titleLarge); Text("Compare two conditions and let your check-ins provide the outcome signal.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        if (adding) ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(hypothesis, { hypothesis = it }, Modifier.fillMaxWidth(), label = { Text("What do you want to learn?") })
                OutlinedTextField(a, { a = it }, Modifier.fillMaxWidth(), label = { Text("Condition A") })
                OutlinedTextField(b, { b = it }, Modifier.fillMaxWidth(), label = { Text("Condition B") })
                Button(enabled = hypothesis.isNotBlank() && a.isNotBlank() && b.isNotBlank(), onClick = { onAdd(ExperimentEntity(hypothesis = hypothesis.trim(), conditionA = a.trim(), conditionB = b.trim())); hypothesis = ""; a = ""; b = ""; adding = false }, modifier = Modifier.fillMaxWidth()) { Text("Create experiment") }
            }
        }
        if (experiments.isEmpty()) EmptyCard("No active experiments", "Try “5-minute walk vs no walk” or “AI help early vs only when stuck.”")
        experiments.forEach { experiment ->
            ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(experiment.hypothesis, style = MaterialTheme.typography.titleLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        ConditionCard("A", experiment.conditionA, Color(0xFFE9E1FF), Modifier.weight(1f))
                        ConditionCard("B", experiment.conditionB, Color(0xFFFFE6F3), Modifier.weight(1f))
                    }
                    Text("Your next check-ins become the outcome signal. Results stay local.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
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
        BrandHeader("Settings", "Connected, private, under your control")
        SettingsCard("Attention sensing", "Aggregate counts only — never notification text or app content.") {
            PermissionRow("Usage access", usage) { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
            HorizontalDivider(); PermissionRow("Notification access", notifications) { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
            TextButton(onClick = { permissionRefresh++ }) { Text("Refresh status") }
        }
        SettingsCard("Health Connect", "Heart rate, sleep, steps and SpO₂ add context around your check-ins.") {
            Button(onClick = { healthPermissionLauncher.launch(probe.permissions) }) { Text("Manage health access") }
            OutlinedButton(onClick = { scope.launch { probing = true; probeResults = probe.probe(); probing = false } }) { Text(if (probing) "Checking…" else "Run diagnostics") }
            probeResults.forEach { HealthResultRow(it) }
        }
        SettingsCard("Check-in reminders", "Optional reminders roughly every four hours between 08:00 and 21:59.") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { if (Build.VERSION.SDK_INT >= 33) notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) else { CheckInReminderScheduler.enable(context); notify("Check-in reminders enabled") } }) { Text("Enable") }
                OutlinedButton(onClick = { CheckInReminderScheduler.disable(context); notify("Check-in reminders disabled") }) { Text("Disable") }
            }
        }
        Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(30.dp), color = MaterialTheme.colorScheme.primaryContainer) {
            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Local-first by design", style = MaterialTheme.typography.titleLarge)
                Text("No cloud account required · No notification content stored · No raw app-switch history persisted", color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
    }
}

@Composable
private fun CheckInCard(onSave: suspend (SelfReportEntity) -> Unit) {
    val scope = rememberCoroutineScope(); var flow by remember { mutableStateOf(3) }; var absorption by remember { mutableStateOf(3) }; var effortless by remember { mutableStateOf(3) }; var reward by remember { mutableStateOf(3) }; var presence by remember { mutableStateOf(3) }; var fatigue by remember { mutableStateOf(2) }; var activity by remember { mutableStateOf("") }; var domain by remember { mutableStateOf("") }; var more by remember { mutableStateOf(false) }; var difficulty by remember { mutableStateOf(3) }; var clarity by remember { mutableStateOf(3) }; var control by remember { mutableStateOf(3) }
    ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(30.dp)) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("How are you doing?", style = MaterialTheme.typography.headlineSmall)
            Text("About 20 seconds. This is the signal Flow learns from.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Stepper("Flow", flow) { flow = it }; Stepper("Absorption", absorption) { absorption = it }; Stepper("Effortless control", effortless) { effortless = it }; Stepper("Enjoyment", reward) { reward = it }; Stepper("Presence", presence) { presence = it }; Stepper("Fatigue", fatigue) { fatigue = it }
            OutlinedTextField(activity, { activity = it }, Modifier.fillMaxWidth(), label = { Text("What are you doing? (optional)") }, singleLine = true)
            OutlinedTextField(domain, { domain = it }, Modifier.fillMaxWidth(), label = { Text("Area of life (optional)") }, singleLine = true)
            Row(verticalAlignment = Alignment.CenterVertically) { Switch(more, { more = it }); Spacer(Modifier.size(8.dp)); Text("More context") }
            if (more) { Stepper("Difficulty", difficulty) { difficulty = it }; Stepper("Goal clarity", clarity) { clarity = it }; Stepper("Control", control) { control = it } }
            Button(onClick = { scope.launch { onSave(SelfReportEntity(capturedAtEpochMs = System.currentTimeMillis(), flowScore = flow, absorption = absorption, effortlessControl = effortless, intrinsicReward = reward, presence = presence, fatigue = fatigue, activityLabel = activity.trim().ifBlank { null }, domain = domain.trim().ifBlank { null }, taskDifficulty = difficulty.takeIf { more }, goalClarity = clarity.takeIf { more }, perceivedControl = control.takeIf { more }, notes = null)) } }, modifier = Modifier.fillMaxWidth()) { Text("Save check-in") }
        }
    }
}

@Composable
private fun Stepper(label: String, value: Int, onChange: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) { Text(label, Modifier.weight(1f)); Text("$value / 5", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold) }
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
            (1..5).forEach { n ->
                val selected = value == n
                Surface(
                    onClick = { onChange(n) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp),
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                ) { Box(Modifier.padding(vertical = 11.dp), contentAlignment = Alignment.Center) { Text(n.toString(), fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) } }
            }
        }
    }
}

@Composable
private fun Sparkline(values: List<Float>) {
    val lineColor = MaterialTheme.colorScheme.primary
    val fillColor = MaterialTheme.colorScheme.primaryContainer
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    Canvas(Modifier.fillMaxWidth().height(160.dp)) {
        if (values.size < 2) return@Canvas
        val step = size.width / (values.size - 1).coerceAtLeast(1)
        val path = Path(); val fill = Path()
        values.forEachIndexed { index, raw ->
            val value = raw.coerceIn(0f, 5f); val x = index * step; val y = size.height - (value / 5f) * size.height
            if (index == 0) { path.moveTo(x, y); fill.moveTo(x, size.height); fill.lineTo(x, y) } else { path.lineTo(x, y); fill.lineTo(x, y) }
        }
        fill.lineTo(size.width, size.height); fill.close()
        drawPath(fill, fillColor.copy(alpha = .6f))
        drawLine(gridColor, Offset(0f, size.height * .5f), Offset(size.width, size.height * .5f), strokeWidth = 1f)
        drawPath(path, lineColor, style = Stroke(width = 6f, cap = StrokeCap.Round))
        values.forEachIndexed { index, raw -> val x = index * step; val y = size.height - (raw.coerceIn(0f, 5f) / 5f) * size.height; drawCircle(Color.White, 7f, Offset(x, y)); drawCircle(lineColor, 4f, Offset(x, y)) }
    }
}

@Composable private fun InsightMetric(label: String, value: String, color: Color, modifier: Modifier = Modifier) { Surface(modifier, shape = RoundedCornerShape(22.dp), color = color) { Column(Modifier.padding(16.dp)) { Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall); Text(value, style = MaterialTheme.typography.headlineSmall) } } }
@Composable private fun ConditionCard(label: String, text: String, color: Color, modifier: Modifier) { Surface(modifier, shape = RoundedCornerShape(20.dp), color = color) { Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) { Text(label, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold); Text(text) } } }

@Composable
private fun SettingsCard(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp)) { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { Text(title, style = MaterialTheme.typography.titleLarge); Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant); content() } }
}

@Composable private fun PermissionRow(label: String, granted: Boolean, onClick: () -> Unit) { Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) { Surface(shape = CircleShape, color = if (granted) Color(0xFFE5F4EC) else MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(40.dp)) { Box(contentAlignment = Alignment.Center) { Icon(if (granted) Icons.Filled.CheckCircle else Icons.Filled.Settings, null, tint = if (granted) Color(0xFF238257) else MaterialTheme.colorScheme.onSurfaceVariant) } }; Spacer(Modifier.size(12.dp)); Column(Modifier.weight(1f)) { Text(label, fontWeight = FontWeight.SemiBold); Text(if (granted) "Enabled" else "Not enabled", color = MaterialTheme.colorScheme.onSurfaceVariant) }; TextButton(onClick = onClick) { Text(if (granted) "Manage" else "Enable") } } }
@Composable private fun HealthResultRow(result: ProbeResult) { val ok = result.error == null && result.recordCount > 0; Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) { Icon(if (ok) Icons.Filled.CheckCircle else Icons.Filled.Favorite, null, tint = if (ok) Color(0xFF238257) else MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.size(10.dp)); Column(Modifier.weight(1f)) { Text(result.type.replace('_', ' ').replaceFirstChar { it.uppercase() }, fontWeight = FontWeight.SemiBold); Text(if (result.error != null) "Unavailable" else "${result.recordCount} records · ${formatEpochShort(result.latestEpochMs)}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) } } }
@Composable private fun EmptyCard(title: String, body: String) { Surface(Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(28.dp)), shape = RoundedCornerShape(28.dp), color = Color.White) { Column(Modifier.padding(26.dp), verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) { Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(52.dp)) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Filled.Science, null, tint = MaterialTheme.colorScheme.primary) } }; Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center); Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center) } } }
@Composable private fun EmptyInline(title: String, body: String) { Column(verticalArrangement = Arrangement.spacedBy(5.dp)) { Text(title, style = MaterialTheme.typography.titleLarge); Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
@Composable private fun ScreenColumn(content: @Composable ColumnScope.() -> Unit) { Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 22.dp), verticalArrangement = Arrangement.spacedBy(16.dp), content = content) }
@Composable private fun BrandHeader(title: String, subtitle: String) { Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { Text(title, style = MaterialTheme.typography.headlineLarge); Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge) } }
@Composable private fun SectionTitle(title: String, subtitle: String) { Column(verticalArrangement = Arrangement.spacedBy(2.dp)) { Text(title, style = MaterialTheme.typography.titleLarge); Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) } }

private fun tabIcon(tab: AppTab): ImageVector = when (tab) { AppTab.Home -> Icons.Filled.Home; AppTab.Insights -> Icons.Filled.BarChart; AppTab.Tasks -> Icons.Filled.TaskAlt; AppTab.Experiments -> Icons.Filled.Science; AppTab.Settings -> Icons.Filled.Settings }
private fun greeting(): String = when (java.time.LocalTime.now().hour) { in 5..11 -> "Good morning"; in 12..17 -> "Good afternoon"; else -> "Good evening" }
private fun stateLabel(report: SelfReportEntity?): String { if (report == null) return "Ready to learn you"; return when { report.fatigue >= 4 -> "Recovery mode"; report.flowScore >= 4 && report.presence >= 4 -> "Flow is available"; report.presence <= 2 -> "Attention is scattered"; else -> "Balanced state" } }
private fun friendlyAction(action: String): String = when (action) { "CONTINUE" -> "Keep going"; "SWITCH_TASK" -> "Switch the task"; "REDUCE_DIFFICULTY" -> "Make it easier"; "ASK_AI" -> "Get AI help"; "TAKE_BREAK" -> "Take a short break"; "EXERCISE" -> "Move for a few minutes"; "STOP" -> "Call it for now"; "REDUCE_INTERRUPTION" -> "Protect your attention"; else -> action.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() } }
private fun friendlyReason(reasons: List<String>): String { if (reasons.isEmpty()) return "Based on your latest state and what Flow has learned so far."; val raw = reasons.first().replace('_', ' ').lowercase(); return raw.replaceFirstChar { it.uppercase() } + "." }
private fun contextSummary(context: ContextSnapshotEntity): String { val parts = mutableListOf<String>(); context.sleepMinutesPrevious24h?.let { parts += "${it / 60}h sleep" }; context.stepCount?.let { parts += "$it recent steps" }; context.appSwitchCount?.let { parts += "$it app switches" }; context.notificationCount?.let { parts += "$it notifications" }; return if (parts.isEmpty()) "Context captured; richer signals are still accumulating." else parts.joinToString(" · ") }
private fun formatScore(value: Double?): String = value?.let { "%.1f".format(it) } ?: "—"
private fun formatEpochShort(epochMs: Long?): String { if (epochMs == null) return "no recent data"; return DateTimeFormatter.ofPattern("MMM d, HH:mm").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(epochMs)) }
