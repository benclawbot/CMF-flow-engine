package com.benclawbot.cmfflow.ranking

import com.benclawbot.cmfflow.data.ContextSnapshotEntity
import com.benclawbot.cmfflow.data.SelfReportEntity
import kotlin.math.abs

data class ContextEvidenceEstimate(
    val adjustment: Double,
    val reasons: List<String>,
)

fun contextEvidenceFor(
    current: ContextSnapshotEntity?,
    reports: List<SelfReportEntity>,
    snapshots: List<ContextSnapshotEntity>,
): ContextEvidenceEstimate {
    if (current == null || reports.isEmpty() || snapshots.isEmpty()) return ContextEvidenceEstimate(0.0, emptyList())

    val reportsById = reports.associateBy { it.id }
    val paired = snapshots.mapNotNull { snapshot -> reportsById[snapshot.selfReportId]?.let { it to snapshot } }
    if (paired.size < 5) return ContextEvidenceEstimate(0.0, emptyList())

    val overallFlow = paired.map { it.first.flowScore }.average()
    var adjustment = 0.0
    val reasons = mutableListOf<String>()

    fun apply(label: String, matches: List<Pair<SelfReportEntity, ContextSnapshotEntity>>, weight: Double, cap: Double, minN: Int) {
        if (matches.size < minN) return
        val delta = ((matches.map { it.first.flowScore }.average() - overallFlow) * weight).coerceIn(-cap, cap)
        adjustment += delta
        reasons += "context_${label}_n=${matches.size}"
        reasons += "context_${label}_delta=${formatDelta(delta)}"
    }

    current.sleepMinutesPrevious24h?.let { sleep ->
        val bucket = when {
            sleep < 360 -> "sleep_low"
            sleep < 450 -> "sleep_mid"
            else -> "sleep_high"
        }
        apply(bucket, paired.filter { (_, s) ->
            val v = s.sleepMinutesPrevious24h ?: return@filter false
            when (bucket) {
                "sleep_low" -> v < 360
                "sleep_mid" -> v in 360..<450
                else -> v >= 450
            }
        }, weight = 0.45, cap = 0.75, minN = 5)
    }

    current.heartRateMeanBpm?.let { hr ->
        apply("hr_similar", paired.filter { (_, s) -> s.heartRateMeanBpm?.let { abs(it - hr) <= 8.0 } == true }, weight = 0.35, cap = 0.6, minN = 5)
    }

    current.stepCount?.let { steps ->
        val active = steps >= 250
        apply(if (active) "recently_active" else "recently_inactive", paired.filter { (_, s) -> s.stepCount?.let { (it >= 250) == active } == true }, weight = 0.3, cap = 0.5, minN = 5)
    }

    current.isCharging?.let { charging ->
        apply(if (charging) "charging" else "not_charging", paired.filter { (_, s) -> s.isCharging == charging }, weight = 0.2, cap = 0.35, minN = 6)
    }

    current.isPhoneInteractive?.let { interactive ->
        apply(if (interactive) "phone_interactive" else "phone_not_interactive", paired.filter { (_, s) -> s.isPhoneInteractive == interactive }, weight = 0.2, cap = 0.35, minN = 6)
    }

    return ContextEvidenceEstimate(adjustment.coerceIn(-1.5, 1.5), reasons)
}

private fun formatDelta(value: Double): String = "%+.2f".format(value)
