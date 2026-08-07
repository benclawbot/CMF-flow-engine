# Research model and design principles

This project is an N-of-1 personal flow engine. It should use research to constrain hypotheses, but it should learn the user's actual patterns rather than assuming population averages are personally true.

## 1. Separate flow experience from its antecedents

A recurring measurement problem in flow research is mixing the experience of flow with conditions thought to produce it.

The engine therefore separates:

### Flow experience (labels)

Primary dimensions:

- absorption;
- effortless control;
- intrinsic reward.

These are treated as the core subjective experience. A global flow score may be derived or requested, but the dimensions are retained independently.

Secondary/extended dimensions can include:

- time distortion;
- reduced self-consciousness;
- desire to continue;
- perceived session quality.

### Flow antecedents (predictors/intervention targets)

- challenge/skill match;
- goal clarity;
- feedback immediacy;
- perceived control;
- intrinsic interest;
- mastery orientation;
- interruption burden;
- attention fragmentation;
- physiological and recovery context;
- social/environmental context.

The engine must not bake antecedents into the label, otherwise it cannot learn whether those antecedents actually predict flow for this user.

## 2. Self-report is the reference label

Current physiological flow research is promising but not mature enough to make consumer-wearable signals ground truth.

The system should:

1. collect lightweight momentary self-reports;
2. align phone/watch/context data to those reports;
3. learn personal associations;
4. report uncertainty;
5. abstain when confidence or source quality is poor.

Vendor-derived stress/readiness scores are contextual features only.

## 3. Use ecological momentary assessment

Flow varies within a person and across situations. The system therefore favors repeated in-context measurements over personality-style assumptions.

Sampling should minimize the measurement burden and avoid destroying the state being measured.

Recommended strategy:

- short post-episode or natural-transition probes;
- occasional richer probes for construct validation;
- event-triggered probes after task switches or sustained sessions when interruption risk is low;
- no repeated prompts during an apparently successful protected session.

## 4. Model domains separately before assuming universality

Flow during coding, cooking, exercise, conversation, reading, learning and family activity may have different observable signatures.

The model should initially include `domain` as context and later compare:

- one global personalized model;
- global model + domain features;
- domain-specific models when enough data exists.

Model complexity must be justified by out-of-sample improvement.

## 5. Challenge/skill balance is important, not sufficient

Research supports challenge/skill match as a meaningful antecedent, but not the sole cause of flow. Clear goals and perceived control also matter.

The controller should therefore diagnose multiple failure modes:

- too easy -> increase challenge;
- too hard -> reduce scope or offer AI assistance;
- unclear -> clarify goal;
- low control -> restructure task/environment;
- fragmented -> reduce avoidable interruptions;
- low intrinsic engagement -> reframe toward mastery, switch activity when appropriate, or accept that the task may simply be necessary rather than flow-producing.

## 6. Interruptions are contextual, not universally bad

Interruption frequency and relevance matter. Some interruptions disrupt flow while relevant collaborative interruptions can advance work or collaborative flow.

The engine should eventually distinguish:

- external vs self-initiated interruption;
- relevant vs irrelevant interruption;
- beneficial vs harmful outcome;
- individual vs collaborative activity.

Do not encode `notification_count > N` as inherently bad without personal evidence.

## 7. Physiology should be modeled nonlinearly

Flow physiology does not appear to reduce to a simple rule such as "low heart rate = flow." Published work suggests nonlinear and task-dependent autonomic patterns.

For the CMF Watch Pro 2 layer:

- prefer personal baselines over population thresholds;
- use deltas/trends rather than isolated values;
- distinguish physical activity from sedentary cognitive work;
- preserve raw timestamps and provenance;
- do not infer HRV unless the underlying data supports a valid calculation;
- do not infer psychological stress directly from a vendor stress score.

## 8. Optimize sustainable utility, not flow minutes

Flow is not always the correct objective. Recovery, reflection, family time, sleep, necessary administration, boredom and mind-wandering can all be valuable.

The policy objective is approximately:

`expected_utility = meaningful_outcome * flow_quality * sustainability - fatigue_cost - interruption_cost`

This objective should be evaluated over multiple horizons:

- immediate session;
- later same day;
- next day;
- rolling week.

A recommendation that improves immediate flow but reliably increases later fatigue can therefore become a bad policy.

## 9. Learn intervention effects, not only correlations

Observation can discover associations but cannot reliably establish what causes better flow.

The experiment engine should support randomized or counterbalanced N-of-1 experiments where practical, including:

- session length;
- notification exposure;
- music/silence;
- physical activity before cognitively demanding work;
- explicit goal clarification;
- challenge manipulation;
- AI assistance timing;
- task ordering.

Experiments should define a primary outcome before assignment and preserve contextual confounders.

## 10. Protect flow by reducing system activity

The system itself can become the interruption.

Policy hierarchy during likely high-quality flow:

1. `NO_ACTION`;
2. `PROTECT_FLOW`;
3. defer noncritical prompts;
4. intervene only for strong long-horizon reasons such as substantial fatigue risk or a hard external constraint.

## Evidence anchors

Key research informing these principles includes:

- Norsworthy et al., *Psychological Flow Scale (PFS): Development and Preliminary Validation of a New Flow Instrument that Measures the Core Experience of Flow to Reflect Recent Conceptual Advancements* (International Journal of Applied Positive Psychology, 2023): proposes absorption, effortless control and intrinsic reward as core experiential dimensions and explicitly separates them from antecedents.
- Wojtasiński et al., *The flow experience: Polish adaptation and validation of the psychological flow scale (PFS)* (PLOS ONE, 2025): supports the three-facet hierarchical structure at within-person and between-person levels and sensitivity to momentary change.
- Fong, Zaleski & Leach, *The challenge-skill balance and antecedents of flow: A meta-analytic investigation* (Journal of Positive Psychology, 2015): challenge-skill balance is a robust but moderate contributor, alongside clear goals and sense of control.
- Fullagar & Kelloway, *Flow at work: An experience sampling approach* (Journal of Occupational and Organizational Psychology, 2009): supports strong situational variation and experience-sampling approaches.
- Wonders, Hodgson & Whitton, *Measuring Flow: Refining Research Protocols That Integrate Physiological and Psychological Approaches* (Human Behavior and Emerging Technologies, 2025): documents methodological weaknesses in physiological flow studies and argues for validated psychological verification.
- Rácz et al., *Physiological assessment of the psychological flow state using wearable devices* (Scientific Reports, 2025): shows feasibility of physiological sensing while also illustrating small-sample, controlled-task limitations and nonlinear patterns.
- Ritonummi et al., *The Impact of Interruptions on Different Types of Flow in Collaborative Software Development Work* (ICIS, 2024): highlights that interruptions can disrupt or advance flow depending on context.

These references guide architecture and experiments; they are not treated as proof that any population-level effect applies to this individual.
