# Architecture

## System objective

Estimate two operational quantities:

- `flow_probability`: likelihood of a good subjective-flow period given current state and candidate activity.
- `fatigue_risk`: likelihood that continuing or selecting an activity degrades later performance/recovery.

The policy engine selects recommendations that improve expected flow while keeping fatigue risk below a learned personal threshold.

## High-level components

### Android application

The phone is the primary node.

- `HealthDataProvider`
  - Health Connect first.
  - Future `CmfBleProvider` behind the same interface.
- `ContextCollector`
  - time/day
  - device usage summaries where Android permissions permit
  - recent notifications/counts where permitted
  - activity context
- `SelfReportService`
  - flow 0–5
  - fatigue 0–5
  - current activity/task
  - optional session-quality annotation
- `TaskRepository`
  - task value
  - urgency
  - user-defined difficulty
  - category/life domain
  - estimated effort
- `StateEstimator`
  - starts rule/statistics based
  - later personalized ML
- `PriorityEngine`
  - candidate ranking by expected outcome and context fit
- `PolicyEngine`
  - generates recommendations
  - explicitly supports `NO_ACTION`
- `ExperimentEngine`
  - randomized/pseudo-randomized personal experiments
- `LocalStore`
  - encrypted local database
  - provenance for every derived feature

## Recommendation policy examples

- High flow probability + low fatigue risk → protect session / no interruption.
- Medium flow + low fatigue → suggest task/environment adjustment.
- Low flow + repeated struggle → offer AI decomposition/assistance.
- High engagement + rising fatigue → suggest planned stopping point.
- Low energy + high cognitive demand → suggest lower-demand valuable task.
- Poor recovery context → lower planned cognitive load.

## Data flow

```text
CMF Watch Pro 2 ──> Nothing X / Health Connect ─┐
                                                 │
Android context ─────────────────────────────────┼─> Local normalized store
                                                 │           │
Self reports ────────────────────────────────────┘           ▼
                                                     State estimator
                                                           │
Tasks ─────────────────────────────────────────────────────┤
                                                           ▼
                                                  Priority + policy
                                                           │
                                                           ▼
                                                   Recommendation
                                                           │
                                                           ▼
                                                acceptance / rejection
                                                           │
                                                           └──> learning
```

## Direct BLE path

Do not couple the MVP to Nothing's private protocol. A future BLE provider may use independently understood protocol behavior, but normalized domain records must remain independent of source.
