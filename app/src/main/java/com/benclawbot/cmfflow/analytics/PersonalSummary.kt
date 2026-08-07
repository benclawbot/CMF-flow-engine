package com.benclawbot.cmfflow.analytics

import com.benclawbot.cmfflow.data.SelfReportEntity

data class PersonalSummary(
    val sampleCount: Int,
    val averageFlow: Double?,
    val averageFatigue: Double?,
    val averagePresence: Double?,
    val strongestDomain: String?,
)

fun summarize(reports: List<SelfReportEntity>): PersonalSummary {
    if (reports.isEmpty()) {
        return PersonalSummary(0, null, null, null, null)
    }

    val strongestDomain = reports
        .mapNotNull { report -> report.domain?.takeIf { it.isNotBlank() }?.let { it to report.flowScore } }
        .groupBy({ it.first }, { it.second })
        .filterValues { it.size >= 3 }
        .maxByOrNull { (_, scores) -> scores.average() }
        ?.key

    return PersonalSummary(
        sampleCount = reports.size,
        averageFlow = reports.map { it.flowScore }.average(),
        averageFatigue = reports.map { it.fatigue }.average(),
        averagePresence = reports.map { it.presence }.average(),
        strongestDomain = strongestDomain,
    )
}
