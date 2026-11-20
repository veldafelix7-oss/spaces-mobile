# Test plan — report evidence, not assumptions

## Stability repair 0.1.3 (candidate)

- Full exact-source gates must pass after the report UI selector correction; do not deliver the failed candidate. Source has 79 JVM cases and 43 Android cases.
- Two consecutive full suites remain mandatory without a failed-suite retry. Per-suite wall-clock bound is 300 seconds for the >60-second real-background/PSS fixture, plus the existing native first-touch/dock/focus assertions.
- New coverage: bounded/torn/failing sample journals, session exit attribution, legacy history migration, summary-only preferences, archive pruning, draft/profile/journal preservation, failed-save recovery, background Activity release, report readability/scrolling and on-demand charts.
- Actual install gate seeds old-schema history/profile/active-draft under published 0.1.2, updates to the candidate, runs the app storage assertions, updates to the higher-version probe, and rechecks them. Update mode must not uninstall or clear data.
- Shizuku actual binder/HiOS effects and PUBG load are NOT emulated by these fixtures. Main-thread rejection, permission handling and command policy do not certify real hardware performance.

## Automated (CI)
- JVM: command allowlist, injection rejection, bounds, package restrictions, mode parsing, launch/exit grace, compare-before-restore.
- Android lint and debug compilation; instrumentation APK compilation.
- Two consecutive complete instrumentation suites per emulator job, fail-closed with no retry after a failure. Only draft candidates are created automatically; publication requires manual review. All identity and replacement/data checks must pass as well.
- Restore only the privately backed-up key matching `config/preview-identity.json`. Missing/wrong secrets must block signing; no generated fallback. Debug/release retain the existing preview2 package and same key.
- For the exact user-approved reset version (0.1.2/code 3), prove old-key update rejection before a clean reset **on the CI emulator only**, then a same-key higher-version 3→4 probe with private file/preferences retention and unchanged package list. Any reset without exact policy approval or outside a CI emulator must fail. The actual path passed on source `901bee2` in run `44445695936`, then again for the published lifecycle revision `8830907` in `44447692351`. The reset exception has now been removed and code 3 is the fixed-key baseline; future versions must increase code and update in place. See [signing setup](UPDATE-IDENTITY.md).
- Published 0.1.2 test preview `8830907`: 64 JVM tests, lint/compilation, two consecutive 31-test Android suites, pinned identity and real reset/same-key data-retaining update all passed in `44447692351`. Top-glyph/inset-adaptation coverage and four real service lifecycle tests are included. Earlier candidates had 49 JVM/27 Android tests and identity failures; those historical results are not the release evidence.
- Overlay setup acknowledges Android's first-use fullscreen tutorial before creating the overlay. It then checks stable activity focus, and the open panel's input focus. Actual first open/collapse, held drag, all-dock repeated glyph cycles, media touch and Back remain OS-input assertions. No system settings are used to disable the tutorial and no missed app tap is retried.
- Verified source `82e5e54`: two independent emulator runs each passed 45 JVM tests and both 25-test Android suites. See [results](TEST-RESULTS.md) for exact runs and earlier failed candidates.
- Android 13 emulator: landscape empty collection, Engine/Device rendering without permissions, language switching, rejected unsupported app, profile persistence, real Android telemetry access, empty/corrupt recovery, real emulator DND apply/restore/external-change/revocation, and foreground session start/duplicate/end/history (without a real game). Screenshots and AndroidRuntime log saved as artifacts.

CI success certifies only those paths. It does NOT certify Shizuku, HiOS, thermal/FPS improvements, DND handling of WhatsApp calls or actual game sessions.

## Tecno BG7 checklist (manual, pending until performed)
For every row record pass/fail, actual result, app version and date. Never record a skipped case as pass.

### Installation and UI
- [ ] Fresh install: collection empty, no bundled/downloaded games.
- [ ] Immediately landscape, both rotations, cutout and navigation bar do not obscure controls.
- [ ] All three tabs scroll and remain readable; ID/EN works; no haptic feedback.
- [ ] Add only installed Flash/Blood Strike; reject other apps/missing games; remove/re-add persists.
- [ ] Force-close/reopen maintains profiles.
- [ ] 0.1.1 isolated preview installs alongside 0.1.0; old data stays present, new collection/permissions configured separately. End/recover old sessions first.
- [ ] After the one-time user-approved reset, same-key higher-version APK updates retain local settings; wrong key fails safely. Debug/release must not add different application IDs. Current 0.1.1 cannot directly update to the new key.
- [ ] Before reset/uninstall, all old sessions are ended and recovery is complete. Fresh data/no import is expected only after the approved reset; never erase an active recovery journal. [Phone checklist](PHONE-RESET.md).

### Permissions and session lifecycle
- [ ] Default manual end remains active while playing and after returning Home. End explicitly with **Akhiri & pulihkan** after playing; DND/profile restoration is not automatic just because the game is no longer foreground.
- [ ] Opt-in auto-end applies next session only. Permission/game-tool windows and split-screen must not be mistaken for game exit; real STOPPED+other-visible evidence and grace periods are required. Unsupported/OEM event gaps remain a limitation.
- [ ] If sidebar disappears, reopen only when convenient: record whether session is active, already ended or recovery is pending; inspect **Detail sesi terakhir / sidebar hilang**. Check notification state. Do not call a stopped heartbeat proof of an OEM kill.
- [ ] While service is alive, explicit **Tampilkan ulang sidebar** does not create a second session. Game/system suppression may still hide an attached overlay; no bypass is attempted.
- [ ] Review app-specific HiOS battery/background settings only on user request. Do not disable global protection or claim immunity to memory pressure/force-stop.
- [ ] Deny each optional permission: no crash, feature is accurately labelled unavailable.
- [ ] Start from this app only; launching game externally does not start service.
- [ ] Service notification stop works; app stop and overlay stop work.
- [ ] Without usage access, manual stop works.
- [ ] With usage access: launch grace, brief switch, >20-second switch, home, lock and game crash behave as documented.
- [ ] Notification denied: app stop remains available; explain FGS visibility in Android task manager.
- [ ] No duplicate service/session when tapping twice.

### Shizuku and recovery (test conservatively)
- [ ] No Shizuku installed, not running, permission denied, permission granted.
- [ ] Record refresh settings and GameManager mode before test.
- [ ] 60/90 option appears only if reported by display modes.
- [ ] Apply one option; inspect engine log and readback. Do not confuse readback with FPS gain.
- [ ] Normal end restores previous values, including previously absent keys.
- [ ] Change setting externally while active; recovery preserves different external value.
- [ ] Disconnect Shizuku during session; recovery remains pending, reconnect and retry.
- [ ] Revoke DND/overlay/usage permissions mid-session: no crash and recovery actionable.
- [ ] Kill app/reboot while active: reopen, explicit recovery, interrupted session labelled, no invented duration/samples.
- [ ] No game file/config/memory mutations; no thermal changes.

### Focus and overlay
- [ ] If Android shows its first-use fullscreen tutorial, acknowledge it. Then check first open/close touches; do not count a consumed tutorial-dismissal touch as an app interaction. Also test clean-install onboarding explicitly.
- [ ] Calls/messages silent, alarms permitted, original DND returns. Check normal calls and WA separately.
- [ ] WA continues receiving messages; there is no WA shortcut/recorder.
- [ ] Tap/swipe open, drag handle left/right/top, reopen panel after rotation, close restores touch access to game.
- [ ] Top tab approaches the physical edge when system bars are hidden; visible status/cutout protection remains, and its glyph center stays outside mandatory gestures. Check transient status bars and both landscape rotations; notification swipe must still work. Test first/repeated chevron-center taps at all three docks. Use published 0.1.2 test preview only after the approved reset and old-session recovery checklist.
- [ ] Open panel takes input focus only while open. Game input/focus/audio resume after arrow or Back; check each game does not unexpectedly pause/reload.
- [ ] Metrics visible only inside panel. Battery temp agrees with public battery data; RAM is system available RAM.
- [ ] FPS explicitly unavailable; no simulated values.
- [ ] TCP test labelled not game ping, two-second timeout, no periodic network polling.
- [ ] Volume control works; brightness link opens system settings.

### History
- [ ] End creates one entry with duration and real samples; too-short sessions show insufficient-data label.
- [ ] Temperature chart uses actual timestamps; gaps not invented.
- [ ] Interrupted sessions marked; recovery logs accessible; delete history does not erase recovery journal.
- [ ] 30-session cap; rolling sample cap; no accounts/tracking traffic.

## Performance protocol (phone only)

Initial device: BG7, Helio G36, 4 GB physical + 4 GB FusiMem, Android 13 / HiOS 13.0.0. Storage 71.29/128 GB used at briefing. These are baseline facts, not benchmark results.

Games observed: PUBGM FLASH 4.7.0 (Exclusive FLASH Beta, Medium frame rate, AA off, adaptive graphics off); Blood Strike 1.003.650026 (Ultra FPS, Smooth resolution, Very Low graphics). FOV first-person 120. Third-person screenshots differ (84 / 71); record current value before test.

1. Use same game mode/map, brightness, Wi-Fi, fan, battery range and similar room/start battery temperature. Do not charge while testing. Finish asset downloads first.
2. Run 15–20 min without this app; record stutter observations, battery change, temperature, and only genuine FPS data if the game provides it.
3. Repeat using app Balanced/System, sidebar closed. Compare app overhead before enabling any intervention.
4. Change ONE option at a time (GameManager preference OR display refresh). Repeat at least twice; let phone cool between sessions.
5. Compare stability, heat and battery. A higher requested display refresh is not higher game FPS. No improvement → revert.
6. Test FusiMem +2/+1/off separately in HiOS with reboot; do not combine with other changes. 4 GB physical remains unchanged. Record reloads as well as stutter.
7. Optional in-game settings comparisons (Flash Beta vs Super Smooth; Blood Strike FOV) must be manual and separate. Never alter game files.
8. Stop if unusually hot, unstable, charging hardware problematic or battery warning appears. Do not bypass thermal throttling.

## Release gate
No final performance claims or production-ready label until hardware checklist and repeated comparisons are completed. Retain failing-case logs; known unavailable features stay disabled. Private release signing identity must be backed up and kept out of Git.
