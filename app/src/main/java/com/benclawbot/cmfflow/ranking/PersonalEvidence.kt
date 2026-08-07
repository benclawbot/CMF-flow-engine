package com.benclawbot.cmfflow.ranking

import com.benclawbot.cmfflow.data.SelfReportEntity
import java.time.Instant
import java.time.ZoneId

data class EvidenceEstimate(
    val adjustment: Double,
    val reasons: List<String>,
)

fun personalEvidenceFor(
    taskDomain: String?,
    reports: List<SelfReportEntity>,
    nowEpochMs: Long = System.currentTimeMillis(),
    zoneId: ZoneId = ZoneId.systemDefault(),
): EvidenceEstimate {
    if (reports.isEmpty()) return EvidenceEstimate(0.0, emptyList())

    var adjustment = 0.0
    val reasons = mutableListOf<String>()
    val overallFlow = reports.map { it.flowScore }.average()

    val normalizedDomain = taskDomain?.trim()?.lowercase()?.takeIf { it.isNotBlank() }
    if (normalizedDomain != null) {
        val domainReports = reports.filter { it.domain?.trim()?.lowercase() == normalizedDomain }
        if (domainReports.size >= 3) {
            val domainFlow = domainReports.map { it.flowScore }.average()
            val delta = ((domainFlow - overallFlow) * 0.8).coerceIn(-1.5, 1.5)
            adjustment += delta
            reasons += "personal_domain_n=${domainReports.size}"
            reasons += "personal_domain_delta=${formatDelta(delta)}"
        }
    }

    val currentBucket = timeBucket(nowEpochMs, zoneId)
    val timeReports = reports.filter { timeBucket(it.capturedAtEpochMs, zoneId) == currentBucket }
    if (timeReports.size >= 5) {
        val bucketFlow = timeReports.map { it.flowScore }.average()
        val delta = ((bucketFlow - overallFlow) * 0.5).coerceIn(-1.0, 1.0)
        adjustment += delta
        reasons += "personal_time_${currentBucket}_n=${timeReports.size}"
        reasons += "personal_time_delta=${formatDelta(delta)}"
    }

    return EvidenceEstimate(adjustment.coerceIn(-2.0, 2.0), reasons)
}

internal fun timeBucket(epochMs: Long, zoneId: ZoneId): String {
    val hour = Instant.ofEpochMilli(epochMs).atZone(zoneId).hour
    return when (hour) {
        in 5..10 -> "morning"
        in 11..16 -> "day"
        in 17..21 -> "evening"
        else -> "night"
    }
}

private fun formatDelta(value: Double): String = "%+.2f".format(value)
