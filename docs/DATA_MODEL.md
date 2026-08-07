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
- `session_quality` nullable
- `notes` nullable

The three core dimensions (absorption, effortless control, intrinsic reward) should be prioritized for frequent sampling. Longer probes can occasionally collect the broader dimensions to avoid excessive interruption.

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

- `id`
- `captured_at`
- `time_of_day_features`
- `recent_screen_time`
- `app_switch_count`
- `notification_count`
- `unlock_count`
- `interruption_count`
- `interruption_relevance` nullable
- `recent_activity_features`
- `sleep_features`
- `heart_rate_features`
- `other_health_features`
- `environment_features` nullable
- `social_context` nullable

Store derived features with version/provenance so model changes remain auditable.

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
