package com.benclawbot.cmfflow.ranking

import com.benclawbot.cmfflow.data.SelfReportEntity
import com.benclawbot.cmfflow.data.TaskEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskRankerTest {
    @Test
    fun highFatiguePrefersEasierShorterTask() {
        val report = SelfReportEntity(
            capturedAtEpochMs = 1,
            flowScore = 2,
            absorption = 2,
            effortlessControl = 2,
            intrinsicReward = 2,
            presence = 2,
            fatigue = 5,
            activityLabel = null,
            domain = null,
            taskDifficulty = null,
            goalClarity = null,
            perceivedControl = null,
            notes = null,
        )
        val easy = TaskEntity(title = "Admin", domain = "admin", valueScore = 3, urgencyScore = 3, difficultyScore = 1, estimatedMinutes = 20)
        val hard = TaskEntity(title = "Architecture", domain = "work", valueScore = 4, urgencyScore = 3, difficultyScore = 5, estimatedMinutes = 90)

        val ranked = rankTasks(listOf(hard, easy), report)

        assertEquals("Admin", ranked.first().task.title)
        assertTrue(ranked.first().reasons.contains("fatigue_guardrail"))
    }

    @Test
    fun strongFlowAllowsHarderHighValueTask() {
        val report = SelfReportEntity(
            capturedAtEpochMs = 1,
            flowScore = 5,
            absorption = 5,
            effortlessControl = 4,
            intrinsicReward = 5,
            presence = 5,
            fatigue = 1,
            activityLabel = null,
            domain = null,
            taskDifficulty = null,
            goalClarity = null,
            perceivedControl = null,
            notes = null,
        )
        val deep = TaskEntity(title = "Deep work", domain = "work", valueScore = 5, urgencyScore = 3, difficultyScore = 4, estimatedMinutes = 75)
        val easy = TaskEntity(title = "Email", domain = "admin", valueScore = 2, urgencyScore = 4, difficultyScore = 1, estimatedMinutes = 15)

        assertEquals("Deep work", rankTasks(listOf(easy, deep), report).first().task.title)
    }
}
