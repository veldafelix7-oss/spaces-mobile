# spaces : mobile — 0.1.2 (NOT RELEASED; one-time reset required)

**Do not distribute until signed installation checks pass.** The user approved starting fresh without old app data, using only a phone. A new key is privately backed up and pinned. The owner's GitHub Actions secret still needs configuration; there is no signed fallback build and no new download yet. [Signing setup](UPDATE-IDENTITY.md) · [phone reset steps](PHONE-RESET.md) · [verification](TEST-RESULTS.md).

- Top tab moves to the nearest safe visual origin instead of permanently reserving hidden status-bar height. Its chevron center stays outside mandatory gestures; visible status bars/cutouts remain protected. Earlier app-code validation passed 49 JVM tests and four 27/27 Android suites; the new signing/install path must still pass real tests.
- Version 0.1.2/code 3 retains `id.veldafelix.spaces.preview2`. Both preview and release now use that same package and private key. No preview3/third-app workaround.
- **Not an update of old-key 0.1.1.** After an APK is verified and available, end/recover sessions in the old apps first. Replacing 0.1.1's key requires one user-performed uninstall/reinstall; its collection/profiles/history are intentionally not migrated. If recovery is pending, do not uninstall. Do not remove current apps while the new build is still blocked.
- Only this exact version is authorized for reset. Future versions must retain the new fixed key and update in place. A CI-only 3→4 probe must prove private-data retention without adding another package; that probe is not a user download.
- CI requires a valid pinned private secret, source/lint checks, two complete Android suites, identity verification and actual installation/reset/update checks before preparing a draft. Publication remains manual. No feature-completeness, phone compatibility or performance-gain claim.

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
