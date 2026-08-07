package com.benclawbot.cmfflow.interventions

import com.benclawbot.cmfflow.data.SelfReportEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class InterventionPolicyTest {
    @Test
    fun strongFlowProtectsCurrentState() {
        val report = report(flow = 5, presence = 5, fatigue = 2, difficulty = 4)
        assertEquals(InterventionAction.CONTINUE, recommendIntervention(report).action)
    }

    @Test
    fun veryHighFatigueStopsSession() {
        val report = report(flow = 4, presence = 4, fatigue = 5, difficulty = 4)
        assertEquals(InterventionAction.STOP, recommendIntervention(report).action)
    }

    @Test
    fun prolongedHighFatigueTriggersBreak() {
        val report = report(flow = 3, presence = 3, fatigue = 4, difficulty = 3)
        assertEquals(InterventionAction.TAKE_BREAK, recommendIntervention(report, minutesOnCurrentTask = 60).action)
    }

    @Test
    fun repeatedStruggleOnHardTaskSuggestsAi() {
        val report = report(flow = 2, presence = 3, fatigue = 2, difficulty = 5)
        assertEquals(InterventionAction.ASK_AI, recommendIntervention(report, repeatedStruggle = true).action)
    }

    private fun report(flow: Int, presence: Int, fatigue: Int, difficulty: Int) = SelfReportEntity(
        capturedAtEpochMs = 1,
        flowScore = flow,
        absorption = flow,
        effortlessControl = flow,
        intrinsicReward = flow,
        presence = presence,
        fatigue = fatigue,
        activityLabel = null,
        domain = "work",
        taskDifficulty = difficulty,
        goalClarity = 3,
        perceivedControl = 3,
        notes = null,
    )
}
