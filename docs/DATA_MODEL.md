# Data model

All timestamps should be UTC internally with timezone captured for presentation/context.

The model treats self-report as the primary label for subjective flow. Wearable and behavioral signals are predictors/evidence, never ground truth by themselves.

## `self_report`

- `id`
- `captured_at`
- `global_flow_score` integer 0..5
- `absorption_score` integer 0..5
- `effortless_control_score` integer 0..5
- `intrinsic_reward_score` integer 0..5
- `time_distortion_score` nullable integer 0..5
- `self_consciousness_score` nullable integer 0..5
- `presence_score` integer 0..5
- `fatigue_score` integer 0..5
- `mood_valence` nullable integer -2..2
- `task_id` nullable
- `activity_label` nullable
- `domain` nullable
- `task_difficulty_score` nullable integer 0..5
- `goal_clarity_score` nullable integer 0..5
- `perceived_control_score` nullable integer 0..5
- `session_quality` nullable
- `notes` nullable

The three core dimensions (absorption, effortless control, intrinsic reward) should be prioritized for frequent sampling. Longer probes can occasionally collect the broader dimensions to avoid excessive interruption. Antecedent fields remain optional so they do not become part of the label by construction.

## `task`

- `id`
- `title`
- `domain` (deep_work, creative, learning, cooking, conversation, family, physical_activity, play, reading, admin, recovery, etc.)
- `value_score`
- `urgency_score`
- `difficulty_score` user-defined
- `skill_match_score` nullable
- `goal_clarity_score` nullable
- `feedback_immediacy_score` nullable
- `perceived_control_score` nullable
- `intrinsic_interest_score` nullable
- `mastery_value_score` nullable
- `external_pressure_score` nullable
- `social_evaluation_score` nullable
- `estimated_minutes`
- `status`
- `created_at`
- `due_at` nullable

## `health_sample`

- `id`
- `type`
- `start_at`
- `end_at` nullable
- `value`
- `unit`
- `source_app`
- `source_device` nullable
- `raw_metadata` nullable
- `quality_status` (active, limited, stale, unavailable)
- `confidence` nullable

Consumer wearable stress/readiness values must be stored as vendor-derived observations, not treated as direct psychological-state labels.

## `context_snapshot`

Each frequent self-report is paired with one local context snapshot. Current implementation records:

- `id`
- `self_report_id`
- `captured_at`
- `window_start`
- `window_end`
- `local_hour`
- `local_day_of_week`
- `battery_percent` nullable
- `is_charging` nullable
- `is_phone_interactive` nullable
- heart-rate record count
- heart-rate sample count
- heart-rate min/max/mean within the context window
- step count within the context window
- sleep minutes during the previous 24 hours
- Health Connect data-origin package names
- `collection_error` nullable

Planned context additions include:

- recent screen time
- app switch count
- notification count
- unlock count
- interruption count and relevance
- environment features
- social context

Store derived features with version/provenance so model changes remain auditable. Missing permissions or unavailable sources should produce explicit missing/error state, never silently fabricated values.

## `flow_episode`

A derived episode reconstructed from self-reports and context rather than asserted directly by sensors.

- `id`
- `start_at`
- `end_at`
- `task_id` nullable
- `domain`
- `flow_probability`
- `flow_quality`
- `label_source`
- `model_version`
- `confidence`

## `recommendation`

- `id`
- `created_at`
- `action_type`
- `task_id` nullable
- `reason_codes`
- `model_version`
- `accepted` nullable
- `responded_at` nullable
- `later_outcome` nullable

Allowed actions include:

- `NO_ACTION`
- `CONTINUE`
- `PROTECT_FLOW`
- `CLARIFY_GOAL`
- `REDUCE_INTERRUPTION`
- `SWITCH_TASK`
- `INCREASE_CHALLENGE`
- `REDUCE_CHALLENGE`
- `OFFER_AI_HELP`
- `MOVE`
- `BREAK`
- `EXERCISE`
- `RECOVER`
- `STOP_SESSION`

## `experiment`

- `id`
- `hypothesis`
- `intervention_a`
- `intervention_b`
- `primary_outcome`
- `secondary_outcomes`
- `start_at`
- `end_at`
- `status`

## `experiment_assignment`

- `id`
- `experiment_id`
- `assigned_condition`
- `assigned_at`
- `completed_at` nullable
- `outcome_reference`
- `context_reference` nullable

## Modeling principle

Optimize expected long-term value, not maximum instantaneous flow:

`utility = meaningful_outcome × flow_quality × sustainability - fatigue_cost - interruption_cost`

The engine must preserve recovery, family/personal obligations, reflection, and necessary low-flow tasks rather than treating all non-flow states as failures.
