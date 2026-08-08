# CMF Flow Privacy

CMF Flow 1.0 is a personal, local-first Android application.

## Data the app can store

CMF Flow may store the following on the Android device:

- Subjective check-ins: flow, absorption, effortless control, enjoyment, presence, fatigue and optional context labels.
- Task metadata entered by the user.
- Focus-session timing and explicit struggle marks.
- Recommendation/intervention events and the user's responses.
- Experiment definitions, randomized trial assignments and linked check-in outcomes.
- Health-context aggregates collected through Health Connect, such as heart-rate summaries, sleep duration and step context.
- Attention-context aggregates such as app-switch count, unlock count, screen transitions and notification count.

## Data the app does not intentionally store

- Notification titles, bodies, senders or message contents.
- A persistent sequence of app/package names from Usage Access.
- Passwords or authentication secrets.
- A cloud copy of the CMF Flow database.

## Network and accounts

The core CMF Flow product does not require a CMF Flow cloud account and does not upload the local learning database as part of its normal operation.

Health Connect and Nothing X are separate Android/system applications with their own data handling and permissions.

## Android backup

CMF Flow disables Android application backup in its manifest so the app database is not included in normal app backup flows.

## Storage protection

The database is stored inside the Android application sandbox and benefits from the device's normal storage protections. CMF Flow 1.0 does not add independent SQLCipher-style database encryption, and the project does not claim that it does.

## Permissions

- **Health Connect read permissions:** optional; used for local health context.
- **Usage Access:** optional; used to calculate aggregate context-switch counts.
- **Notification Listener:** optional; used only to count notification events locally.
- **Notification permission:** optional; used for check-in reminders.

The app remains usable when optional sensing permissions are not granted; unavailable signals remain unavailable rather than being fabricated.

## Control

Permissions can be revoked at any time in Android settings. Deleting the application removes its private local app data under normal Android behavior.

## Scope

CMF Flow is not a medical device and its recommendations are not medical advice. Physiological signals are treated as noisy contextual evidence rather than diagnoses or medical truth.
