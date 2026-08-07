package com.benclawbot.cmfflow.interventions

import com.benclawbot.cmfflow.data.SessionEntity

data class SessionSignals(
    val minutesOnCurrentTask: Int?,
    val repeatedStruggle: Boolean,
)

fun sessionSignals(
    session: SessionEntity?,
    nowEpochMs: Long = System.currentTimeMillis(),
): SessionSignals {
    if (session == null || session.status != "active") {
        return SessionSignals(null, false)
    }

    val elapsedMs = (nowEpochMs - session.startedAtEpochMs).coerceAtLeast(0L)
    val minutes = (elapsedMs / 60_000L).toInt()
    return SessionSignals(
        minutesOnCurrentTask = minutes,
        repeatedStruggle = session.struggleCount >= 2,
    )
}
