package com.benclawbot.cmfflow

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import com.benclawbot.cmfflow.analytics.summarize
import com.benclawbot.cmfflow.attention.AttentionAccess
import com.benclawbot.cmfflow.data.ContextSnapshotEntity
import com.benclawbot.cmfflow.data.ExperimentAssignmentEntity
import com.benclawbot.cmfflow.data.ExperimentEntity
import com.benclawbot.cmfflow.data.InterventionEventEntity
import com.benclawbot.cmfflow.data.RecommendationEventEntity
import com.benclawbot.cmfflow.data.SelfReportEntity
import com.benclawbot.cmfflow.data.SessionEntity
import com.benclawbot.cmfflow.data.TaskEntity
import com.benclawbot.cmfflow.experiments.analyzeExperiment
import com.benclawbot.cmfflow.experiments.chooseNextCondition
import com.benclawbot.cmfflow.health.HealthConnectProbe
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
import kotlin.random.Random

private enum class ProductTab(val label: String) {
    Home("Home"), Insights("Insights"), Tasks("Tasks"), Experiments("Experiments"), Settings("Settings")
}

@Composable
fun ProductApp(
    probe: HealthConnectProbe,
    reports: List<SelfReportEntity>,
    contexts: List<ContextSnapshotEntity>,
    recommendations: List<RecommendationEventEntity>,
    tasks: List<TaskEntity>,
    session: SessionEntity?,
    experiments: List<ExperimentEntity>,
    assignments: List<ExperimentAssignmentEntity>,
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
    assignExperiment: suspend (ExperimentAssignmentEntity) -> Long,
) {
    var tab by remember { mutableStateOf(ProductTab.Home) }
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
    val openTrial = assignments.firstOrNull { it.outcomeSelfReportId == null }
    val openTrialExperiment = experiments.firstOrNull { it.id == openTrial?.experimentId }

    var interventionEventId by remember { mutableStateOf<Long?>(null) }
    var recommendationEventId by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(intervention.action, intervention.reasons, session?.id, session?.struggleCount) {
        interventionEventId = recordIntervention(
            InterventionEventEntity(
                action = intervention.action.name,
                reasonsSnapshot = intervention.reasons.joinToString("|"),
            ),
        )
    }
    LaunchedEffect(topTask?.task?.id, topTask?.score) {
        recommendationEventId = topTask?.let {
            recordRecommendation(
                RecommendationEventEntity(
                    taskId = it.task.id,
                    taskTitle = it.task.title,
                    taskDomain = it.task.domain,
                    score = it.score,
                    reasonsSnapshot = it.reasons.joinToString("|"),
                ),
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                ProductTab.entries.forEach { item ->
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = { tab = item },
                        icon = { Icon(productTabIcon(item), contentDescription = item.label) },
                        label = { Text(item.label, maxLines = 1) },
                    )
                }
            }
        },
    ) { inner ->
        Box(Modifier.fillMaxSize().padding(inner)) {
            when (tab) {
                ProductTab.Home -> ProductHomeScreen(
                    latest = latest,
                    session = session,
                    sessionMinutes = sessionState.minutesOnCurrentTask,
                    interventionTitle = friendlyAction(intervention.action.name),
                    interventionReason = friendlyReason(intervention.reasons),
                    topTask = topTask?.task,
                    openTrial = openTrial,
                    openTrialExperiment = openTrialExperiment,
                    onCheckIn = saveReport,
                    onDoIntervention = {
                        scope.launch {
                            interventionEventId?.let { respondIntervention(it, "accepted") }
                            notify("Action accepted")
                        }
                    },
                    onDismissIntervention = {
                        scope.launch { interventionEventId?.let { respondIntervention(it, "dismissed") } }
                    },
                    onStartTask = {
                        val task = topTask?.task ?: return@ProductHomeScreen
                        scope.launch {
                            recommendationEventId?.let { respondRecommendation(it, "accepted") }
                            if (session == null) {
                                startSession(
                                    SessionEntity(
                                        taskId = task.id,
                                        taskTitle = task.title,
                                        taskDomain = task.domain,
                                        startedAtEpochMs = System.currentTimeMillis(),
                                    ),
                                )
                            }
                            notify("Focus session started")
                        }
                    },
                    onOpenTasks = { tab = ProductTab.Tasks },
                    onEndSession = { id -> scope.launch { endSession(id); notify("Session ended") } },
                    onStruggle = { id -> scope.launch { markStruggle(id); notify("Struggle noted") } },
                )
                ProductTab.Insights -> ProductInsightsScreen(reports, contexts)
                ProductTab.Tasks -> ProductTasksScreen(
                    rankedTasks = ranked.map { it.task to it.reasons },
                    onAdd = { task -> scope.launch { addTask(task); notify("Task added") } },
                    onDone = { id -> scope.launch { completeTask(id); notify("Task completed") } },
                    onStart = { task ->
                        scope.launch {
                            if (session == null) {
                                startSession(
                                    SessionEntity(
                                        taskId = task.id,
                                        taskTitle = task.title,
                                        taskDomain = task.domain,
                                        startedAtEpochMs = System.currentTimeMillis(),
                                    ),
                                )
                            }
                            tab = ProductTab.Home
                            notify("Focus session started")
                        }
                    },
                )
                ProductTab.Experiments -> ProductExperimentsScreen(
                    experiments = experiments,
                    assignments = assignments,
                    reports = reports,
                    openTrial = openTrial,
                    onAdd = { exp -> scope.launch { addExperiment(exp); notify("Experiment created") } },
                    onComplete = { id -> scope.launch { completeExperiment(id); notify("Experiment completed") } },
                    onStartTrial = { experiment ->
                        scope.launch {
                            if (openTrial != null) {
                                notify("Finish the current trial with a check-in first")
                            } else {
                                val condition = chooseNextCondition(experiment, assignments, Random.nextBoolean())
                                assignExperiment(
                                    ExperimentAssignmentEntity(
                                        experimentId = experiment.id,
                                        assignedCondition = condition,
                                    ),
                                )
                                notify("Trial assigned: $condition")
                                tab = ProductTab.Home
                            }
                        }
                    },
                )
                ProductTab.Settings -> ProductSettingsScreen(probe, notify)
            }
        }
    }
}

@Composable
private fun ProductHomeScreen(
    latest: SelfReportEntity?,
    session: SessionEntity?,
    sessionMinutes: Int?,
    interventionTitle: String,
    interventionReason: String,
    topTask: TaskEntity?,
    openTrial: ExperimentAssignmentEntity?,
    openTrialExperiment: ExperimentEntity?,
    onCheckIn: suspend (SelfReportEntity) -> Unit,
    onDoIntervention: () -> Unit,
    onDismissIntervention: () -> Unit,
    onStartTask: () -> Unit,
    onOpenTasks: () -> Unit,
    onEndSession: (Long) -> Unit,
    onStruggle: (Long) -> Unit,
) {
    var checkIn by remember { mutableStateOf(latest == null) }
    val gradient = Brush.linearGradient(listOf(Color(0xFF5D3BE8), Color(0xFF9478FF)))
    ProductScreenColumn {
        Text(greeting(), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text("Shape the conditions for your next good hour.", color = MaterialTheme.colorScheme.onSurfaceVariant)

        Surface(shape = RoundedCornerShape(32.dp), color = Color.Transparent, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.background(gradient).padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StateRing(value = if (session != null) ((sessionMinutes ?: 0) / 60f).coerceIn(0f, 1f) else ((latest?.flowScore ?: 0) / 5f))
                    Spacer(Modifier.size(16.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(if (session != null) "IN FOCUS" else "CURRENT STATE", color = Color.White.copy(alpha = .7f), style = MaterialTheme.typography.labelMedium)
                        Text(if (session != null) session.taskTitle ?: "Focus session" else stateLabel(latest), color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text(if (session != null) "${sessionMinutes ?: 0} min · ${session.struggleCount} struggle marks" else "Flow adapts as you check in", color = Color.White.copy(alpha = .82f))
                    }
                }
                if (session != null) {
                    LinearProgressIndicator(progress = { ((sessionMinutes ?: 0) / 60f).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onStruggle(session.id) }) { Text("I'm stuck") }
                        TextButton(onClick = { onEndSession(session.id) }) { Text("Finish", color = Color.White) }
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        StateMetric("Flow", latest?.flowScore, Modifier.weight(1f))
                        StateMetric("Presence", latest?.presence, Modifier.weight(1f))
                        StateMetric("Fatigue", latest?.fatigue, Modifier.weight(1f))
                    }
                }
            }
        }

        if (openTrial != null && openTrialExperiment != null) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("ACTIVE EXPERIMENT", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
                    Text(openTrial.assignedCondition, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(openTrialExperiment.hypothesis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Do this condition now. Your next check-in completes the trial automatically.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        SectionTitle("Right now")
        ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(26.dp)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(interventionTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(interventionReason, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onDoIntervention) { Text("Do it") }
                    TextButton(onClick = onDismissIntervention) { Text("Not now") }
                }
            }
        }

        SectionTitle("Best next task")
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(26.dp)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (topTask == null) {
                    Text("Nothing queued", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("Add a few tasks and Flow will choose based on your current state.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedButton(onClick = onOpenTasks) { Text("Add tasks") }
                } else {
                    Text(topTask.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("${topTask.domain.replaceFirstChar { it.uppercase() }} · ${topTask.estimatedMinutes} min", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = onStartTask) {
                        Icon(Icons.Filled.PlayArrow, null)
                        Spacer(Modifier.size(6.dp))
                        Text("Start focus")
                    }
                }
            }
        }

        Button(onClick = { checkIn = !checkIn }, modifier = Modifier.fillMaxWidth()) {
            Text(if (checkIn) "Close check-in" else if (openTrial != null) "Complete trial check-in" else "Quick check-in")
        }
        if (checkIn) ProductCheckInCard(onSave = { report -> onCheckIn(report); checkIn = false })
    }
}

@Composable
private fun ProductInsightsScreen(reports: List<SelfReportEntity>, contexts: List<ContextSnapshotEntity>) {
    val summary = summarize(reports)
    ProductScreenColumn {
        ScreenHeader("Insights", "Your patterns, with early coincidences kept separate from stronger evidence.")
        if (reports.isEmpty()) {
            EmptyCard("No insights yet", "A few quick check-ins will turn this into your personal flow map.")
            return@ProductScreenColumn
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            InsightMetric("Flow", formatScore(summary.averageFlow), Color(0xFFE8DFFF), Modifier.weight(1f))
            InsightMetric("Presence", formatScore(summary.averagePresence), Color(0xFFDDF2EA), Modifier.weight(1f))
            InsightMetric("Fatigue", formatScore(summary.averageFatigue), Color(0xFFFFE6E1), Modifier.weight(1f))
        }

        ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(26.dp)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Recent flow", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Sparkline(reports.take(30).reversed().map { it.flowScore.toFloat() })
                Text("${reports.size.coerceAtMost(100)} recent local check-ins", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(26.dp)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("What Flow is learning", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                summary.strongestDomain?.let {
                    Text("Your strongest sampled area so far is ${it.replaceFirstChar(Char::uppercase)}.")
                } ?: Text("Domain patterns unlock after enough comparable check-ins.")
                contexts.firstOrNull()?.let {
                    HorizontalDivider()
                    Text("Latest context", style = MaterialTheme.typography.titleMedium)
                    Text(contextSummary(it), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(26.dp)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Evidence guardrails", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("Personalization only activates after minimum sample thresholds and learned adjustments are capped, so a few unusual days cannot take over your recommendations.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ProductTasksScreen(
    rankedTasks: List<Pair<TaskEntity, List<String>>>,
    onAdd: (TaskEntity) -> Unit,
    onDone: (Long) -> Unit,
    onStart: (TaskEntity) -> Unit,
) {
    var adding by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var domain by remember { mutableStateOf("work") }
    var value by remember { mutableStateOf(3) }
    var urgency by remember { mutableStateOf(3) }
    var difficulty by remember { mutableStateOf(3) }
    var minutes by remember { mutableStateOf("30") }

    ProductScreenColumn {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) { ScreenHeader("Tasks", "A state-aware queue, not a static to-do list.") }
            IconButton(onClick = { adding = !adding }) { Icon(Icons.Filled.Add, "Add task") }
        }
        if (adding) {
            ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(26.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), label = { Text("Task") }, singleLine = true)
                    OutlinedTextField(domain, { domain = it }, Modifier.fillMaxWidth(), label = { Text("Area of life") }, singleLine = true)
                    ScorePicker("Value", value) { value = it }
                    ScorePicker("Urgency", urgency) { urgency = it }
                    ScorePicker("Difficulty", difficulty) { difficulty = it }
                    OutlinedTextField(minutes, { minutes = it.filter(Char::isDigit) }, Modifier.fillMaxWidth(), label = { Text("Minutes") }, singleLine = true)
                    Button(
                        enabled = title.isNotBlank(),
                        onClick = {
                            onAdd(
                                TaskEntity(
                                    title = title.trim(),
                                    domain = domain.trim().ifBlank { "other" },
                                    valueScore = value,
                                    urgencyScore = urgency,
                                    difficultyScore = difficulty,
                                    estimatedMinutes = minutes.toIntOrNull()?.coerceAtLeast(1) ?: 30,
                                ),
                            )
                            title = ""
                            adding = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Add task") }
                }
            }
        }

        if (rankedTasks.isEmpty()) EmptyCard("Your queue is clear", "Add something when you want help choosing the right next move.")
        rankedTasks.forEachIndexed { index, (task, reasons) ->
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(42.dp)) {
                            Box(contentAlignment = Alignment.Center) { Text("${index + 1}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }
                        }
                        Spacer(Modifier.size(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(task.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("${task.domain} · ${task.estimatedMinutes} min", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Text(friendlyTaskReasons(reasons), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onStart(task) }) { Text("Focus") }
                        OutlinedButton(onClick = { onDone(task.id) }) { Text("Done") }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductExperimentsScreen(
    experiments: List<ExperimentEntity>,
    assignments: List<ExperimentAssignmentEntity>,
    reports: List<SelfReportEntity>,
    openTrial: ExperimentAssignmentEntity?,
    onAdd: (ExperimentEntity) -> Unit,
    onComplete: (Long) -> Unit,
    onStartTrial: (ExperimentEntity) -> Unit,
) {
    var adding by remember { mutableStateOf(false) }
    var hypothesis by remember { mutableStateOf("") }
    var conditionA by remember { mutableStateOf("") }
    var conditionB by remember { mutableStateOf("") }

    ProductScreenColumn {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) { ScreenHeader("Experiments", "Balanced randomized N-of-1 trials, linked to your next check-in.") }
            IconButton(onClick = { adding = !adding }) { Icon(Icons.Filled.Add, "Add experiment") }
        }

        if (openTrial != null) {
            ElevatedCard(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Trial in progress", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(openTrial.assignedCondition, style = MaterialTheme.typography.headlineSmall)
                    Text("Your next check-in records the outcome. Start another trial only after that.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        if (adding) {
            ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(26.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(hypothesis, { hypothesis = it }, Modifier.fillMaxWidth(), label = { Text("What do you want to test?") })
                    OutlinedTextField(conditionA, { conditionA = it }, Modifier.fillMaxWidth(), label = { Text("Condition A") })
                    OutlinedTextField(conditionB, { conditionB = it }, Modifier.fillMaxWidth(), label = { Text("Condition B") })
                    Button(
                        enabled = hypothesis.isNotBlank() && conditionA.isNotBlank() && conditionB.isNotBlank(),
                        onClick = {
                            onAdd(
                                ExperimentEntity(
                                    hypothesis = hypothesis.trim(),
                                    conditionA = conditionA.trim(),
                                    conditionB = conditionB.trim(),
                                ),
                            )
                            hypothesis = ""
                            conditionA = ""
                            conditionB = ""
                            adding = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Create experiment") }
                }
            }
        }

        if (experiments.isEmpty()) {
            EmptyCard("No active experiments", "Try “Does a 5-minute walk improve my next focus block?” or “Does asking AI earlier reduce fatigue?”")
        }

        experiments.forEach { experiment ->
            val result = analyzeExperiment(experiment, assignments, reports)
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(26.dp)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(experiment.hypothesis, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        ConditionResultCard("A", result.conditionA.condition, result.conditionA.completedTrials, result.conditionA.averageUtility, Modifier.weight(1f))
                        ConditionResultCard("B", result.conditionB.condition, result.conditionB.completedTrials, result.conditionB.averageUtility, Modifier.weight(1f))
                    }
                    Text(result.summary, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (result.evidenceReady && result.deltaAminusB != null) {
                        Text("Estimated utility difference A−B: ${"%.2f".format(result.deltaAminusB)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                    Button(enabled = openTrial == null, onClick = { onStartTrial(experiment) }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (openTrial == null) "Start randomized trial" else "Finish current trial first")
                    }
                    TextButton(onClick = { onComplete(experiment.id) }) { Text("Archive experiment") }
                }
            }
        }
    }
}

@Composable
private fun ProductSettingsScreen(probe: HealthConnectProbe, notify: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var results by remember { mutableStateOf<List<ProbeResult>>(emptyList()) }
    var probing by remember { mutableStateOf(false) }
    var refresh by remember { mutableStateOf(0) }
    val usage = remember(refresh) { AttentionAccess.hasUsageAccess(context) }
    val notificationListener = remember(refresh) { AttentionAccess.hasNotificationListenerAccess(context) }
    val healthLauncher = rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) { granted ->
        notify(if (granted.containsAll(probe.permissions)) "Health Connect connected" else "Some health permissions remain off")
    }
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            CheckInReminderScheduler.enable(context)
            notify("Check-in reminders enabled")
        } else notify("Notification permission not granted")
    }

    ProductScreenColumn {
        ScreenHeader("Settings", "Sensing, health data, reminders and privacy.")
        SettingsCard("Attention sensing", "Only aggregate counts are stored — never notification text or app content.") {
            PermissionRow("Usage access", usage) { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
            HorizontalDivider()
            PermissionRow("Notification access", notificationListener) { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
            TextButton(onClick = { refresh++ }) { Text("Refresh status") }
        }
        SettingsCard("Health Connect", "Heart rate, sleep, steps and SpO₂ add context to your check-ins.") {
            Button(onClick = { healthLauncher.launch(probe.permissions) }) { Text("Manage health access") }
            OutlinedButton(onClick = {
                scope.launch {
                    probing = true
                    results = probe.probe()
                    probing = false
                }
            }) { Text(if (probing) "Checking…" else "Run diagnostics") }
            results.forEach { HealthResultRow(it) }
        }
        SettingsCard("Check-in reminders", "Optional reminders run roughly every four hours during the day.") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    if (Build.VERSION.SDK_INT >= 33) notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    else {
                        CheckInReminderScheduler.enable(context)
                        notify("Check-in reminders enabled")
                    }
                }) { Text("Enable") }
                OutlinedButton(onClick = {
                    CheckInReminderScheduler.disable(context)
                    notify("Check-in reminders disabled")
                }) { Text("Disable") }
            }
        }
        SettingsCard("Privacy", "CMF Flow is local-first. Your current product build does not require a cloud account.") {
            PrivacyLine("Health context stays on this device")
            PrivacyLine("Notification content is never stored")
            PrivacyLine("Raw app-switch history is not persisted")
            PrivacyLine("Experiments and outcomes remain local")
        }
    }
}

@Composable
private fun ProductCheckInCard(onSave: suspend (SelfReportEntity) -> Unit) {
    val scope = rememberCoroutineScope()
    var flow by remember { mutableStateOf(3) }
    var absorption by remember { mutableStateOf(3) }
    var effortless by remember { mutableStateOf(3) }
    var reward by remember { mutableStateOf(3) }
    var presence by remember { mutableStateOf(3) }
    var fatigue by remember { mutableStateOf(2) }
    var activity by remember { mutableStateOf("") }
    var domain by remember { mutableStateOf("") }
    var more by remember { mutableStateOf(false) }
    var difficulty by remember { mutableStateOf(3) }
    var clarity by remember { mutableStateOf(3) }
    var control by remember { mutableStateOf(3) }

    ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(26.dp)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("How are you doing?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("About 20 seconds. This is the ground truth Flow learns from.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            ScorePicker("Flow", flow) { flow = it }
            ScorePicker("Absorption", absorption) { absorption = it }
            ScorePicker("Effortless control", effortless) { effortless = it }
            ScorePicker("Enjoyment", reward) { reward = it }
            ScorePicker("Presence", presence) { presence = it }
            ScorePicker("Fatigue", fatigue) { fatigue = it }
            OutlinedTextField(activity, { activity = it }, Modifier.fillMaxWidth(), label = { Text("What are you doing? (optional)") }, singleLine = true)
            OutlinedTextField(domain, { domain = it }, Modifier.fillMaxWidth(), label = { Text("Area of life (optional)") }, singleLine = true)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(more, { more = it })
                Spacer(Modifier.size(8.dp))
                Text("More context")
            }
            if (more) {
                ScorePicker("Difficulty", difficulty) { difficulty = it }
                ScorePicker("Goal clarity", clarity) { clarity = it }
                ScorePicker("Control", control) { control = it }
            }
            Button(
                onClick = {
                    scope.launch {
                        onSave(
                            SelfReportEntity(
                                capturedAtEpochMs = System.currentTimeMillis(),
                                flowScore = flow,
                                absorption = absorption,
                                effortlessControl = effortless,
                                intrinsicReward = reward,
                                presence = presence,
                                fatigue = fatigue,
                                activityLabel = activity.trim().ifBlank { null },
                                domain = domain.trim().ifBlank { null },
                                taskDifficulty = difficulty.takeIf { more },
                                goalClarity = clarity.takeIf { more },
                                perceivedControl = control.takeIf { more },
                                notes = null,
                            ),
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Save check-in") }
        }
    }
}

@Composable
private fun ScorePicker(label: String, value: Int, onChange: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(label, Modifier.weight(1f))
            Text("$value / 5", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            (1..5).forEach { n ->
                val selected = value == n
                Surface(
                    onClick = { onChange(n) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                ) {
                    Box(Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                        Text(n.toString(), fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        }
    }
}

@Composable
private fun Sparkline(values: List<Float>) {
    val line = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.outlineVariant
    val fill = MaterialTheme.colorScheme.primaryContainer
    Canvas(Modifier.fillMaxWidth().height(150.dp)) {
        if (values.size < 2) return@Canvas
        val step = size.width / (values.size - 1)
        val path = Path()
        val fillPath = Path()
        values.forEachIndexed { index, raw ->
            val value = raw.coerceIn(0f, 5f)
            val x = index * step
            val y = size.height - (value / 5f) * size.height
            if (index == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, size.height)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }
        fillPath.lineTo(size.width, size.height)
        fillPath.close()
        drawPath(fillPath, fill.copy(alpha = .65f))
        drawLine(grid, Offset(0f, size.height * .5f), Offset(size.width, size.height * .5f), strokeWidth = 1f)
        drawPath(path, line, style = Stroke(width = 5f))
    }
}

@Composable
private fun StateRing(value: Float) {
    Canvas(Modifier.size(82.dp)) {
        drawCircle(Color.White.copy(alpha = .18f), style = Stroke(width = 10f))
        drawArc(
            color = Color.White,
            startAngle = -90f,
            sweepAngle = 360f * value.coerceIn(0f, 1f),
            useCenter = false,
            style = Stroke(width = 10f),
        )
    }
}

@Composable
private fun StateMetric(label: String, value: Int?, modifier: Modifier) {
    Surface(modifier, shape = RoundedCornerShape(18.dp), color = Color.White.copy(alpha = .14f)) {
        Column(Modifier.padding(12.dp)) {
            Text(label, color = Color.White.copy(alpha = .72f), style = MaterialTheme.typography.bodySmall)
            Text(value?.toString() ?: "—", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun InsightMetric(label: String, value: String, color: Color, modifier: Modifier) {
    Surface(modifier, shape = RoundedCornerShape(22.dp), color = color) {
        Column(Modifier.padding(14.dp)) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ConditionResultCard(label: String, text: String, trials: Int, average: Double?, modifier: Modifier) {
    Surface(modifier, shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Text(text, fontWeight = FontWeight.SemiBold)
            Text("$trials completed", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            average?.let { Text("Utility ${"%.1f".format(it)}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
private fun SettingsCard(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(26.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
            content()
        }
    }
}

@Composable
private fun PermissionRow(label: String, granted: Boolean, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Icon(if (granted) Icons.Filled.CheckCircle else Icons.Filled.Settings, null, tint = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.size(10.dp))
        Column(Modifier.weight(1f)) {
            Text(label, fontWeight = FontWeight.SemiBold)
            Text(if (granted) "Enabled" else "Not enabled", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TextButton(onClick = onClick) { Text(if (granted) "Manage" else "Enable") }
    }
}

@Composable
private fun HealthResultRow(result: ProbeResult) {
    val ok = result.error == null && result.recordCount > 0
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Icon(if (ok) Icons.Filled.CheckCircle else Icons.Filled.Favorite, null, tint = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.size(10.dp))
        Column(Modifier.weight(1f)) {
            Text(result.type.replace('_', ' ').replaceFirstChar(Char::uppercase), fontWeight = FontWeight.SemiBold)
            Text(
                if (result.error != null) "Unavailable" else "${result.recordCount} records · ${formatEpochShort(result.latestEpochMs)}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun PrivacyLine(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.size(8.dp))
        Text(text)
    }
}

@Composable
private fun EmptyCard(title: String, body: String) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(26.dp)) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(58.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Filled.Science, null, tint = MaterialTheme.colorScheme.primary) }
            }
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ProductScreenColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        content = content,
    )
}

@Composable
private fun ScreenHeader(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
}

private fun productTabIcon(tab: ProductTab): ImageVector = when (tab) {
    ProductTab.Home -> Icons.Filled.Home
    ProductTab.Insights -> Icons.Filled.BarChart
    ProductTab.Tasks -> Icons.Filled.TaskAlt
    ProductTab.Experiments -> Icons.Filled.Science
    ProductTab.Settings -> Icons.Filled.Settings
}

private fun greeting(): String = when (java.time.LocalTime.now().hour) {
    in 5..11 -> "Good morning"
    in 12..17 -> "Good afternoon"
    else -> "Good evening"
}

private fun stateLabel(report: SelfReportEntity?): String = when {
    report == null -> "Ready to learn you"
    report.fatigue >= 4 -> "Recovery mode"
    report.flowScore >= 4 && report.presence >= 4 -> "Flow is available"
    report.presence <= 2 -> "Attention is scattered"
    else -> "Balanced state"
}

private fun friendlyAction(action: String): String = when (action) {
    "CONTINUE" -> "Keep going"
    "SWITCH_TASK" -> "Switch the task"
    "REDUCE_DIFFICULTY" -> "Make it easier"
    "ASK_AI" -> "Get AI help"
    "TAKE_BREAK" -> "Take a short break"
    "EXERCISE" -> "Move for a few minutes"
    "STOP" -> "Call it for now"
    "REDUCE_INTERRUPTION" -> "Protect your attention"
    else -> action.replace('_', ' ').lowercase().replaceFirstChar(Char::uppercase)
}

private fun friendlyReason(reasons: List<String>): String {
    if (reasons.isEmpty()) return "Based on your latest state and what Flow has learned so far."
    return reasons.first().replace('_', ' ').lowercase().replaceFirstChar(Char::uppercase) + "."
}

private fun friendlyTaskReasons(reasons: List<String>): String {
    val useful = reasons.mapNotNull {
        when {
            it.startsWith("value=") -> "high value"
            it.startsWith("urgency=") -> "urgency considered"
            it.startsWith("difficulty_fit=") -> "fits your current capacity"
            it == "fatigue_guardrail" -> "fatigue protected"
            it == "long_task_penalty" -> "long duration discounted"
            it.contains("personal") -> "uses your history"
            it.contains("context") -> "context-aware"
            it.contains("fragmentation") -> "attention-aware"
            it.contains("outcome") -> "learned from outcomes"
            else -> null
        }
    }.distinct().take(3)
    return if (useful.isEmpty()) "Ranked from value, urgency and difficulty fit." else useful.joinToString(" · ")
}

private fun contextSummary(context: ContextSnapshotEntity): String {
    val parts = mutableListOf<String>()
    context.sleepMinutesPrevious24h?.let { parts += "${it / 60}h sleep" }
    context.stepCount?.let { parts += "$it recent steps" }
    context.heartRateMeanBpm?.let { parts += "${it.toInt()} bpm mean HR" }
    context.appSwitchCount?.let { parts += "$it app switches" }
    context.notificationCount?.let { parts += "$it notifications" }
    return if (parts.isEmpty()) "Context captured; richer signals are still accumulating." else parts.joinToString(" · ")
}

private fun formatScore(value: Double?): String = value?.let { "%.1f".format(it) } ?: "—"

private fun formatEpochShort(epochMs: Long?): String {
    if (epochMs == null) return "no recent data"
    return DateTimeFormatter.ofPattern("MMM d, HH:mm").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(epochMs))
}
