# 0.1.3 stability repair — evidence and validation

## Incident, not an invented diagnosis
User clarified that PUBG continued running while the spaces sidebar AND notification disappeared. Android's own main-process exit report says **low memory, 11:45:23** after the last saved heartbeat at 11:45:16. The session started at 11:41:36, matching the PUBG history. Battery exemption was already enabled. This is evidence of a low-memory spaces process exit, not evidence of PUBG crashing, a spaces heap leak, or which process consumed the most memory. The user reported Blood Strike worked in that trial.

## Code-level workload found and repaired
- Old ticks serialized the entire growing JSONArray into SharedPreferences every ten seconds, rewriting preferences that also contained all history sample graphs.
- Device rendered every history graph at once, and the stopped Activity retained that view tree and icon cache during gameplay.
- These are provable allocation/retention sources in code, **not measurements of their responsibility for the phone kill**.
- New ticks append a single bounded record to a private synced disk journal on the serial worker. No accumulated session JSONArray is held by the service; no whole draft JSON is rewritten on each tick. At most one persistence tick is queued; next tick follows its completion. Latest 1080 records are read only for archive/recovery, and compaction bounds disk growth to fewer than 2160 complete records between writes. Torn final records are ignored/repaired, never invented.
- History preferences keep summaries; each session's samples are atomically saved separately. Existing 0.1.2 samples migrate before the old preference payload is replaced. Migration runs off the UI thread and can temporarily materialize the old payload once; it is not a zero-allocation operation. New-session persistence no longer reserializes all earlier graphs. Failed migration/save preserves original data and active draft for retry. Repeated save with the same session ID is idempotent. Max 30 sessions remains; deleting history does not delete an active draft/settings journal.
- History charts remain available on request in **Grafik & laporan sesi**; only one graph dialog is retained. Activity releases content/nav/icon caches and report dialogs on stop, rebuilding on resume; service/overlay is NOT stopped by this UI cleanup. No forced GC, process killing, thermal edits or keep-alive loop.
- Small local own-process PSS samples are recorded at initialization and no more than once per minute (first/last/sampled peak). They are estimates, not total system RAM, continuous peak capture, FPS measurements or leak proof.

## Reports and existing Shizuku path
- Scrollable/selectable report uses compact text and sections. Game, session ID, start, last event, saved sample, historical sidebar status/time, Android reason and periodic spaces memory are separated from CURRENT service/Shizuku/battery state. Historical cards use their own captured evidence, not the next game's report.
- Android exit matching rejects finished sessions, wrong PID/process name, older exits and exits beyond the closed interruption window. Older previews may not have detailed evidence; no historical cause is fabricated.
- Engine/recovery messages are translated into plain Indonesian/English, distinguish readback from performance, and explain skipped/unverified/pending/external-change outcomes.
- Shizuku permission vs actually bound UserService is distinguished. Timeout/death invalidates late callbacks, clears old bindings, and execution refuses the main thread. No new tuning commands. Actual Shizuku/OEM support and performance remain a phone-only validation item.

## Status
0.1.3/code 4 is a **candidate** using the existing .preview2 package and pinned stable key; 0.1.2/code 3 is the published baseline. No uninstall/reset is authorized or required for this update. Compile/lint/JVM/Android/actual same-key replacement checks for this source are pending. Python fail-closed gate mocks passed locally; they are not Android/runtime proof. No local Android SDK is installed.

Scope excludes downscale/new booster controls, FPS capture, cleaner/freezer, recording and WhatsApp shortcut. User explicitly rejected recording/WA shortcut; never list them as unfinished requests.
