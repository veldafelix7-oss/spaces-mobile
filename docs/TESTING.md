# Test plan — report evidence, not assumptions

## Automated (CI)
- JVM: command allowlist, injection rejection, bounds, package restrictions, mode parsing, launch/exit grace, compare-before-restore.
- Android lint and debug compilation; instrumentation APK compilation.
- Two consecutive complete instrumentation suites per emulator job, fail-closed with no retry after a failure. Only draft candidates are created automatically; publication requires manual review.
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
- [ ] Same-key higher-version APK updates without losing local settings. Wrong key fails safely. Preview and release separate.

### Permissions and session lifecycle
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
- [ ] Top tab starts below the mandatory system band; notification swipe remains available above it. Test repeated chevron-center/glyph taps at all three docks.
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
