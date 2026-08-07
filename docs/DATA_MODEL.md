# Data model

All timestamps should be UTC internally with timezone captured for presentation/context.

## `self_report`

- `id`
- `captured_at`
- `flow_score` integer 0..5
- `fatigue_score` integer 0..5
- `task_id` nullable
- `activity_label` nullable
- `session_quality` nullable
- `notes` nullable

## `task`

- `id`
- `title`
- `domain` (work, learning, family, exercise, leisure, admin, etc.)
- `value_score`
- `urgency_score`
- `difficulty_score` user-defined
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

## `context_snapshot`

- `id`
- `captured_at`
- `time_of_day_features`
- `recent_screen_time`
- `app_switch_count`
- `notification_count`
- `recent_activity_features`
- `sleep_features`
- `heart_rate_features`
- `other_health_features`

Store derived features with version/provenance so model changes remain auditable.

## `recommendation`

- `id`
- `created_at`
- `action_type`
- `task_id` nullable
- `reason_codes`
- `model_version`
- `accepted` nullable
- `responded_at` nullable

Allowed actions include:

- `NO_ACTION`
- `CONTINUE`
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
