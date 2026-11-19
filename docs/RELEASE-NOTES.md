# spaces : mobile — 0.1.2 work in progress (NOT RELEASED)

**Do not distribute the current candidate.** Sidebar positioning passed 49 JVM tests and four 27/27 Android suites, but the signing certificate does not match installed 0.1.1. No direct-update compatibility or completed data migration is claimed. [Signing blocker](UPDATE-IDENTITY.md) · [verification](TEST-RESULTS.md).

- Top tab moves to the nearest safe visual origin instead of permanently reserving hidden status-bar height. Its chevron center stays outside mandatory gestures; visible status bars/cutouts remain protected. The position follows inset changes without changing the saved horizontal fraction. Side docks remain unchanged.
- VersionCode increases from 2 to 3; application ID remains `id.veldafelix.spaces.preview2`. A valid but different certificate is no longer accepted. No preview3 or user-data deletion workaround.
- CI blocks publication until certificate/package/version checks, two full Android suites and actual same-package replacement/data retention all pass. The manual production workflow is guarded as well. Restoring the original private signing key is still required.
- Phone verification of the new top position is pending; no new APK has been delivered. This is not the final app or a feature-completeness claim.

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
