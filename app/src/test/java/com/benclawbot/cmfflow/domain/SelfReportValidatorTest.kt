package com.benclawbot.cmfflow.domain

import com.benclawbot.cmfflow.data.SelfReportEntity
import org.junit.Assert.assertTrue
import org.junit.Test

class SelfReportValidatorTest {
    @Test
    fun acceptsScoresWithinRange() {
        assertTrue(SelfReportValidator.validate(report()).isSuccess)
    }

    @Test
    fun rejectsScoresOutsideRange() {
        assertTrue(SelfReportValidator.validate(report(fatigue = 6)).isFailure)
    }

    private fun report(fatigue: Int = 2) = SelfReportEntity(
        capturedAtEpochMs = 1L,
        flowScore = 3,
        absorption = 4,
        effortlessControl = 3,
        intrinsicReward = 4,
        presence = 4,
        fatigue = fatigue,
        activityLabel = null,
        domain = null,
        taskDifficulty = null,
        goalClarity = null,
        perceivedControl = null,
        notes = null,
    )
}
