# Verified test preview 0.1.3 — published; phone validation still pending

Exact source `bafbe42a296cdd886750e89f55029882e88c5f0c` passed [run 44741557154](https://github.com/veldafelix7-oss/spaces-mobile/actions/runs/44741557154): compilation/lint, **79 JVM tests**, **43 Android instrumentation tests in two consecutive full suites**, fixed signing identity, and actual same-key update continuity from published 0.1.2/code 3 to candidate/code 4 and higher-version probe. The fixture retained seeded legacy history, profile, active session draft and journal through both replacements; no uninstall or data clear was performed in update mode. The successful manual preview workflow published [`preview-0.1.3-bafbe42`](https://github.com/veldafelix7-oss/spaces-mobile/releases/tag/preview-0.1.3-bafbe42), a **test preview**, not final. APK size 2,594,041 bytes; SHA-256 `6d2a92c98e72ac862bea145bb54d1e5e458920593e3e3892ad4e087511e22242`. Same-package/key update from 0.1.2; no reset/uninstall.

The compact report UI test now targets the tagged visible report body and verifies game attribution, realistic date, saved-heartbeat/state wording, process-versus-game caveat, font sizing, selectability and scrolling. Earlier source `24cfb82` had one selector-related UI assertion failure and was superseded; source `dc0462c` passed, after which its generated screenshot exposed epoch/blank timestamps in the *test fixture*. Current `bafbe42` seeds coherent values and passed the full suite. This does not imply a product clock defect or failure in the final candidate.

Emulator results do not certify Shizuku behavior on HiOS, PUBG/Blood Strike stability, memory use under gameplay, FPS/battery improvement, or resolution of the user's incident. No leak has been demonstrated. Do not install a CI draft APK; real-device testing and explicit publication remain separate.

---

# Verified results — 2026-11-18

## Disappearing-sidebar follow-up — automated gates passed, phone validation remains

The user observed the edge handle disappear while actively playing both PUBG and Blood Strike, and continued playing without reopening spaces. There is no phone notification/history/recovery evidence identifying whether the service auto-ended, crashed, was killed, or an attached overlay was hidden. Do not assign an OEM cause or claim the phone problem is conclusively fixed.

Code inspection found a real weakness: the old detector carried forward the last non-empty RESUMED package and could auto-end using stale other-app state. The follow-up defaults to manual end, makes automatic end explicit opt-in, and uses a bounded in-memory activity visibility ledger: game observed, all game activities explicitly STOPPED, another activity still visible, valid usage query, usable screen, no open overlay panel and both grace periods. PAUSED does not mean invisible. Query/permission uncertainty fails conservatively. Manual mode does not query other-app usage events. The public SDK exposes package/class, not per-instance IDs; repeated still-visible game-class resumes are treated as ambiguous and cannot prove exit. The first follow-up build `44447287360` caught an unavailable SDK method before any APK was produced; the adapter was corrected to public package/class data with this conservative fallback.

Local evidence includes last session event/time, sampling heartbeat, sidebar attachment status and, when available on reopen, Android's own-process historical exit reason filtered by recorded PID/time. No upload, invented kill diagnosis or attachment-equals-visibility claim. The user can request sidebar reattachment while the service is alive; automatic detached-view repair is bounded to one attempt. No process auto-restart or hide-overlay bypass. Delayed profile work checks destruction before applying and before creating the overlay; interrupted state/journal is preserved.

Verified run [44447692351](https://github.com/veldafelix7-oss/spaces-mobile/actions/runs/44447692351), source `8830907ee36f364f46d5a4377a50283b91776ae1`: compilation/lint, **64 JVM tests**, signed build, pinned identity, **31/31 + 31/31 Android tests**, and actual reset/same-key 3→4 private-data-retention gates all passed. Published test preview [`preview-0.1.2-8830907`](https://github.com/veldafelix7-oss/spaces-mobile/releases/tag/preview-0.1.2-8830907) is 2,567,697 bytes; GitHub asset digest SHA-256 `3fea59262819d93d8851acdd808072af29e5aea5f98e8cd6ebf7906bf3dae132`. Test-preview only, not whole-app completion or phone/game certification.

New verified coverage: 15 JVM exit-monitor cases and four service instrumentation cases: background/manual session with usage permission beyond old exit grace, unexpected service destruction preserving recovery data, explicit sidebar reattachment without ending, and cancellation of queued profile work preventing late apply/ghost overlay. These use an allowlisted fixture ID, **not installed/running games**. The Android suite wall-clock cap increases from 180 to 240 seconds for the real background soak; no tap retry/assertion is weakened. The two suites passed consecutively without retry. An initial successful addView does not yet prove ViewRoot attachment: source `8830907` waits for the attachment callback before reporting successful user reattachment. Raw artifact/log downloads from the sandbox were blocked by outbound host restrictions; the test counts and installation result were verified through GitHub job/check annotations, and APK digest/size through GitHub asset metadata.

## Fixed-key signing and actual reset/update — passed before follow-up

[44445695936](https://github.com/veldafelix7-oss/spaces-mobile/actions/runs/44445695936), source `901bee2`, passed:

- source compilation/lint and 49 JVM tests;
- signed APK/instrumentation build using the owner's configured private bundle, matching pinned certificate `f53383afe91aa4524dc2b58245bdf66dd4a0ce52ddbaffd96834de9ed3094287`;
- exact authorized **reset** mode (not a claim of same-key update from old 0.1.1);
- **27/27 + 27/27 Android tests**;
- actual emulator rejection of an old-key update, preserved old marker, approved emulator-only uninstall/clean install, then **same-key 3→4 update retaining private file/preferences and unchanged package list**. The higher-version probe is not delivered.

No publication job ran (manual dispatch), and no phone APK was delivered. The owner no longer needs to configure the secret again. [Signing policy and private-backup verification](UPDATE-IDENTITY.md).

## Signing setup history — reset approved, initial missing-secret guard

The user explicitly selected **start fresh without old app data** and **phone only/no PC**. This authorizes preparing one exact reset, not silent data deletion or arbitrary future key changes. No phone app/data has been changed.

- New RSA-3072 signing identity is pinned in `config/preview-identity.json` and applies to both debug and release under the **same existing `id.veldafelix.spaces.preview2` package**. A new private key was generated, not a recovery of the old key. The old 0.1.1 cannot be updated directly with it.
- The private signing bundle was uploaded to the user's private Drive; matching size/checksum and **owner-only** permissions were verified. A real local restore of that backup succeeded and matched the pinned certificate with mode-0600 output. No private credentials were committed, published or printed in chat.
- [44441128698](https://github.com/veldafelix7-oss/spaces-mobile/actions/runs/44441128698), source `e8cc12f`: source compilation, lint, **49 JVM tests**, and instrumentation Java compilation passed. Signing/reset/preservation/probe mocks and actual temporary test-key restoration cases passed on Java 17 in CI. **The signed-build job failed as intended because `SPACES_PREVIEW_SIGNING_BUNDLE` is not configured.** Android emulator, real reset/update, identity and publication jobs were skipped.
- Latest repeat [44441746761](https://github.com/veldafelix7-oss/spaces-mobile/actions/runs/44441746761), source `1d795b4`: source checks again passed **49 JVM tests**, lint and instrumentation Java compilation, including the strengthened restore/probe guards. Signed build again correctly stopped on the missing private secret; identity/emulator/install/publication were skipped, not passed.
- Additional local tests cover rejecting inconsistent PKCS12 key passwords and preserving an existing key rather than overwriting it. Neither local fixtures nor mocks prove actual signed Android installation.
- The subsequently successful installation gate (run above) proves old-key update rejection, then an approved clean reset on a GitHub CI emulator only, followed by a same-key **3→4** private-data-retaining update probe. At these initial missing-secret runs the path was pending; it later passed in `44445695936` above. Probe output is separate from the main publication APK and the main APK is restored even after a failed probe build.
- At this historical stage no new APK was delivered. Secret setup was later completed and validated as recorded above; it does not need repeating. The revised source still needs its own full checks before manual publication. [Setup and recovery policy](UPDATE-IDENTITY.md) · [phone instructions](PHONE-RESET.md).

The following sections record earlier evidence, before approval of the fixed-key reset route.

## Unreleased 0.1.2 — sidebar tests passed, UPDATE BLOCKED

- Runtime/test source `6d5e1e2`: [44437141451](https://github.com/veldafelix7-oss/spaces-mobile/actions/runs/44437141451) passed compilation/lint/**49 JVM tests** and **27/27 + 27/27 Android tests**.
- Source `8ae23c4` (same app/test code, signing-check changes only): independent [44438095141](https://github.com/veldafelix7-oss/spaces-mobile/actions/runs/44438095141) also passed compilation/lint/**49 JVM tests** and **27/27 + 27/27 Android tests**. Four complete suites passed, with no retry after failure.
- These overall runs are **not green**: their identity jobs failed; actual same-package upgrade checks and publication were skipped. Initial certificate-output parsing was too narrow; after support for the real `V2 Signer:` format, a real no-build audit [44439015416](https://github.com/veldafelix7-oss/spaces-mobile/actions/runs/44439015416) correctly failed on **different certificates**. [Exact public fingerprints and recovery requirements](UPDATE-IDENTITY.md).
- Version 0.1.2/code 3 keeps package `id.veldafelix.spaces.preview2`; **no new release/APK was delivered**. The existing public 0.1.1 remains unchanged. Do not describe the signing problem, data migration, or the entire requested app as finished.

### Top-tab correction

The old top origin used the full status/cutout/mandatory band, including hidden status bars. The new origin uses live status-bar visibility with display-wide inset dimensions. It takes the maximum of visible status bar, cutout, and mandatory band minus half the visible 12dp chevron plus one pixel. Thus the rim can be closer while the first glyph tap stays outside the mandatory band; no reserved top space means exactly flush. The origin adapts to actual inset changes, preserves the saved horizontal fraction, and avoids updates during an active handle gesture. Side docks and panel controls remain unchanged.

Using frame-relative inset sizes directly would cause position/inset feedback in a small overlay, so only visibility is taken from the live root; dimensions remain screen-relative. A no-change guard avoids redundant window updates.

Four new JVM tests cover flush fullscreen, nearer mandatory-band placement with a safe glyph center, status/cutout precedence and malformed inputs. Two added Android tests verify actual first top-chevron tap/close and live-inset repositioning without horizontal preference drift or repeated-inset movement. Existing actual handle tap, held top drag/downward swipe, all-dock cycles, volume/focus and Back tests remain. The initial [44436942435](https://github.com/veldafelix7-oss/spaces-mobile/actions/runs/44436942435) exposed a mistaken one-pixel expected value in a new test; it was corrected to require the integer glyph center strictly beyond the mandatory pixel, not by weakening the safety rule.

The closer placement has **not been installed on the user's Tecno** because a compatible update is unavailable. No phone visual/gesture certification is claimed. The upgrade script has local mock coverage for success and certificate/file/preferences/package failures, but its real successful emulator path is **not yet verified**. Signing mock tests cover standard/SDK-scoped/V2 output, mismatched certificates, package/version drift, absent digests, invalid signatures and download failure. Existing two-suite success/failure/timeout mocks still pass.

## Current 0.1.1 preview — repeated emulator validation passed

- Source: `82e5e54f65d60efbe06f171a458ed11049b7b3cd`.
- [44376931976](https://github.com/veldafelix7-oss/spaces-mobile/actions/runs/44376931976): compilation, lint, **45 JVM tests**, **25/25 + 25/25 Android tests**; draft prepared.
- Independent fresh-emulator [44378340735](https://github.com/veldafelix7-oss/spaces-mobile/actions/runs/44378340735): compilation, lint, **45 JVM tests**, **25/25 + 25/25 Android tests**. No retries after failure; all four complete suites passed.
- The gate has now actually run on the emulator, not just mocked. Publication remains a manual action after review; future CI runs create drafts only.
- APK: **2,552,137 bytes**; release `preview-0.1.1-82e5e54`. GitHub asset digest: `59a529d27c944ada6aacc59f5fd33e0a7a263709eb24c703b172f6fa08b58da3`. Asset digest/metadata were verified via GitHub API; no local download/reinstall is claimed.

### What the investigation established

The earlier isolated green run was correctly rejected after an independent failure. UI-thread-only state reads did **not** fix it ([44372715979](https://github.com/veldafelix7-oss/spaces-mobile/actions/runs/44372715979)). Explicit focus-readiness checks then exposed that a visible panel sometimes never acquired input focus ([44373827241](https://github.com/veldafelix7-oss/spaces-mobile/actions/runs/44373827241)).

Bounded `dumpsys input` and `dumpsys window windows` diagnostics in [44375894754](https://github.com/veldafelix7-oss/spaces-mobile/actions/runs/44375894754) showed:

```text
FocusedWindows: displayId=0, name='2adeec4 ImmersiveModeConfirmation'
spaces-panel: isReadyForDisplay()=true, mDrawState=HAS_DRAWN, isVisible=true
```

Android's first-use immersive fullscreen tutorial was above the correctly laid-out panel, held focus, and could consume a subsequent touch. This establishes the cause of the captured failure, not proof that every historical failure had the same cause.

The test setup now acknowledges the framework `android:id/ok` button in the Android-owned window **before creating the overlay**, then waits for stable activity focus. It does not change secure settings, suppress system windows, retry panel taps, call app `performClick()` in OS-input tests, or accept a second touch as success. The original first-open/first-collapse assertions remain; the later diagnostic second tap was removed. Controller state is read on its owning UI thread; the open panel must acquire input focus before the next OS interaction. Failure diagnostics are retained as bounded annotations, and teardown tolerates partial setup failure.

This is test-environment preparation for a real system dialog, not a runtime ability to bypass it. On a phone, acknowledge Android's fullscreen instruction when shown before testing the panel. The new preview is suitable for **device testing**, not a claim of universal touch reliability. Tecno/HiOS game focus, incoming-call Focus behavior, thermal/battery/FPS effects, and permanent production signing remain unverified/unresolved. The old failed `preview-0.1.1-b6bcdd7` stays draft and must not be confused with this preview.

## Earlier 0.1.1 candidate — withdrawn after repeat failure

**Do not read the successful single run below as stable validation.** The independent repeat [44367972091](https://github.com/veldafelix7-oss/spaces-mobile/actions/runs/44367972091), source `ffeb50c` (documentation-only changes after `b6bcdd7`), passed build/lint/45 JVM tests but **failed 1 of 25 Android tests**: `actualWindowTapsOpenAndCollapsePanel`. The first collapse tap produced no overlay event; a later diagnostic tap closed it. The test correctly retained the original failure. InputDispatcher warning log was empty. Six repeated glyph cycles, media and Back tests passed, so those alone are insufficient to certify the first-tap path.

The automatically published candidate was returned to **draft** after this result; no revised APK is being recommended to the user. At that point root cause remained unestablished; see the later captured system-window evidence above. Top-safe-band opening is passing, but gesture stability as a whole is not proven. Temporary focus, chevron alignment, frame-boundary updates, display stabilization and screenshot waits must not be described as a definitive collapse fix.

CI policy has now been tightened: two consecutive suites must pass (no retry after a failed suite), and a successful build only prepares a **draft**. Manual review is required before publication. The gate script was initially locally tested with mocked adb success/failure/timeout cases; subsequent actual two-pass results are recorded above.

### Historical single-run 0.1.1 evidence (superseded for release readiness)

- Tested source: `b6bcdd7e4272b015682953ee302ea78aac41900d`.
- [Successful run 44366688867](https://github.com/veldafelix7-oss/spaces-mobile/actions/runs/44366688867): build, emulator and preview publication all succeeded.
- Candidate assets (now draft, not a public download); `spaces-mobile-preview.apk`, **2,551,781 bytes**. SHA-256 is attached to the release.
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
