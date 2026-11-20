# spaces : mobile

Native Android game space for **Tecno Spark 20C (BG7), Android 13 / HiOS 13**, designed around 4 GB physical RAM. Landscape, restrained light glass, almost invisible blue tint. No WebView, Flutter engine, ads, account or analytics.

## Download and verified checks

**0.1.3 repair candidate passed CI; not published.** Repair-only memory/session/sidebar and clearer reports—no downscale/new booster features. Exact source `bafbe42` passed build/lint, 79 JVM tests, two consecutive 43-test Android suites, pinned-key identity and actual same-key data-preserving update in [44741557154](https://github.com/veldafelix7-oss/spaces-mobile/actions/runs/44741557154). This does not certify HiOS or gameplay and does not show a proven leak/FPS improvement. Published download below remains 0.1.2; no 0.1.3 APK is handed off yet. [Evidence and scope](docs/MEMORY-STABILITY.md).

**0.1.2 test preview is available — not the final app.** Source `8830907` passed build/lint/**64 JVM tests**, **31/31 + 31/31 Android tests**, pinned signing identity, and actual approved reset followed by same-key higher-version private-data retention: [44447692351](https://github.com/veldafelix7-oss/spaces-mobile/actions/runs/44447692351). This does not certify HiOS/PUBG/Blood Strike survival or all requested features.

[Download 0.1.2 Android 13 test preview (~2.6 MB)](https://github.com/veldafelix7-oss/spaces-mobile/releases/download/preview-0.1.2-8830907/spaces-mobile-preview.apk) · [Release + checksum](https://github.com/veldafelix7-oss/spaces-mobile/releases/tag/preview-0.1.2-8830907)

**One-time reset required from old-key 0.1.1, not a direct update.** Download first, end/recover sessions in each old app, then follow the explicitly approved [phone reset checklist](docs/PHONE-RESET.md). Collections/profiles/history start empty. No migration or third package. Sessions now default to **manual end**; after playing use **Akhiri & pulihkan**. Optional automatic detection is off by default. Local sidebar/session evidence and an explicit reattach action help investigate recurrence; no anti-kill or hidden-overlay bypass claim. [Evidence](docs/TEST-RESULTS.md) · [fixed signing policy](docs/UPDATE-IDENTITY.md).

### Previous preview: 0.1.1 (historical)

Source `82e5e54` passed build/lint/**45 JVM tests** and **four complete 25-test Android suites**, two consecutive suites in each of two independent emulator runs: [44376931976](https://github.com/veldafelix7-oss/spaces-mobile/actions/runs/44376931976) · [44378340735](https://github.com/veldafelix7-oss/spaces-mobile/actions/runs/44378340735). Window-manager diagnostics identified Android's first-use fullscreen tutorial taking input focus; setup now acknowledges that system dialog before creating the overlay, without retrying any panel tap. If Android shows the fullscreen tutorial on your phone, acknowledge it first. This is **not** a claim that the app can receive input through a system modal. The previous failed candidate remains withdrawn. See [evidence and earlier failures](docs/TEST-RESULTS.md). HiOS/game compatibility and performance gains remain unverified.

### Important: separate installation, not an in-place update

0.1.1 uses isolated ID `id.veldafelix.spaces.preview2` and label `spaces : mobile · 0.1.1` because the original preview signing key could not be recovered. **Do not uninstall the old app or clear its data.** Old app/data stay separate; collection and permissions must be configured again. End/recover old sessions before using the new install. This is **not migration or a permanent signing solution**. [Original 0.1.0 preview](https://github.com/veldafelix7-oss/spaces-mobile/releases/tag/preview-0.1.0-b7891f8) remains available.

## Scope: 0.1.2 device-testing preview, not a proven performance booster

The app is functional groundwork with honest capability boundaries. A command's successful readback is **not** proof of a higher FPS. Testing on the actual BG7 is necessary; emulator results cannot certify HiOS or game compatibility.

### Implemented
- Initially empty collection; opt in to installed `com.tencent.igfit` (PUBGM FLASH) and `com.netease.newspike` (Blood Strike). Other apps cannot be added; games are not bundled/downloaded.
- Indonesian/English UI, landscape in both directions, original lightweight vector game scenes and lion emblem, plus real icons loaded locally from installed games.
- Per-game Eco/Balanced/Performance preferences (Android GameManager) and supported 60/90 Hz display settings, optional through Shizuku. Balanced/System defaults do not force performance settings.
- Optional Focus mode using Android DND (calls/messages silenced, alarms retained; actual OEM behavior must be verified).
- Foreground session started only from this app, ongoing stop action, optional usage-based session end (30-second launch grace, 20-second absence grace; sampled every 10 seconds).
- Thin edge-docked tab; quick inward swipe/tap opens metrics; hold and drag snaps left/right/top. The released 0.1.1 top tab reserves the full system band. Unreleased 0.1.2 moves closer when bars are hidden, while keeping the chevron center outside mandatory gestures and respecting visible status bars/cutouts. No persistent numeric HUD while closed.
- Real battery temperature, available system RAM, battery level, media-volume control, brightness-settings shortcut, optional explicitly labelled TCP connectivity test.
- Last 30 local sessions and temperature-over-time chart. Up to 1080 samples per session at 10-second intervals (rolling 3 hours). No invented FPS or 1% low.
- Durable write-ahead journal, readback and compare-before-restore; preserves externally changed values. Explicit recovery after interruption.
- Configurable exclusion list, WhatsApp selected by default. **No processes are force-stopped in this version**, so exclusions are stored for future validated controls, not presented as an active cleaner.

### Deliberately not implemented / not validated
Render downscale (especially HiOS Android 13), SurfaceFlinger FPS capture, process freezing/debloat, AOT compiler controls, per-app firewall, global game EQ, driver switching, touch controls, charging controls, root tuning and thermal bypass. No recording or WhatsApp shortcut. These are **not** quietly simulated.

## Build and install (phone-only friendly)

Only use the manually published test-preview link after **all** source, signing, identity, Android and real installation gates pass for that APK. A successful build job alone is not approval to install. 0.1.2 is published as a test preview; [phone reset checklist](docs/PHONE-RESET.md) applies to the user's explicitly approved one-time reset. End/recover old sessions first. The probe APK is CI-only and must not be installed on the phone.

### Fixed signing identity for preview and release
Both build types retain `id.veldafelix.spaces.preview2` and use the same privately backed-up PKCS12 key. The single repository secret is `SPACES_PREVIEW_SIGNING_BUNDLE`; its configuration has already been validated. There is **no cache/random-key fallback**. CI fails closed on a missing, malformed or wrong key; credential-free local builds are unsigned. Never paste the bundle into chat or publish it.

The manual **Signed release APK** workflow uses the same fixed identity, requires a full successful push-triggered Android run for that exact source, validates the matching `v…` version tag and prepares only a draft. It does not use separate release signing secrets or create another package. Future published versions must increase versionCode and retain the same package/key. [Policy and evidence](docs/UPDATE-IDENTITY.md).

Local: JDK 17, Android SDK 35, then `./gradlew testDebugUnitTest lintDebug assembleDebug`. Gradle wrapper is included. Dependencies: Android Gradle Plugin 8.7.3, Shizuku API/provider 13.1.5, JUnit 4.13.2; no backend/API key.

## Phone setup
1. Install and open; add supported installed games yourself.
2. Device → grant only permissions for the features you want (overlay, DND, notifications; usage access optional).
3. For advanced settings, start official Shizuku via wireless debugging, then grant this app access. Usually restart Shizuku after a phone reboot.
4. Start with Balanced + System refresh. Launch only from Play to start a session.
5. End via sidebar/notification/app; if interrupted, reopen → Check & recover.

**Do not grant Accessibility or root; neither is requested.** FusiMem is not changed by this app. User device supports +1/+2/+4 GB, currently +4 GB; test separately after reboot.

## Privacy and safety
Session data and recovery snapshots remain in app-private storage; Android cloud backup is disabled. No automatic network calls. Optional TCP test contacts Cloudflare `1.1.1.1:443`; update and Shizuku links open the browser. TCP timing is not in-match ping. Battery temperature is not CPU temperature. No anti-ban guarantee: game rules still apply. No edits to game files/memory and no thermal protection changes.

See [test plan](docs/TESTING.md), [architecture](docs/ARCHITECTURE.md) and [release notes](docs/RELEASE-NOTES.md).
