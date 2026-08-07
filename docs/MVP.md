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

Collect:

- flow 0–5;
- fatigue 0–5;
- current task/activity;
- task difficulty 0–5;
- contextual snapshot;
- recommendation response when applicable.

No automatic flow detection is required yet.

## Phase 2 — recommendations

Start with transparent rules and personal statistics. Examples:

- suggest a high-value, high-fit task;
- offer AI assistance after prolonged struggle;
- suggest stopping when fatigue risk rises;
- avoid interrupting an apparently successful session.

## Phase 3 — personal prediction

Only after sufficient labels:

- predict next-period subjective flow;
- predict later fatigue cost;
- validate calibration against held-out observations;
- retain simple baselines for comparison.

## Phase 4 — experiments

Support N-of-1 experiments such as:

- 60 vs 90 minute sessions;
- physical activity before demanding work;
- music vs silence;
- AI assistance timing;
- task order.

Experiments must store hypothesis, intervention, outcome, dates, and confounders.

## Full product direction

Add laptop agent, richer context, encrypted local sync, adaptive prioritization across life domains, personalized experimentation, and optional direct watch BLE ingestion.
