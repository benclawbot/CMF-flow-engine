package com.benclawbot.cmfflow.domain

import com.benclawbot.cmfflow.data.SelfReportEntity

object SelfReportValidator {
    private val scoreRange = 0..5

    fun validate(report: SelfReportEntity): Result<SelfReportEntity> {
        val scores = listOf(
            "flow" to report.flowScore,
            "absorption" to report.absorption,
            "effortlessControl" to report.effortlessControl,
            "intrinsicReward" to report.intrinsicReward,
            "presence" to report.presence,
            "fatigue" to report.fatigue,
        ) + listOfNotNull(
            report.taskDifficulty?.let { "taskDifficulty" to it },
            report.goalClarity?.let { "goalClarity" to it },
            report.perceivedControl?.let { "perceivedControl" to it },
        )

        val invalid = scores.firstOrNull { (_, value) -> value !in scoreRange }
        return if (invalid == null) {
            Result.success(report)
        } else {
            Result.failure(IllegalArgumentException("${invalid.first} must be in 0..5, was ${invalid.second}"))
        }
    }
}
