# Flow Agent

A local-first personal system for learning the conditions that support subjective flow while minimizing long-term fatigue across work and life.

## Goals

- Optimize for **subjective flow**, not raw screen time or productivity.
- Minimize cumulative fatigue and protect long-term performance.
- Learn a personalized model from self-reports and passive context.
- Recommend actions rather than forcibly controlling behavior.
- Prioritize tasks using AI based on value, urgency, difficulty, current state, and expected flow fit.
- Support personal experiments to discover causal effects.
- Keep sensitive data local by default.

## Initial hardware / surfaces

- Android phone — primary compute, storage, self-report, task planning, notifications.
- CMF Watch Pro 2 — heart rate, sleep, activity and other accessible health context through Health Connect first; direct BLE is a later fallback/research path.
- Laptop integration — later phase.

## Core loop

`Observe → Estimate state → Recommend → Self-report → Learn`

The primary ground-truth label is a lightweight subjective flow score supplied by the user.

## MVP

1. Android app with local encrypted persistence.
2. Health Connect capability probe and ingestion.
3. 0–5 flow and fatigue self-reports.
4. Context snapshots around reports.
5. Manual task list with user-defined difficulty.
6. AI-assisted task prioritization.
7. Rule-based recommendation engine first.
8. Personal experiment framework.
9. Local analytics and export.
10. Prediction only after enough labeled data exists.

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md), [docs/MVP.md](docs/MVP.md), and [docs/DATA_MODEL.md](docs/DATA_MODEL.md).

## Principles

- The system may recommend **do nothing**.
- Physiological signals are noisy context, not medical truth.
- Rejected recommendations are useful feedback.
- No cloud dependency is required for the core loop.
- Models should start simple and become more sophisticated only when the data justifies it.

## Status

Repository scaffold / design phase.
