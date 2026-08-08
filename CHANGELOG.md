# Changelog

## 1.0.0 — Release candidate

### Product

- Rebuilt the Android experience around Home, Insights, Tasks, Experiments and Settings.
- Added one-time onboarding and a real adaptive launcher icon.
- Added system light/dark theme support and polished Material 3 visual hierarchy.
- Added a quick subjective check-in flow and dedicated current-state presentation.
- Added state-aware task ranking and session-aware intervention recommendations.
- Added focus sessions with explicit struggle tracking.
- Added outcome learning for recommendations and interventions.
- Added privacy-preserving attention-fragmentation sensing.
- Added Health Connect integration for CMF Watch Pro 2 / Nothing X context.
- Added balanced randomized N-of-1 experiments with linked follow-up outcomes and minimum-evidence result thresholds.
- Added optional check-in reminders.
- Completed the product loop from recommendations and shared tasks through outcome check-ins, evidence-backed experiments and learned insights.
- Added context-aware check-ins that reuse active task context and suppress redundant reminders after recent outcomes.

### Health Connect

- Fixed Health Connect permission-client registration.
- Added permission rationale flow.
- Fixed record pagination so high-volume signals such as heart rate are not silently truncated at the first page.
- Confirmed real-device Nothing X export for heart rate, sleep, steps and SpO₂ under `com.nothing.smartcenter`.

### Privacy and reliability

- Data remains local-first with no CMF Flow cloud account required.
- Notification content and raw app-transition histories are not persisted.
- Android application backup is disabled.
- Existing Room migrations are non-destructive.
- CI gates Android lint, unit tests and debug APK generation.

### Remaining hardware-only validation

GitHub issue #1 tracks two non-software-blocking checks on the physical CMF Watch Pro 2: fresh heart-rate recency using the paginated probe and exercise-session export after a known workout.
