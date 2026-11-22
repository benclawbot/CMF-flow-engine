# Optional encrypted export: implementation plan

**Status: design proposal, not a shipped feature.** CMF Flow is local-first and Android automatic backup is disabled by default. Export must be explicit, user-initiated, and opt-in.

## Proposed user flow

1. In Settings choose **Export an encrypted backup**.
2. Explain which data categories will be included: task and experiment history, check-ins, local learning state, and any stored health aggregates. Never export raw notification text or package transition history (the app does not store those).
3. Ask the user for a fresh passphrase and show a separate recovery warning. Do not persist the passphrase.
4. Use Android Storage Access Framework (ACTION_CREATE_DOCUMENT) to write an encrypted versioned archive only to the location selected by the user.
5. Offer a **preview-only import** through ACTION_OPEN_DOCUMENT, display archive version, counts and categories, and require explicit confirmation before changing a database.
6. Validate a backup and restore on physical Android hardware, including interruption, large files and low-storage failure.

## Threat and format requirements

- Use a maintained authenticated-encryption implementation (AES-256-GCM or equivalent) and a memory-hard passphrase KDF (Argon2id or a documented Android-supported alternative) with per-export random salt, nonce and KDF parameters stored in a versioned header.
- Authenticate metadata, prevent path traversal if archiving files, limit decoded size, and refuse unknown versions.
- Never stream plaintext to logs, crash reports, analytics, third-party services or shared storage.
- Use transactional restore into a temporary database with integrity/schema checks before swapping into the app's live storage.
- Preserve the privacy model: no cloud account, no scheduled upload, no default automatic backup.
- Document that losing the passphrase makes recovery impossible.

## Verification gate before release

Unit tests for KDF/header compatibility, corrupt/wrong-passphrase archives, nonce uniqueness, upgrade migrations, round-trip data fidelity, interrupted writes and rollback. Run Android lint, all Room migration tests, UI automation, and a real-device export/import drill.

The existing disabled-Android-backup behavior should remain unchanged until the feature is fully implemented and independently reviewed.
