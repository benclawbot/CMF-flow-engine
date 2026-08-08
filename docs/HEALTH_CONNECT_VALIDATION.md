# CMF Watch Pro 2 → Health Connect validation

Validated on 2026-08-08 using a physical CMF Watch Pro 2 paired through Nothing X.

## Confirmed exports

- Heart rate: exported by `com.nothing.smartcenter`.
- Sleep: exported by `com.nothing.smartcenter`.
- Steps: exported by `com.nothing.smartcenter`.
- Oxygen saturation: exported by `com.nothing.smartcenter`; a fresh SpO₂ reading appeared at 2026-08-08 07:09:30.

## Probe pagination correction

The first probe implementation read only one Health Connect page. Heart-rate results repeatedly returned exactly 1000 records, so the apparent latest timestamp around 2026-08-05 was a page boundary, not evidence of a multi-day sync delay.

The probe now follows `ReadRecordsResponse.pageToken` until no more pages remain before calculating record count and time coverage.

## Still to validate

Exercise export remains inconclusive until a known workout is recorded on the watch, synced through Nothing X, and the probe is rerun.

Stress, Active Score, and Training Load are vendor-specific concepts and are not represented by the current standard Health Connect probe.

## Ingestion decision

Health Connect is sufficient as the default MVP ingestion path for heart rate, sleep, steps, and SpO₂. Direct BLE/Gadgetbridge-derived ingestion should therefore not be a prerequisite for the MVP. It remains a later optional provider if exercise fails to export or if stress, Active Score, Training Load, lower-latency data, or higher-resolution raw signals prove valuable enough to justify the maintenance cost.
