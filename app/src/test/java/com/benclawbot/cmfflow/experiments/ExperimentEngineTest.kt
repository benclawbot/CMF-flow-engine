package com.benclawbot.cmfflow.experiments

import com.benclawbot.cmfflow.data.ExperimentAssignmentEntity
import com.benclawbot.cmfflow.data.ExperimentEntity
import com.benclawbot.cmfflow.data.SelfReportEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExperimentEngineTest {
    private val experiment = ExperimentEntity(
        id = 7,
        hypothesis = "A improves flow",
        conditionA = "Walk first",
        conditionB = "No walk",
    )

    @Test
    fun choosesUnderrepresentedConditionBeforeRandomTieBreak() {
        val assignments = listOf(
            ExperimentAssignmentEntity(experimentId = 7, assignedCondition = "Walk first"),
            ExperimentAssignmentEntity(experimentId = 7, assignedCondition = "Walk first"),
            ExperimentAssignmentEntity(experimentId = 7, assignedCondition = "No walk"),
        )
        assertEquals("No walk", chooseNextCondition(experiment, assignments, randomBoolean = true))
    }

    @Test
    fun tieUsesRandomizedChoice() {
        assertEquals("Walk first", chooseNextCondition(experiment, emptyList(), randomBoolean = true))
        assertEquals("No walk", chooseNextCondition(experiment, emptyList(), randomBoolean = false))
    }

    @Test
    fun resultWaitsForFourCompletedTrialsPerCondition() {
        val reports = (1L..6L).map { id -> report(id, flow = 4, presence = 4, fatigue = 2) }
        val assignments = listOf(
            assignment(1, "Walk first"), assignment(2, "Walk first"), assignment(3, "Walk first"),
            assignment(4, "No walk"), assignment(5, "No walk"), assignment(6, "No walk"),
        )
        assertFalse(analyzeExperiment(experiment, assignments, reports).evidenceReady)
    }

    @Test
    fun resultIdentifiesBetterConditionAfterThreshold() {
        val reports = (1L..8L).map { id ->
            if (id <= 4) report(id, flow = 5, presence = 5, fatigue = 1)
            else report(id, flow = 3, presence = 3, fatigue = 3)
        }
        val assignments = (1L..4L).map { assignment(it, "Walk first") } +
            (5L..8L).map { assignment(it, "No walk") }

        val result = analyzeExperiment(experiment, assignments, reports)
        assertTrue(result.evidenceReady)
        assertTrue((result.deltaAminusB ?: 0.0) > 0.35)
        assertTrue(result.summary.contains("Walk first"))
    }

    private fun assignment(reportId: Long, condition: String) = ExperimentAssignmentEntity(
        experimentId = 7,
        assignedCondition = condition,
        outcomeSelfReportId = reportId,
        completedAtEpochMs = reportId,
    )

    private fun report(id: Long, flow: Int, presence: Int, fatigue: Int) = SelfReportEntity(
        id = id,
        capturedAtEpochMs = id,
        flowScore = flow,
        absorption = 3,
        effortlessControl = 3,
        intrinsicReward = 3,
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
