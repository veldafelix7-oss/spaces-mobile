# Verified results — 2026-11-18

## Released 0.1.1 evidence

- Tested source: `b6bcdd7e4272b015682953ee302ea78aac41900d`.
- [Successful run 44366688867](https://github.com/veldafelix7-oss/spaces-mobile/actions/runs/44366688867): build, emulator and preview publication all succeeded.
- [Published pre-release](https://github.com/veldafelix7-oss/spaces-mobile/releases/tag/preview-0.1.1-b6bcdd7); `spaces-mobile-preview.apk`, **2,551,781 bytes**. SHA-256 is attached to the release.
- APK/test APK compilation, Android lint and **45 JVM tests passed, with 0 failures/errors/skips**. Edge geometry now includes reserved top-band/clamping tests.
- **25 Android 13 instrumentation tests passed**, including the prior suite plus:
  - Actual right-handle tap and collapse-control center tap, without retrying a missed first tap.
  - Held OS-input drag to top, then inward swipe from below the mandatory notification/cutout band.
  - Right inward swipe opening.
  - **Six open/close cycles**, twice on each dock (right/left/top), tapping the visible chevron and checking focus returns to the activity.
  - Actual media-slider touch changes Android stream volume; original volume restored in `finally`.
  - OS Back closes the interactive panel and returns focus.
  - Physical navigation padding taps preserve the same rail; settings/data tests remain separate from actual OS-input assertions.
- Closed/open states now share one overlay input window. The open panel temporarily takes input focus and excludes the keyboard; collapse restores nonfocusable dock params. This is separate from the DND/Focus setting and must still be checked against game-specific window-focus behavior on HiOS.
- The narrow collapse View now matches the painted chevron's 12dp width/thickness; the parent also accepts a wider edge strip. Earlier tests exposed a mismatch between the padded center and the visible glyph. The final suite tests both the control center and visible glyph. No OS-input test is bypassed by calling `performClick()` or accepting a second tap.
- Diagnostics retain the first-tap assertion even if a subsequent diagnostic tap succeeds. Earlier failures (20/22, then 21/22 and 23/24) remain in Actions history; one isolated passing tap was not accepted as final verification.
- Test display orientation is stabilized before surface creation. UI-thread waits are bounded; actual events go through UiAutomation, with terminal-event acknowledgement. Failure thumbnails can be retrieved through bounded GitHub annotations when artifact redirects cannot be downloaded.
- Separate `.preview2` installation preserves the old `.preview` private marker in CI. This is not migration or same-key update verification. Permanent production signing remains unconfigured.
- A reduced emulator screenshot from the revision was visually inspected for basic home/panel layout via annotation retrieval. This is not inspection of every screen, real installed-game artwork or BG7 display behavior.
- Local XML parsing, Python syntax and Git whitespace checks passed. No local Android SDK is present: Android builds/tests ran in Actions, not locally.
- **Not verified on the revised APK:** BG7 game gestures/window focus, HiOS permission/lifecycle behavior, real incoming-call Focus behavior, game FPS, temperature/battery gains or zero overhead. Existing 0.1.0 phone feedback does not certify this revision.

## Released 0.1.0 evidence
- Tested source: `b7891f807f59c1ee1b0948c1f43b31c93b99e58b`.
- [Successful GitHub Actions run](https://github.com/veldafelix7-oss/spaces-mobile/actions/runs/44325479133).
- [Published pre-release APK](https://github.com/veldafelix7-oss/spaces-mobile/releases/tag/preview-0.1.0-b7891f8).
- APK asset: `spaces-mobile-preview.apk`, 2,528,225 bytes. SHA-256 checksum attached to the release.
- Source built with JDK 17, Android SDK 35; app minimum Android 13, target Android 14. Emulator: Android 13, x86_64, Pixel 2 profile, not Tecno hardware.

## Passed
- APK compilation, instrumentation APK compilation, Android lint (no errors; warnings are not represented as zero).
- **30 JVM tests: 0 failures, 0 errors, 0 skipped.** Checks cover typed shell allowlists/injection rejection, refresh bounds, supported packages, mode parsing, session grace windows, and restore comparisons including decimal refresh values.
- **13 Android instrumentation tests:**
  1. Starts landscape with empty collection.
  2. Engine/Device screens render without special permissions.
  3. Indonesian → English switching.
  4. Unsupported app rejected by catalog store.
  5. Profile values persist.
  6. Public Android memory/battery telemetry can be sampled.
  7. Corrupt journal blocks mutation and is retained.
  8. Two supported card layouts render (isolated test fixture, not real game installation).
  9. Empty recovery is safe.
  10. Focus journal applies DND, survives Engine recreation, and restores original state.
  11. Recovery preserves a completed external DND change.
  12. Revoked DND permission retains journal; regrant enables recovery.
  13. Foreground session start, duplicate-start rejection, stop and single history entry (without real games).
- Local XML parsing and Git whitespace checks.

Four screenshots, instrumentation output and AndroidRuntime log are in the `emulator-results` artifact. Screenshots were generated by the tests but not independently visually reviewed here: the sandbox cannot download the external artifact storage host. APK publication was verified via GitHub API (asset uploaded, release marked prerelease), not by locally reinstalling the APK.

## Defects caught and corrected before passing run
- Duplicate Java getter/setter signature: renamed setter to `setProfile`.
- Android API/style compatibility: target minimum explicitly Android 13, as scoped.
- Broadcast receiver export flag: now explicitly not exported.
- Asynchronous DND transition: bounded readback and test synchronization; external-change test now waits for the external change before testing recovery.
- Restore comparison handles numeric refresh formatting (60 vs 60.0) without losing recovery intent.
- Unreadable journal no longer silently becomes an empty journal.

## NOT VERIFIED / NOT CLAIMED
- Installation and operation on actual Tecno BG7 / HiOS 13.
- Shizuku pairing, binder reconnection and privileged GameManager/refresh commands on HiOS.
- Overlay gestures, touch interference, rotation and volume/brightness behavior inside the real games.
- Real game detection/launch/exit across OEM lifecycle behavior and game variants.
- WhatsApp incoming-call handling under DND, real calls, OEM permission revocation and reboot scenarios.
- Manual APK update compatibility after preview signing-cache eviction; production signing identity is not configured.
- Actual GPU/CPU consumption, FPS, stutter, battery-life improvement or thermal improvement.
- No FPS/downscale/automatic background cleaner is shipped. Exclusions are stored but no apps are force-stopped.

The manual device checklist in TESTING.md remains pending. This is an installable tested **preview**, not a completed hardware performance certification.
