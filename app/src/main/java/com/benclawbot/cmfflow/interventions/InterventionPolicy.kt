package com.benclawbot.cmfflow.interventions

import com.benclawbot.cmfflow.data.SelfReportEntity

enum class InterventionAction {
    CONTINUE,
    SWITCH_TASK,
    REDUCE_DIFFICULTY,
    ASK_AI,
    TAKE_BREAK,
    EXERCISE,
    STOP,
}

data class InterventionRecommendation(
    val action: InterventionAction,
    val reasons: List<String>,
)

fun recommendIntervention(
    latestReport: SelfReportEntity?,
    minutesOnCurrentTask: Int? = null,
    repeatedStruggle: Boolean = false,
): InterventionRecommendation {
    if (latestReport == null) {
        return InterventionRecommendation(
            InterventionAction.CONTINUE,
            listOf("no_recent_state_keep_intervention_minimal"),
        )
    }

    val fatigue = latestReport.fatigue
    val flow = latestReport.flowScore
    val presence = latestReport.presence
    val difficulty = latestReport.taskDifficulty

    return when {
        fatigue >= 5 -> InterventionRecommendation(
            InterventionAction.STOP,
            listOf("very_high_fatigue", "protect_long_term_capacity"),
        )
        fatigue >= 4 && (minutesOnCurrentTask ?: 0) >= 45 -> InterventionRecommendation(
            InterventionAction.TAKE_BREAK,
            listOf("high_fatigue", "extended_session"),
        )
        repeatedStruggle && difficulty != null && difficulty >= 4 -> InterventionRecommendation(
            InterventionAction.ASK_AI,
            listOf("repeated_struggle", "high_task_difficulty"),
        )
        flow <= 1 && presence <= 2 && difficulty != null && difficulty >= 4 -> InterventionRecommendation(
            InterventionAction.REDUCE_DIFFICULTY,
            listOf("low_flow", "low_presence", "challenge_may_exceed_current_capacity"),
        )
        flow <= 1 && presence <= 2 -> InterventionRecommendation(
            InterventionAction.SWITCH_TASK,
            listOf("low_flow", "low_presence"),
        )
        flow >= 4 && presence >= 4 && fatigue <= 3 -> InterventionRecommendation(
            InterventionAction.CONTINUE,
            listOf("strong_flow", "strong_presence", "fatigue_within_guardrail"),
        )
        fatigue >= 3 && flow <= 2 -> InterventionRecommendation(
            InterventionAction.EXERCISE,
            listOf("moderate_fatigue", "low_flow", "state_reset_option"),
        )
        else -> InterventionRecommendation(
            InterventionAction.CONTINUE,
            listOf("no_strong_reason_to_interrupt"),
        )
    }
}
