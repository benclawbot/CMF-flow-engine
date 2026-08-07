package com.benclawbot.cmfflow.ranking

import com.benclawbot.cmfflow.data.RecommendationEventEntity
import com.benclawbot.cmfflow.data.SelfReportEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OutcomeEvidenceTest {
    @Test
    fun doesNotActivateWithTooFewAcceptedDomainOutcomes() {
        val reports = (1L..6L).map { report(it, flow = 3, presence = 3, fatigue = 2) }
        val events = (1L..3L).map { id -> acceptedEvent(id, "work", id) }

        val evidence = outcomeEvidenceFor("work", events, reports)

        assertEquals(0.0, evidence.adjustment, 0.0001)
        assertTrue(evidence.reasons.isEmpty())
    }

    @Test
    fun positiveAcceptedOutcomesCreateBoundedPositiveAssociation() {
        val baseline = (1L..4L).map { report(it, flow = 2, presence = 2, fatigue = 3) }
        val strong = (5L..8L).map { report(it, flow = 5, presence = 5, fatigue = 1) }
        val reports = baseline + strong
        val events = (5L..8L).map { id -> acceptedEvent(id, "work", id) }

        val evidence = outcomeEvidenceFor("work", events, reports)

        assertTrue(evidence.adjustment > 0.0)
        assertTrue(evidence.adjustment <= 1.0)
        assertTrue(evidence.reasons.any { it.startsWith("accepted_outcome_work_n=4") })
    }

    private fun acceptedEvent(id: Long, domain: String, reportId: Long) = RecommendationEventEntity(
        id = id,
        taskId = id,
        taskTitle = "Task $id",
        taskDomain = domain,
        score = 1.0,
        reasonsSnapshot = "test",
        response = "accepted",
        respondedAtEpochMs = id,
        outcomeSelfReportId = reportId,
    )

    private fun report(id: Long, flow: Int, presence: Int, fatigue: Int) = SelfReportEntity(
        id = id,
        capturedAtEpochMs = id,
        flowScore = flow,
        absorption = flow,
        effortlessControl = flow,
        intrinsicReward = flow,
        presence = presence,
        fatigue = fatigue,
        activityLabel = null,
        domain = null,
        taskDifficulty = null,
        goalClarity = null,
        perceivedControl = null,
        notes = null,
    )
}
