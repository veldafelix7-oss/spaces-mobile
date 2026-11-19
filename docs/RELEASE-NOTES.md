# spaces : mobile — 0.1.2 candidate (not the final app; one-time reset required)

**Hold until this source passes all checks.** The user approved starting fresh without old app data. The fixed key is privately backed up; the installed package stays `id.veldafelix.spaces.preview2` for debug/release. Secret configuration and the first real reset/3→4 update validation succeeded in [44445695936](https://github.com/veldafelix7-oss/spaces-mobile/actions/runs/44445695936). The disappearing-sidebar follow-up below needs its own tests. [Evidence](TEST-RESULTS.md) · [phone reset steps](PHONE-RESET.md).

- Top tab moves to the nearest safe visual origin instead of reserving hidden status-bar height. Visible status/cutout protection and safe glyph-center input remain.
- **Safer session ending:** manual by default, even with usage permission. After playing, press **Akhiri & pulihkan** in the sidebar, notification or app. Automatic end is a separate opt-in for the next session; it requires having observed the game, explicit game activity STOPPED, another activity still visible, an unlocked/interactive screen, no open control panel, and the existing grace periods. Transient RESUMED/PAUSED activity alone, failed queries and permission loss do not end a session. Repeated same-class game resumes are ambiguous under the public SDK and fall back to manual ending. No other-app activity list is saved.
- **Disappearing-sidebar investigation:** the old code could keep a stale other-app RESUMED package and auto-end a session. That is a code weakness, **not proof of the cause on the user's phone**. A stopped process or game/system overlay suppression can still remove/hide the sidebar; no anti-kill/HiOS survival guarantee.
- Add local last-event/heartbeat/attachment evidence and Android's own-process exit report when available, under **Perangkat → Detail sesi terakhir / sidebar hilang**. Attachment is not proof of visibility; a missing heartbeat alone is not a kill diagnosis. No automatic diagnostic upload.
- Add explicit **Tampilkan ulang sidebar** for a still-running session, plus one bounded repair of a genuinely detached view. No game/system hide-overlay bypass, process restart loop, wakelock or persistent network activity. Battery/background settings open only on user request and are not changed automatically.
- Guard delayed profile initialization so stopping a service cannot later apply a queued profile or create a ghost overlay. Unexpected stop preserves the active flag, draft samples and recovery journal. No sticky auto-restart; reopen and recover when necessary.
- **Installation:** version 0.1.2/code 3 uses a new key, so it is **not** a direct update of old-key 0.1.1. Only after the tested APK is available, end/recover old sessions before the user-approved uninstall/reinstall. Data starts empty; no automatic migration. No third package.
- The approved reset exception applies only to this exact version. Future builds must retain the fixed key and increase versionCode. The CI-only higher-version probe is not a user download. Manual publication only; no feature-completeness, real-game/HiOS, FPS or battery-gain claim.


---

## Previous published 0.1.1 notes (historical)


Source `82e5e54`: build/lint/45 JVM tests and **four 25/25 Android suites**, two consecutive suites on each of two independent emulator runs. [First run](https://github.com/veldafelix7-oss/spaces-mobile/actions/runs/44376931976) · [Independent repeat](https://github.com/veldafelix7-oss/spaces-mobile/actions/runs/44378340735).

Earlier candidates were withheld after first-touch failures. Window-manager diagnostics captured Android's fullscreen onboarding dialog taking focus above the visible panel. Test setup now acknowledges that system dialog before overlay creation; no panel tap is retried or bypassed. If the phone shows Android's fullscreen tutorial, acknowledge it first. This does not let the app bypass system dialogs. HiOS/game touch/focus and hardware performance still need device testing. [Full evidence, including failures](TEST-RESULTS.md).

Follow-up to real Tecno Spark 20C feedback:
- Thin edge-docked sidebar tab with chevron. Hold and drag to snap left/right/top; top tab becomes horizontal. Edge and normalized position survive rotation/restart; legacy positions migrate.
- The top tab sits just below Android's reserved status/notification/cutout band. It stays horizontal; the app does not override mandatory system gestures.
- One overlay input window is reused for both states. The open panel takes temporary input focus (not DND/Focus mode), supports Back, and returns focus when closed; game-specific behavior still needs phone testing.
- Compact session panel with a fixed collapse arrow, outside the scrolling controls; no large Close button. The closed handle is hidden while the panel is open.
- Entire navigation tiles are touch targets; switching tabs retains the rail instead of rebuilding the full screen.
- More visible static glass highlights/rims and subtle neutral background contrast. No live blur added.
- Original installed-game icons shown on game cards; no bundled or downloaded game artwork.
- Neutral concise Indonesian copy, explicit Shizuku connection/permission label instead of ambiguous Standard mode.
- Edge-to-edge app backdrop with cutout-safe navigation controls; the app does not change the games' own camera/cutout policies.
- **Installation change:** 0.1.0's preview signing identity could not be recovered from CI. This build uses a separate preview application ID (`id.veldafelix.spaces.preview2`) and launcher label `spaces : mobile · 0.1.1`. It installs alongside the old preview, not over it. Old local data is not deleted or automatically migrated. End/recover all old sessions before starting one in the new app. Collection and permissions must be configured again in the new app.
- Same-key in-place production updates remain blocked until private signing secrets are configured. CI verifies both APK signatures and the isolated preview ID; emulator checks old private data survives side-by-side installation.

Unchanged safety limits: no FPS-unlock, verified FPS capture, render downscale, automatic process stopping, global EQ or touch-driver control. No game-file/memory modification or thermal bypass. HiOS gestures and hardware performance still require real-device testing. Slightly smoother play reported by the user is subjective, not a measured FPS gain.
