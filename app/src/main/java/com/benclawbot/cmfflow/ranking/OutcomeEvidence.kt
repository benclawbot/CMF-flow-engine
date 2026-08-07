package com.benclawbot.cmfflow.ranking

import com.benclawbot.cmfflow.data.RecommendationEventEntity
import com.benclawbot.cmfflow.data.SelfReportEntity

data class OutcomeEvidenceEstimate(
    val adjustment: Double,
    val reasons: List<String>,
)

fun outcomeEvidenceFor(
    taskDomain: String?,
    events: List<RecommendationEventEntity>,
    reports: List<SelfReportEntity>,
): OutcomeEvidenceEstimate {
    val domain = taskDomain?.trim()?.lowercase()?.takeIf { it.isNotBlank() }
        ?: return OutcomeEvidenceEstimate(0.0, emptyList())
    if (reports.isEmpty() || events.isEmpty()) return OutcomeEvidenceEstimate(0.0, emptyList())

    val reportsById = reports.associateBy { it.id }
    val acceptedOutcomes = events
        .asSequence()
        .filter { it.response == "accepted" }
        .filter { it.taskDomain.trim().lowercase() == domain }
        .mapNotNull { event -> event.outcomeSelfReportId?.let(reportsById::get) }
        .toList()

    if (acceptedOutcomes.size < MIN_ACCEPTED_DOMAIN_OUTCOMES) {
        return OutcomeEvidenceEstimate(0.0, emptyList())
    }

    val baselineUtility = reports.map(::outcomeUtility).average()
    val acceptedUtility = acceptedOutcomes.map(::outcomeUtility).average()
    val rawDelta = acceptedUtility - baselineUtility
    val adjustment = (rawDelta * 0.6).coerceIn(-1.0, 1.0)

    return OutcomeEvidenceEstimate(
        adjustment = adjustment,
        reasons = listOf(
            "accepted_outcome_${domain}_n=${acceptedOutcomes.size}",
            "accepted_outcome_assoc=${formatDelta(adjustment)}",
        ),
    )
}

internal fun outcomeUtility(report: SelfReportEntity): Double =
    report.flowScore + report.presence * 0.35 - report.fatigue * 0.55

private fun formatDelta(value: Double): String = "%+.2f".format(value)

private const val MIN_ACCEPTED_DOMAIN_OUTCOMES = 4
