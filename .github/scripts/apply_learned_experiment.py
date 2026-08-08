from pathlib import Path


def replace_once(text: str, old: str, new: str, label: str) -> str:
    if old not in text:
        raise SystemExit(f"Anchor not found: {label}")
    return text.replace(old, new, 1)

engine_path = Path("app/src/main/java/com/benclawbot/cmfflow/experiments/ExperimentEngine.kt")
e = engine_path.read_text()
e = replace_once(
    e,
    '''data class ExperimentResult(
    val conditionA: ConditionResult,
    val conditionB: ConditionResult,
    val deltaAminusB: Double?,
    val evidenceReady: Boolean,
    val summary: String,
)
''',
    '''data class ExperimentResult(
    val conditionA: ConditionResult,
    val conditionB: ConditionResult,
    val deltaAminusB: Double?,
    val evidenceReady: Boolean,
    val summary: String,
)

data class LearnedExperimentRecommendation(
    val experimentId: Long,
    val condition: String,
    val utilityAdvantage: Double,
)
''',
    "learned experiment model",
)
e = replace_once(
    e,
    '''private fun utility(report: SelfReportEntity): Double =
    report.flowScore.toDouble() + report.presence.toDouble() - report.fatigue.toDouble()
''',
    '''fun learnedExperimentRecommendation(
    experiments: List<ExperimentEntity>,
    assignments: List<ExperimentAssignmentEntity>,
    reports: List<SelfReportEntity>,
): LearnedExperimentRecommendation? = experiments.asSequence()
    .map { experiment -> experiment to analyzeExperiment(experiment, assignments, reports) }
    .mapNotNull { (experiment, result) ->
        val delta = result.deltaAminusB ?: return@mapNotNull null
        if (!result.evidenceReady || kotlin.math.abs(delta) < 0.35) return@mapNotNull null
        LearnedExperimentRecommendation(
            experimentId = experiment.id,
            condition = if (delta > 0) experiment.conditionA else experiment.conditionB,
            utilityAdvantage = kotlin.math.abs(delta),
        )
    }
    .firstOrNull()

private fun utility(report: SelfReportEntity): Double =
    report.flowScore.toDouble() + report.presence.toDouble() - report.fatigue.toDouble()
''',
    "learned recommendation function",
)
engine_path.write_text(e)

product_path = Path("app/src/main/java/com/benclawbot/cmfflow/ProductApp.kt")
s = product_path.read_text()
s = replace_once(
    s,
    '''import com.benclawbot.cmfflow.experiments.analyzeExperiment
import com.benclawbot.cmfflow.experiments.chooseNextCondition''',
    '''import com.benclawbot.cmfflow.experiments.analyzeExperiment
import com.benclawbot.cmfflow.experiments.chooseNextCondition
import com.benclawbot.cmfflow.experiments.learnedExperimentRecommendation''',
    "learned experiment import",
)
s = replace_once(
    s,
    '''    val experimentSuggestionAvailable = experiments.isEmpty() && openTrial == null && reports.size >= 3
    val experimentSuggestion = if (experimentSuggestionAvailable) suggestedExperiment(reports, contexts) else null

    var interventionEventId''',
    '''    val experimentSuggestionAvailable = experiments.isEmpty() && openTrial == null && reports.size >= 3
    val experimentSuggestion = if (experimentSuggestionAvailable) suggestedExperiment(reports, contexts) else null
    val learnedExperiment = learnedExperimentRecommendation(experimentHistory, assignments, reports)
        ?.takeIf { openTrial == null && intervention.action.name == "CONTINUE" }
    val effectiveInterventionTitle = learnedExperiment?.condition ?: friendlyAction(intervention.action.name)
    val effectiveInterventionReason = learnedExperiment?.let {
        "A balanced personal experiment associated this condition with a ${"%.2f".format(it.utilityAdvantage)} higher follow-up utility."
    } ?: friendlyReason(intervention.reasons)
    val interventionReasonLabel = if (learnedExperiment != null) "Learned from your experiment" else "Why now"
    val interventionActionKey = learnedExperiment?.let { "EXPERIMENT:${it.experimentId}:${it.condition}" } ?: intervention.action.name
    val interventionReasonKeys = learnedExperiment?.let {
        listOf("balanced_experiment_evidence", "utility_advantage=${"%.2f".format(it.utilityAdvantage)}")
    } ?: intervention.reasons

    var interventionEventId''',
    "effective learned intervention",
)
s = replace_once(
    s,
    '''    LaunchedEffect(intervention.action, intervention.reasons, session?.id, session?.struggleCount) {
        interventionEventId = recordIntervention(
            InterventionEventEntity(
                action = intervention.action.name,
                reasonsSnapshot = intervention.reasons.joinToString("|"),
            ),
        )
    }''',
    '''    LaunchedEffect(interventionActionKey, interventionReasonKeys, session?.id, session?.struggleCount) {
        interventionEventId = recordIntervention(
            InterventionEventEntity(
                action = interventionActionKey,
                reasonsSnapshot = interventionReasonKeys.joinToString("|"),
            ),
        )
    }''',
    "record learned intervention",
)
s = replace_once(
    s,
    '''                    interventionTitle = friendlyAction(intervention.action.name),
                    interventionReason = friendlyReason(intervention.reasons),''',
    '''                    interventionTitle = effectiveInterventionTitle,
                    interventionReason = effectiveInterventionReason,
                    interventionReasonLabel = interventionReasonLabel,''',
    "Home learned intervention args",
)
s = replace_once(
    s,
    '''    interventionTitle: String,
    interventionReason: String,
    topTask: TaskEntity?,''',
    '''    interventionTitle: String,
    interventionReason: String,
    interventionReasonLabel: String,
    topTask: TaskEntity?,''',
    "Home learned intervention signature",
)
s = replace_once(
    s,
    '''                Text("Why now", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                Text(interventionReason, color = MaterialTheme.colorScheme.onSurfaceVariant)''',
    '''                Text(interventionReasonLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                Text(interventionReason, color = MaterialTheme.colorScheme.onSurfaceVariant)''',
    "learned intervention label",
)
product_path.write_text(s)

test_path = Path("app/src/test/java/com/benclawbot/cmfflow/experiments/ExperimentEngineTest.kt")
t = test_path.read_text()
insert = '''
    @Test
    fun learnedRecommendationUsesWinningConditionAfterEvidenceThreshold() {
        val reports = (1L..8L).map { id ->
            if (id <= 4) report(id, flow = 5, presence = 5, fatigue = 1)
            else report(id, flow = 3, presence = 3, fatigue = 3)
        }
        val assignments = (1L..4L).map { assignment(it, "Walk first") } +
            (5L..8L).map { assignment(it, "No walk") }

        val learned = learnedExperimentRecommendation(listOf(experiment), assignments, reports)
        assertEquals("Walk first", learned?.condition)
        assertTrue((learned?.utilityAdvantage ?: 0.0) >= 0.35)
    }

    @Test
    fun learnedRecommendationStaysOffWhenConditionsAreSimilar() {
        val reports = (1L..8L).map { id -> report(id, flow = 4, presence = 4, fatigue = 2) }
        val assignments = (1L..4L).map { assignment(it, "Walk first") } +
            (5L..8L).map { assignment(it, "No walk") }

        assertEquals(null, learnedExperimentRecommendation(listOf(experiment), assignments, reports))
    }
'''
t = replace_once(
    t,
    '''    private fun assignment(reportId: Long, condition: String) = ExperimentAssignmentEntity(''',
    insert + '''
    private fun assignment(reportId: Long, condition: String) = ExperimentAssignmentEntity(''',
    "learned experiment tests",
)
test_path.write_text(t)
