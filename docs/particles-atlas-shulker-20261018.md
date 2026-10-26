# Particle atlas and Shulker Preview

- Removed the obsolete Particles mode selector. Shape remains the single visual choice; existing event toggles and persisted shape are preserved.
- Packed eleven 1000×1000 source images into 128×128 cells with transparent gutters, atlas size 528×396. Originals remain available. The Particles render path loads only the atlas: base RGBA texture storage drops from 44,000,000 to 836,352 bytes (excluding driver overhead/mipmaps).
- All shapes now share one sprite batch. Working batch arrays persist across frames; extraction still publishes independent copied vertex/color arrays to avoid mutating queued render state. Disable/world change releases the retained renderer.
- Increased notification spacing around the divider, detail row and action. Checked with the production chrome's offline PNG renderer in build/offline-preview/shared-notices.png.
- Added Shulker Preview using vanilla tooltip placement and a 27-slot grid. Only client-known CONTAINER data is displayed; existing vanilla tooltip images are preserved. Snapshot caching follows immutable container-component identity and clears on disable/world leave/closed screen.

Verification: 358 tests passed; build and source bundle passed. Atlas dimensions/gutters, shape persistence and native tooltip descriptors checked. No game launched and no FPS improvement measured; remaining runtime visual/multiplayer and frame-time verification is explicit.
