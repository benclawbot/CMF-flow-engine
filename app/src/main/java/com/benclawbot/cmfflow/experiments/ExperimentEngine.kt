package com.benclawbot.cmfflow.experiments

import com.benclawbot.cmfflow.data.ExperimentAssignmentEntity
import com.benclawbot.cmfflow.data.ExperimentEntity
import com.benclawbot.cmfflow.data.SelfReportEntity

private const val MIN_COMPLETED_PER_CONDITION = 4

data class ConditionResult(
    val condition: String,
    val completedTrials: Int,
    val averageUtility: Double?,
)

data class ExperimentResult(
    val conditionA: ConditionResult,
    val conditionB: ConditionResult,
    val deltaAminusB: Double?,
    val evidenceReady: Boolean,
    val summary: String,
)

data class LearnedExperimentRecommendation(
    val experimentId: Long,
    val condition: String,
    val utilityAdvantage: Double,
)

fun chooseNextCondition(
    experiment: ExperimentEntity,
    assignments: List<ExperimentAssignmentEntity>,
    randomBoolean: Boolean,
): String {
    val relevant = assignments.filter { it.experimentId == experiment.id }
    val countA = relevant.count { it.assignedCondition == experiment.conditionA }
    val countB = relevant.count { it.assignedCondition == experiment.conditionB }
    return when {
        countA < countB -> experiment.conditionA
        countB < countA -> experiment.conditionB
        randomBoolean -> experiment.conditionA
        else -> experiment.conditionB
    }
}

fun analyzeExperiment(
    experiment: ExperimentEntity,
    assignments: List<ExperimentAssignmentEntity>,
    reports: List<SelfReportEntity>,
): ExperimentResult {
    val reportsById = reports.associateBy { it.id }
    val relevant = assignments.filter { it.experimentId == experiment.id && it.outcomeSelfReportId != null }

    fun resultFor(condition: String): ConditionResult {
        val utilities = relevant
            .filter { it.assignedCondition == condition }
            .mapNotNull { assignment -> reportsById[assignment.outcomeSelfReportId]?.let(::utility) }
        return ConditionResult(
            condition = condition,
            completedTrials = utilities.size,
            averageUtility = utilities.takeIf { it.isNotEmpty() }?.average(),
        )
    }

    val a = resultFor(experiment.conditionA)
    val b = resultFor(experiment.conditionB)
    val ready = a.completedTrials >= MIN_COMPLETED_PER_CONDITION && b.completedTrials >= MIN_COMPLETED_PER_CONDITION
    val delta = if (ready && a.averageUtility != null && b.averageUtility != null) a.averageUtility - b.averageUtility else null
    val summary = when {
        !ready -> "Keep testing — at least $MIN_COMPLETED_PER_CONDITION completed trials per condition are needed before comparing results."
        delta == null -> "Not enough linked outcomes to compare conditions yet."
        kotlin.math.abs(delta) < 0.35 -> "The two conditions are performing similarly so far."
        delta > 0 -> "${experiment.conditionA} is associated with better follow-up flow so far."
        else -> "${experiment.conditionB} is associated with better follow-up flow so far."
    }
    return ExperimentResult(a, b, delta, ready, summary)
}

fun learnedExperimentRecommendation(
    experiments: List<ExperimentEntity>,
    assignments: List<ExperimentAssignmentEntity>,
    reports: List<SelfReportEntity>,
): LearnedExperimentRecommendation? = experiments.asSequence()
    .map { experiment -> experiment to analyzeExperiment(experiment, assignments, reports) }
    .mapNotNull { (experiment, result) ->
        val delta = result.deltaAminusB ?: return@mapNotNull null
        if (!result.evidenceReady || kotlin.math.abs(delta) < 0.35) return@mapNotNull null
        LearnedExperimentRecommendation(
            experimentId = experiment.id,
            condition = if (delta > 0) experiment.conditionA else experiment.conditionB,
            utilityAdvantage = kotlin.math.abs(delta),
        )
    }
    .firstOrNull()

private fun utility(report: SelfReportEntity): Double =
    report.flowScore.toDouble() + report.presence.toDouble() - report.fatigue.toDouble()
