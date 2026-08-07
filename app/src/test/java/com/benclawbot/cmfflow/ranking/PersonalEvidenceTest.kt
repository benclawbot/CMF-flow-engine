package com.benclawbot.cmfflow.ranking

import com.benclawbot.cmfflow.data.SelfReportEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

class PersonalEvidenceTest {
    private val utc = ZoneId.of("UTC")

    @Test
    fun `domain evidence requires at least three samples`() {
        val reports = listOf(
            report(1, "work", 5),
            report(2, "work", 5),
            report(3, "family", 2),
            report(4, "family", 2),
        )

        val evidence = personalEvidenceFor("work", reports, nowEpochMs = noonMs(), zoneId = utc)

        assertEquals(0.0, evidence.adjustment, 0.0001)
        assertTrue(evidence.reasons.none { it.startsWith("personal_domain") })
    }

    @Test
    fun `repeated high flow domain receives bounded positive evidence`() {
        val reports = listOf(
            report(1, "work", 5), report(2, "work", 5), report(3, "work", 4),
            report(4, "admin", 1), report(5, "admin", 2), report(6, "admin", 2),
        )

        val evidence = personalEvidenceFor("work", reports, nowEpochMs = noonMs(), zoneId = utc)

        assertTrue(evidence.adjustment > 0.0)
        assertTrue(evidence.adjustment <= 2.0)
        assertTrue(evidence.reasons.contains("personal_domain_n=3"))
    }

    @Test
    fun `time evidence requires five comparable samples`() {
        val reports = (1L..4L).map { report(it, "work", 5, capturedAt = noonMs() + it * 1_000) } +
            listOf(report(10, "admin", 1, capturedAt = midnightMs()))

        val evidence = personalEvidenceFor(null, reports, nowEpochMs = noonMs(), zoneId = utc)

        assertEquals(0.0, evidence.adjustment, 0.0001)
        assertTrue(evidence.reasons.none { it.startsWith("personal_time") })
    }

    private fun report(id: Long, domain: String, flow: Int, capturedAt: Long = midnightMs()) = SelfReportEntity(
        id = id,
        capturedAtEpochMs = capturedAt,
        flowScore = flow,
        absorption = flow,
        effortlessControl = flow,
        intrinsicReward = flow,
        presence = flow,
        fatigue = 2,
        activityLabel = null,
        domain = domain,
        taskDifficulty = null,
        goalClarity = null,
        perceivedControl = null,
        notes = null,
    )

    private fun noonMs() = 12L * 60L * 60L * 1000L
    private fun midnightMs() = 0L
}
