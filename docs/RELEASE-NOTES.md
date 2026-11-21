# spaces : mobile — 0.1.4 same-key update candidate (not final)

VersionCode 5 is the next in-place update after the published 0.1.3/code 4 preview. It keeps the same `id.veldafelix.spaces.preview2` package and pinned signing key. The source/signing/emulator/update checks for this exact candidate must pass before offering its APK. No uninstall, data clear, or reset is part of this update.

This candidate adds automatic Shizuku UserService binding when Spaces resumes or Shizuku returns, clearer live/connecting/failure states, a single bounded reconnect after a service disconnect, and visible failure details in Device. Session charts show local-time labels and exact selected sample times/values; missing intervals remain gaps. Neither feature is yet validated on the TECNO/HiOS device. No FPS, memory-leak, or performance gain is claimed.

**Manual TECNO/HiOS, Shizuku and gameplay validation remain pending.** Do not install until the matching same-key test preview link is published after all automated gates succeed. End and recover any active session before installing; install over the existing app and preserve its data.

---

## Previous test preview: 0.1.3 (historical)


**Automated checks passed** for exact source `bafbe42a296cdd886750e89f55029882e88c5f0c`: [run 44741557154](https://github.com/veldafelix7-oss/spaces-mobile/actions/runs/44741557154) — build/lint, 79 JVM tests, 43 Android instrumentation tests in two consecutive suites, pinned signing identity, and same-key update/data-retention verification. **Manual TECNO/HiOS, Shizuku and gameplay validation remain pending.** This is a test preview, not a final app or an FPS/stability guarantee.

[Download 0.1.3 Android test preview](https://github.com/veldafelix7-oss/spaces-mobile/releases/download/preview-0.1.3-bafbe42/spaces-mobile-preview.apk) · [SHA-256](https://github.com/veldafelix7-oss/spaces-mobile/releases/download/preview-0.1.3-bafbe42/SHA256SUMS.txt). APK: 2,594,041 bytes; SHA-256 `6d2a92c98e72ac862bea145bb54d1e5e458920593e3e3892ad4e087511e22242`.

**Direct in-place update from published 0.1.2/code 3:** same `id.veldafelix.spaces.preview2` package and pinned signing key; versionCode 4. End and recover any active session first. Download, then install over 0.1.2; **do not uninstall, clear data or reset**. CI seeded old-format history, profile, active draft and journal, and verified those persisted across the candidate and a higher-version probe. The probe itself is CI-only and must not be installed. If still using old-key 0.1.1, that version is not directly updatable; follow the one-time migration/reset instructions only if applicable and only after recovering its sessions.

Repair-only changes. No downscale, FPS meter, cleaner/freezer, EQ, extra tuning, screen recording or WhatsApp shortcut.

- Reduce avoidable background allocation/work: bounded disk-backed sample journal, serial worker/backpressure and private atomic session archives instead of rewriting growing draft/history preferences each tick. Keep current and legacy session data recoverable.
- Release stopped Activity UI/icon retention. Load only the selected history graph.
- Improve selectable/scrollable Indonesian/English reports: game/session identity and times, saved evidence versus current status, historical sidebar evidence, own-process Android exit reason, and sampled PSS with explicit limitations. Clarify session end mode and saved heartbeat.
- Harden existing Shizuku binding invalidation/timeout handling; no additional privileged commands or tuning.

The incident evidence says Android reported a low-memory **spaces-process** exit while PUBG continued running. It does not identify the largest RAM consumer, establish a spaces leak, prove PUBG was responsible for memory pressure, or certify the repair on HiOS. No FPS/battery improvement is claimed. [Evidence and scope](MEMORY-STABILITY.md) · [manual phone validation checklist](TESTING.md).

---



## Previous 0.1.2 test preview (historical)

# spaces : mobile — 0.1.2 test preview (not the final app; one-time reset required)

**Automated gates passed for source `8830907`.** [Run 44447692351](https://github.com/veldafelix7-oss/spaces-mobile/actions/runs/44447692351): build/lint/**64 JVM tests**, **31/31 + 31/31 Android tests**, pinned signing identity, and actual approved reset followed by same-key 3→4 update retaining private file/preferences with no extra package. This is a **test preview**, not proof that the disappearing-sidebar incident on HiOS has been reproduced or conclusively resolved, and not completion of every requested app feature.

The user approved starting fresh without old app data. The key is privately backed up; the installed package stays `id.veldafelix.spaces.preview2` for debug/release. **Download first; finish/recover old sessions before any uninstall.** Old-key 0.1.1 cannot be updated directly. [Evidence](TEST-RESULTS.md) · [phone reset steps](PHONE-RESET.md).

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
