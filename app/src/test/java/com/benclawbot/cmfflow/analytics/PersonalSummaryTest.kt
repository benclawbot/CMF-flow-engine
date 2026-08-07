package com.benclawbot.cmfflow.analytics

import com.benclawbot.cmfflow.data.SelfReportEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PersonalSummaryTest {
    @Test
    fun strongestDomainRequiresAtLeastThreeSamples() {
        val reports = listOf(
            report(flow = 5, fatigue = 1, presence = 5, domain = "coding"),
            report(flow = 5, fatigue = 2, presence = 4, domain = "coding"),
            report(flow = 3, fatigue = 2, presence = 3, domain = "reading"),
        )

        val summary = summarize(reports)

        assertEquals(3, summary.sampleCount)
        assertNull(summary.strongestDomain)
    }

    @Test
    fun strongestDomainUsesOnlyDomainsWithEnoughSamples() {
        val reports = listOf(
            report(flow = 5, fatigue = 1, presence = 5, domain = "coding"),
            report(flow = 4, fatigue = 2, presence = 4, domain = "coding"),
            report(flow = 5, fatigue = 1, presence = 5, domain = "coding"),
            report(flow = 5, fatigue = 2, presence = 5, domain = "reading"),
            report(flow = 5, fatigue = 2, presence = 5, domain = "reading"),
        )

        assertEquals("coding", summarize(reports).strongestDomain)
    }

    private fun report(flow: Int, fatigue: Int, presence: Int, domain: String?) = SelfReportEntity(
        capturedAtEpochMs = 0,
        flowScore = flow,
        absorption = flow,
        effortlessControl = flow,
        intrinsicReward = flow,
        presence = presence,
        fatigue = fatigue,
        activityLabel = null,
        domain = domain,
        taskDifficulty = null,
        goalClarity = null,
        perceivedControl = null,
        notes = null,
    )
}
