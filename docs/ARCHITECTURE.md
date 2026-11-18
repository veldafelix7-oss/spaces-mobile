# Architecture and safety decisions

- `MainActivity`: native UI; nothing renders behind a game after Activity pause. No continuous background UI animation, real-time blur, or external artwork.
- `Store`: private preferences, explicit game selection, profiles, local history, durable recovery state. Backup disabled to avoid restoring a journal to another device.
- `SessionService`: user-started special-use foreground service. START_NOT_STICKY: no silent restart or auto-optimization when a game is opened elsewhere. Handler samples every 10 seconds. At most 1080 telemetry samples and 30 saved sessions. Does not hold wake/Wi-Fi locks.
- `Engine`: single serial executor; synchronous committed write-ahead journal before OS mutation. Restore checks current vs applied value, preserving other actors' changes. Missing settings restored by deletion. Permission/connection failures retain entries. Settings readback is clearly distinguished from an effective rendering/frequency change. Manual recovery required after crash/reboot.
- `PrivilegeBridge`: binds a non-daemon Shizuku UserService; permission checked, timeout applied off UI thread. No root calls or deprecated remote-process API.
- `ShellService` + `ShellPolicy`: typed AIDL; only fixed settings keys and two fixed game IDs accepted. ProcessBuilder argument arrays, no shell interpolation. Bounded output, five-second execution timeout. Command policy has JVM tests. No `force-stop`, `pm disable`, file writes, thermal edits or arbitrary shell interface.
- `Telemetry`: public Android battery broadcast + ActivityManager; optional UsageEvents. No fake FPS sensor. Latest foreground event is best-effort, not a guaranteed game lifecycle signal. Grace windows avoid immediate session end when launching or switching briefly; manual stop remains available.
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
