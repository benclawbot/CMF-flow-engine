# Encrypted local export and restore

**Status:** Opt-in encrypted backup implementation on the app's Settings screen. No automatic backup or cloud account. Validate a round-trip on a physical device before treating a backup as your only recovery copy.

## Using it

1. Open **Settings → Encrypted backup**.
2. Choose a passphrase of at least 12 characters, and confirm it. If you lose the passphrase, **the archive cannot be recovered**.
3. Choose **Export encrypted backup** and select your destination through Android's Storage Access Framework.
4. To restore, enter the passphrase, choose **Open encrypted backup and preview restore**, select the .cmfbak file, review the record count, and explicitly confirm **Replace local data**. Existing local data is overwritten only after a successful decryption and schema check.
5. Keep the original backup in a safe location until a separate restore on a test device has succeeded.

An exported archive contains all local Room tables: check-ins, task state, experiment history and assignments, session data, recommendation/intervention records, and stored context aggregates (including health-derived summaries). No notification text or raw app-switch history is added. A chosen document provider may sync the encrypted file to the cloud if **you** select one.

## Cryptography and format

The portable archive is versioned (\`CMFBK001\`) and encrypted with **AES-256-GCM**. A fresh 16-byte salt and 12-byte nonce are generated for every export. The passphrase derives a key using **PBKDF2-HMAC-SHA256 with 600,000 iterations**, an Android API 28-compatible platform primitive used instead of Argon2id. The entire archive header, including format version and KDF parameters, is authenticated as GCM additional data. Decryption rejects corruption or incorrect passphrases without returning partial data.

Limits: 8 MiB unencrypted JSON snapshot; 30,000 rows total; 8 MiB plus header/tag ciphertext. Unknown archive formats, altered headers, invalid table names/columns, extra tables and mismatching Room schema versions fail closed.

## Restore boundaries

A user-confirmed restore validates all table/column shapes before replacing data in a single Room database transaction. Any SQL constraint or storage error rolls the transaction back. The import does not execute arbitrary SQL from the archive, does not write files supplied by the archive, and does not request network permissions.

Do not open backups from untrusted sources. The file still contains sensitive data, even though its contents are encrypted; protect both the archive and the passphrase. A cancelled document picker leaves existing local data unchanged.

## Remaining release validation

Cryptographic unit tests cover round-trips, wrong passphrases, tampering, nonce uniqueness, and archive limits. Android CI covers Kotlin compilation, tests, lint and APK build. A physical Android device test must still verify a populated database restore, low-storage interruption, process death, and recovery after application upgrade. The export/import workflow should not be treated as a validated disaster-recovery solution until that drill is completed.

Automatic Android app backup remains disabled, and no cloud service is introduced.
