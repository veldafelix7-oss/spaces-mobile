# spaces : mobile — 0.1.1 preview

Follow-up to real Tecno Spark 20C feedback:
- Thin edge-docked sidebar tab with chevron. Hold and drag to snap left/right/top; top tab becomes horizontal. Edge and normalized position survive rotation/restart; legacy positions migrate.
- Compact session panel with a fixed collapse arrow, outside the scrolling controls; no large Close button. The closed handle is hidden while the panel is open.
- Entire navigation tiles are touch targets; switching tabs retains the rail instead of rebuilding the full screen.
- More visible static glass highlights/rims and subtle neutral background contrast. No live blur added.
- Original installed-game icons shown on game cards; no bundled or downloaded game artwork.
- Neutral concise Indonesian copy, explicit Shizuku connection/permission label instead of ambiguous Standard mode.
- Edge-to-edge app backdrop with cutout-safe navigation controls; the app does not change the games' own camera/cutout policies.
- **Installation change:** 0.1.0's preview signing identity could not be recovered from CI. This build uses a separate preview application ID (`id.veldafelix.spaces.preview2`) and launcher label `spaces : mobile · 0.1.1`. It installs alongside the old preview, not over it. Old local data is not deleted or automatically migrated. End/recover all old sessions before starting one in the new app. Collection and permissions must be configured again in the new app.
- Same-key in-place production updates remain blocked until private signing secrets are configured. CI verifies both APK signatures and the isolated preview ID; emulator checks old private data survives side-by-side installation.

Unchanged safety limits: no FPS-unlock, verified FPS capture, render downscale, automatic process stopping, global EQ or touch-driver control. No game-file/memory modification or thermal bypass. HiOS gestures and hardware performance still require real-device testing. Slightly smoother play reported by the user is subjective, not a measured FPS gain.
