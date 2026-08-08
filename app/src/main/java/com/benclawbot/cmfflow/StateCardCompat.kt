package com.benclawbot.cmfflow

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.benclawbot.cmfflow.data.SelfReportEntity
import com.benclawbot.cmfflow.data.SessionEntity

@Composable
fun StateCard(report: SelfReportEntity?, session: SessionEntity?, minutes: Int?) {
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
