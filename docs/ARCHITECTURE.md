# Architecture and safety decisions

- `MainActivity`: native UI; stopped activities release their content/nav/icon cache and report dialogs, rebuilding on resume. History cards retain metadata, and a single requested graph is loaded off-thread. No continuous background UI animation, real-time blur, or external artwork.
- `Store`: private preferences, explicit game selection, profiles, local history, durable recovery state. Backup disabled to avoid restoring a journal to another device.
- `SessionService`: user-started special-use foreground service. START_NOT_STICKY: no silent restart or auto-optimization when a game is opened elsewhere. Handler requests a sample roughly every 10 seconds after the preceding worker write completes, with at most one persistence tick in flight. `SampleJournal` appends synced private records; compaction caps disk growth, and recovery reads the latest 1080 points. Preferences retain small history summaries and point arrays live in atomic per-session files (30 saved sessions). Older preference-backed histories migrate once off-thread before gameplay profile work; failure preserves the original payload. Does not hold wake/Wi-Fi locks.
- `Engine`: single serial executor; synchronous committed write-ahead journal before OS mutation. Restore checks current vs applied value, preserving other actors' changes. Missing settings restored by deletion. Permission/connection failures retain entries. Settings readback is clearly distinguished from an effective rendering/frequency change. Manual recovery required after crash/reboot.
- `PrivilegeBridge`: binds a non-daemon Shizuku UserService; permission checked, timeout applied off UI thread. No root calls or deprecated remote-process API.
- `ShellService` + `ShellPolicy`: typed AIDL; only fixed settings keys and two fixed game IDs accepted. ProcessBuilder argument arrays, no shell interpolation. Bounded output, five-second execution timeout. Command policy has JVM tests. No `force-stop`, `pm disable`, file writes, thermal edits or arbitrary shell interface.
- `Telemetry`: public Android battery broadcast + ActivityManager; optional UsageEvents. No fake FPS sensor. Manual end is the default and performs no other-app event queries. Opt-in `SessionExitMonitor` requires explicit STOPPED evidence, another still-visible activity, usable screen and grace windows; ambiguous/query/permission conditions fail conservatively. Own-process PSS is sampled no more than once a minute for local diagnostics, not FPS or leak proof.
- `OverlayController`: application overlay, optional permission. Only handle draws when closed. Panel is scrollable, fixed to screen bounds; no game input injection. Volume changes are user actions (not restored as profile changes). Brightness opens system settings; no WRITE_SETTINGS permission.

## Known limits / failure states

1. Force-stopping the app/OS process kill prevents code from executing: settings can persist until manual recovery. No guarantee of restoration on shutdown. Persistent journal blocks a new session until reviewed.
2. Shizuku disconnection leaves shell changes pending; focus can still be restored if DND access remains.
3. Display settings can be ignored/overridden by HiOS. GameManager may be absent, not expose supported modes, or be ignored by games.
4. Compare-and-restore cannot distinguish an external actor setting the exact same value. In ambiguous cases no absolute guarantees are made.
5. Usage-based exit is approximate and optional. Opening another app/locking the phone for long enough may end a session. Without usage access use manual end.
6. No real FPS or native engine frame-time source has been validated on the BG7; graphs report battery temperature only.
7. The saved exclusion list does not kill/protect processes: no background killer exists yet.
8. Requested shell settings use the current shell/default Android user; secondary Android user/work-profile optimization is not supported.
9. The app is not an OEM privileged Game Space and cannot replace OEM driver/touch/charging integration.
