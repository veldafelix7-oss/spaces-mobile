# spaces : mobile

Native Android game space for **Tecno Spark 20C (BG7), Android 13 / HiOS 13**, designed around 4 GB physical RAM. Landscape, restrained light glass, almost invisible blue tint. No WebView, Flutter engine, ads, account or analytics.

## Download and verified checks

**0.1.3 stability test preview is available — not final.** Exact source `bafbe42a296cdd886750e89f55029882e88c5f0c` passed build/lint, 79 JVM tests, two consecutive 43-test Android suites, pinned-key identity and actual same-key data-preserving update: [44741557154](https://github.com/veldafelix7-oss/spaces-mobile/actions/runs/44741557154). [Download 0.1.3 for Android 13](https://github.com/veldafelix7-oss/spaces-mobile/releases/download/preview-0.1.3-bafbe42/spaces-mobile-preview.apk) · [release/checksum](https://github.com/veldafelix7-oss/spaces-mobile/releases/tag/preview-0.1.3-bafbe42). SHA-256: `6d2a92c98e72ac862bea145bb54d1e5e458920593e3e3892ad4e087511e22242`.

**If you have 0.1.2 installed:** this is a direct in-place update with the same package and key. End/recover any active session, download the APK, then install over the app. **Do not uninstall or clear data**; local profile/history are intended to remain. Real TECNO/HiOS/Shizuku/gameplay checks are still pending. No leak, anti-kill or FPS/battery improvement is claimed. [Evidence and repair scope](docs/MEMORY-STABILITY.md) · [update notes](docs/RELEASE-NOTES.md).

**0.1.2 is the previous test preview**, retained for historical reference. 0.1.2 was the one-time migration from old-key 0.1.1; the reset directions below apply only if you are still on that old-key installation, not to 0.1.2→0.1.3. Always end/recover old sessions before any uninstall; [phone checklist](docs/PHONE-RESET.md).

## Scope: repair-only 0.1.3 preview; no proven performance gain

The app is functional groundwork with honest capability boundaries. A command's successful readback is **not** proof of a higher FPS. Testing on the actual BG7 is necessary; emulator results cannot certify HiOS or game compatibility.

### Implemented
- Initially empty collection; opt in to installed `com.tencent.igfit` (PUBGM FLASH) and `com.netease.newspike` (Blood Strike). Other apps cannot be added; games are not bundled/downloaded.
- Indonesian/English UI, landscape in both directions, original lightweight vector game scenes and lion emblem, plus real icons loaded locally from installed games.
- Per-game Eco/Balanced/Performance preferences (Android GameManager) and supported 60/90 Hz display settings, optional through Shizuku. Balanced/System defaults do not force performance settings.
- Optional Focus mode using Android DND (calls/messages silenced, alarms retained; actual OEM behavior must be verified).
- Foreground session started only from this app, ongoing stop action, optional usage-based session end (30-second launch grace, 20-second absence grace; sampled every 10 seconds).
- Thin edge-docked tab; quick inward swipe/tap opens metrics; hold and drag snaps left/right/top. The released 0.1.1 top tab reserves the full system band. The released 0.1.2 moves closer when bars are hidden, while keeping the chevron center outside mandatory gestures and respecting visible status bars/cutouts. No persistent numeric HUD while closed.
- Real battery temperature, available system RAM, battery level, media-volume control, brightness-settings shortcut, optional explicitly labelled TCP connectivity test.
- Last 30 local sessions and temperature-over-time chart. Up to 1080 samples per session at 10-second intervals (rolling 3 hours). No invented FPS or 1% low.
- Durable write-ahead journal, readback and compare-before-restore; preserves externally changed values. Explicit recovery after interruption.
- Configurable exclusion list, WhatsApp selected by default. **No processes are force-stopped in this version**, so exclusions are stored for future validated controls, not presented as an active cleaner.

### Deliberately not implemented / not validated
Render downscale (especially HiOS Android 13), SurfaceFlinger FPS capture, process freezing/debloat, AOT compiler controls, per-app firewall, global game EQ, driver switching, touch controls, charging controls, root tuning and thermal bypass. No recording or WhatsApp shortcut. These are **not** quietly simulated.

## Build and install (phone-only friendly)

Only use the manually published test-preview link after **all** source, signing, identity, Android and real installation gates pass for that APK. A successful build job alone is not approval to install. 0.1.3 is published as a same-key update preview. End/recover active sessions before updating; no uninstall or reset. The [phone reset checklist](docs/PHONE-RESET.md) applies only to the historical one-time 0.1.1→0.1.2 migration. The probe APK is CI-only and must not be installed on the phone.

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
