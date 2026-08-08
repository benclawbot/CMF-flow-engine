package com.benclawbot.cmfflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.benclawbot.cmfflow.health.HealthConnectProbe
import com.benclawbot.cmfflow.health.HealthContextCollector
import com.benclawbot.cmfflow.reminders.CheckInReminderScheduler
import com.benclawbot.cmfflow.ui.FlowTheme

class ProductActivity : ComponentActivity() {
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
            val experimentHistory by database.experimentDao().observeRecent().collectAsState(initial = emptyList())
            val assignments by database.experimentAssignmentDao().observeRecent().collectAsState(initial = emptyList())

            FlowTheme {
                ProductApp(
                    probe = probe,
                    reports = reports,
                    contexts = contexts,
                    recommendations = recommendations,
                    tasks = tasks,
                    session = session,
                    experiments = experiments,
                    experimentHistory = experimentHistory,
                    assignments = assignments,
                    saveReport = { report ->
                        val reportId = database.selfReportDao().insert(report)
                        database.contextSnapshotDao().insert(contextCollector.collect(reportId, report.capturedAtEpochMs))
                        database.recommendationEventDao().attachOutcomeToLatestResponded(reportId)
                        database.interventionEventDao().attachOutcomeToLatestResponded(reportId)
                        database.experimentAssignmentDao().attachOutcomeToLatestOpen(reportId, System.currentTimeMillis())
                        CheckInReminderScheduler.markCheckIn(this@ProductActivity, report.capturedAtEpochMs)
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
                    assignExperiment = { database.experimentAssignmentDao().insert(it) },
                )
            }
        }
    }
}
