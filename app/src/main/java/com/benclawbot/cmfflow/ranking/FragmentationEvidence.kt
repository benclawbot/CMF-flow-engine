package com.benclawbot.cmfflow.ranking

import com.benclawbot.cmfflow.data.ContextSnapshotEntity
import com.benclawbot.cmfflow.data.SelfReportEntity

data class FragmentationEvidenceEstimate(
    val harmfulAssociation: Boolean,
    val adjustment: Double,
    val sampleCount: Int,
    val reasons: List<String>,
)

fun fragmentationEvidenceFor(
    current: ContextSnapshotEntity?,
    reports: List<SelfReportEntity>,
    snapshots: List<ContextSnapshotEntity>,
): FragmentationEvidenceEstimate {
    if (current == null) return FragmentationEvidenceEstimate(false, 0.0, 0, emptyList())

    val currentScore = fragmentationScore(current) ?: return FragmentationEvidenceEstimate(false, 0.0, 0, emptyList())
    val reportsById = reports.associateBy { it.id }
    val paired = snapshots.mapNotNull { snapshot ->
        val report = reportsById[snapshot.selfReportId] ?: return@mapNotNull null
        val score = fragmentationScore(snapshot) ?: return@mapNotNull null
        Triple(report, snapshot, score)
    }

    if (paired.size < MIN_FRAGMENTATION_SAMPLES) {
        return FragmentationEvidenceEstimate(false, 0.0, paired.size, emptyList())
    }

    val median = paired.map { it.third }.sorted().let { values ->
        if (values.size % 2 == 1) values[values.size / 2].toDouble()
        else (values[values.size / 2 - 1] + values[values.size / 2]) / 2.0
    }

    val high = paired.filter { it.third > median }
    val low = paired.filter { it.third <= median }
    if (high.size < MIN_BUCKET_SAMPLES || low.size < MIN_BUCKET_SAMPLES) {
        return FragmentationEvidenceEstimate(false, 0.0, paired.size, emptyList())
    }

    val highUtility = high.map { utility(it.first) }.average()
    val lowUtility = low.map { utility(it.first) }.average()
    val delta = highUtility - lowUtility
    val harmful = delta <= HARMFUL_DELTA_THRESHOLD
    val currentHigh = currentScore > median
    val adjustment = if (harmful && currentHigh) delta.coerceIn(-0.8, 0.0) else 0.0

    val reasons = buildList {
        add("fragmentation_evidence_n=${paired.size}")
        add("fragmentation_high_vs_low_delta=${"%+.2f".format(delta)}")
        if (harmful) add("fragmentation_harmful_association")
        if (currentHigh) add("fragmentation_currently_high")
    }

    return FragmentationEvidenceEstimate(harmful, adjustment, paired.size, reasons)
}

private fun fragmentationScore(snapshot: ContextSnapshotEntity): Int? {
    val components = listOfNotNull(
        snapshot.appSwitchCount?.times(2),
        snapshot.unlockCount?.times(3),
        snapshot.screenInteractiveTransitions,
        snapshot.notificationCount,
    )
    return components.takeIf { it.isNotEmpty() }?.sum()
}

private fun utility(report: SelfReportEntity): Double =
    ((report.flowScore + report.presence) / 2.0) - (report.fatigue * 0.35)

private const val MIN_FRAGMENTATION_SAMPLES = 10
private const val MIN_BUCKET_SAMPLES = 4
private const val HARMFUL_DELTA_THRESHOLD = -0.5
