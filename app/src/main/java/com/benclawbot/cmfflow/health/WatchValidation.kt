package com.benclawbot.cmfflow.health

/** Evaluate the hardware-only Health Connect acceptance evidence without mixing data origins. */
object WatchValidation {
    const val NOTHING_X = "com.nothing.smartcenter"
    private const val MINUTE_MS = 60_000L

    /** Elapsed minutes since the latest Nothing X heart-rate sample; null if absent. */
    fun heartRateRecencyMinutes(result: ProbeResult, checkedAtEpochMs: Long): Long? {
        if (result.type != "heart_rate" || result.error != null) return null
        val latest = result.latestByOriginEpochMs[NOTHING_X] ?: return null
        if (latest > checkedAtEpochMs) return null
        return (checkedAtEpochMs - latest) / MINUTE_MS
    }

    /** A known watch workout needs a same-origin start and end within tolerance. */
    fun matchesKnownWorkout(
        result: ProbeResult,
        workoutStartEpochMs: Long,
        workoutEndEpochMs: Long,
        toleranceMs: Long = 5 * MINUTE_MS,
    ): Boolean {
        if (result.type != "exercise" || result.error != null ||
            workoutEndEpochMs < workoutStartEpochMs || toleranceMs < 0
        ) return false
        return result.exerciseSessions.any { session ->
            session.originPackage == NOTHING_X &&
                kotlin.math.abs(session.startEpochMs - workoutStartEpochMs) <= toleranceMs &&
                kotlin.math.abs(session.endEpochMs - workoutEndEpochMs) <= toleranceMs
        }
    }
}
