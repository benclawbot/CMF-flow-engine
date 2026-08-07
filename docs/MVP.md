# MVP plan

## Phase 0 — capability probe

Build a minimal Android diagnostic screen that:

1. requests Health Connect permissions;
2. enumerates available relevant record types;
3. lists data origins;
4. prints timestamps and sampling intervals;
5. measures synchronization latency after the watch syncs;
6. compares accessible records with Nothing X displays;
7. records whether heart rate, sleep, exercise, steps, SpO2 and stress-like data are available.

Output a capability matrix before designing assumptions around watch data.

## Phase 1 — labeled personal dataset

Collect a deliberately short ecological-momentary-assessment label:

- global flow 0–5;
- absorption 0–5;
- effortless control 0–5;
- intrinsic reward 0–5;
- presence 0–5;
- fatigue 0–5;
- current task/activity;
- task difficulty 0–5;
- goal clarity 0–5;
- contextual snapshot;
- recommendation response when applicable.

Occasionally run an extended probe that also asks about time distortion, self-consciousness, perceived control, challenge/skill match, and motivation source. This gives richer validation without interrupting every session.

No automatic flow detection is required yet. Wearable measurements are features only; subjective reports remain the primary labels.

## Phase 2 — recommendations

Start with transparent rules and personal statistics. Examples:

- protect a high-quality ongoing session by recommending no action;
- clarify a vague goal before changing task difficulty;
- suppress/reduce avoidable interruptions when fragmentation is high;
- suggest a high-value, high-fit task;
- offer AI assistance after prolonged struggle;
- increase or reduce challenge based on reported skill match;
- suggest stopping when fatigue risk rises;
- avoid treating necessary recovery or low-flow activities as failures.

## Phase 3 — personal prediction

Only after sufficient labels:

- predict next-period subjective flow probability and quality;
- predict individual dimensions (absorption, effortless control, intrinsic reward) rather than only one score;
- predict later fatigue cost;
- model domains separately when evidence supports it;
- validate calibration against held-out observations;
- retain simple baselines for comparison;
- abstain when data quality or confidence is poor.

Prefer simple interpretable models first. The project is an N-of-1 longitudinal system; model complexity must be earned by evidence.

## Phase 4 — experiments

Support N-of-1 experiments such as:

- 60 vs 90 minute sessions;
- physical activity before demanding work;
- music vs silence;
- AI assistance timing;
- task order;
- high vs low notification exposure;
- explicit goal clarification vs normal task start;
- different challenge levels for the same activity.

Experiments must store hypothesis, intervention, outcome, dates, context and known confounders.

## Intervention policy

The controller should distinguish:

- `ENTER`: create better preconditions for flow;
- `BOOST`: adjust challenge, clarity, feedback or assistance;
- `MAINTAIN`: minimize unnecessary intervention during good flow;
- `RECOVER`: protect long-term capacity when fatigue risk dominates.

`NO_ACTION` and `PROTECT_FLOW` are first-class actions.

## Full product direction

Add laptop agent, richer context, encrypted local sync, adaptive prioritization across life domains, personalized experimentation, domain-specific flow models, collaborative-flow context, and optional direct watch BLE ingestion.

The long-term objective is not maximum flow minutes. It is maximizing meaningful sustainable outcomes while learning when flow helps, what reliably precedes it, and when not to pursue it.
