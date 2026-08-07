package com.benclawbot.cmfflow.ranking

import com.benclawbot.cmfflow.data.ContextSnapshotEntity
import com.benclawbot.cmfflow.data.RecommendationEventEntity
import com.benclawbot.cmfflow.data.SelfReportEntity
import com.benclawbot.cmfflow.data.TaskEntity
import kotlin.math.abs

data class RankedTask(
    val task: TaskEntity,
    val score: Double,
    val reasons: List<String>,
)

fun rankTasks(
    tasks: List<TaskEntity>,
    latestReport: SelfReportEntity?,
    historicalReports: List<SelfReportEntity> = emptyList(),
    currentContext: ContextSnapshotEntity? = null,
    historicalContexts: List<ContextSnapshotEntity> = emptyList(),
    recommendationEvents: List<RecommendationEventEntity> = emptyList(),
): List<RankedTask> {
    val fatigue = latestReport?.fatigue ?: 2
    val flow = latestReport?.flowScore ?: 3
    val presence = latestReport?.presence ?: 3
    val preferredDifficulty = when {
        fatigue >= 4 -> 1
        flow >= 4 && presence >= 4 -> 4
        else -> 3
    }
    val contextEvidence = contextEvidenceFor(currentContext, historicalReports, historicalContexts)
    val fragmentationEvidence = fragmentationEvidenceFor(currentContext, historicalReports, historicalContexts)

    return tasks
        .filter { it.status == "open" }
        .map { task ->
            val fitPenalty = abs(task.difficultyScore - preferredDifficulty) * 1.5
            val fatiguePenalty = if (fatigue >= 4) task.difficultyScore * 1.25 else 0.0
            val durationPenalty = if (fatigue >= 4 && task.estimatedMinutes > 45) 2.0 else 0.0
            val personalEvidence = personalEvidenceFor(task.domain, historicalReports)
            val outcomeEvidence = outcomeEvidenceFor(task.domain, recommendationEvents, historicalReports)
            val evidenceAdjustment = (
                personalEvidence.adjustment +
                    contextEvidence.adjustment +
                    fragmentationEvidence.adjustment +
                    outcomeEvidence.adjustment
            ).coerceIn(-3.5, 3.5)
            val score = task.valueScore * 2.0 + task.urgencyScore * 1.5 - fitPenalty - fatiguePenalty - durationPenalty + evidenceAdjustment
            val reasons = buildList {
                add("value=${task.valueScore}")
                add("urgency=${task.urgencyScore}")
                add("difficulty_fit=${task.difficultyScore}/$preferredDifficulty")
                if (fatigue >= 4) add("fatigue_guardrail")
                if (durationPenalty > 0) add("long_task_penalty")
                addAll(personalEvidence.reasons)
                addAll(contextEvidence.reasons)
                addAll(fragmentationEvidence.reasons)
                addAll(outcomeEvidence.reasons)
                if (
                    historicalReports.isNotEmpty() &&
                    personalEvidence.reasons.isEmpty() &&
                    contextEvidence.reasons.isEmpty() &&
                    fragmentationEvidence.reasons.isEmpty() &&
                    outcomeEvidence.reasons.isEmpty()
                ) {
                    add("personal_evidence_insufficient")
                }
            }
            RankedTask(task, score, reasons)
        }
        .sortedByDescending { it.score }
}
