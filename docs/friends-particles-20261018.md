# Friends and visual fixes — 2026-10-18

- Friends visual selection is one labelled dropdown block. Individual flags are authoritative; the retired master switch is no longer checked. Direct attacks on friends are cancelled before local FakePlayer or normal attack handling when protection is enabled.
- Near uses actual interpolated bearings, a fixed shared radius, fixed icon scale, and green friend arrows. Population changes no longer reposition bearings. Closely aligned players can consequently overlap; no false bearings are introduced to separate them.
- Unbound HOLD bindings normalize to TOGGLE. This fixes enabled HUD state being discarded during save. Previously discarded enabled state cannot be reconstructed; enable Armor HUD once after updating.
- Click Pearl captures short input events, selects a hotbar slot, uses the native item API after two ticks and restores the slot. No block interaction or hand-renderer dependency. Cooldown, GUI, manual slot changes and server rejection remain valid reasons not to throw.
- Particles uses eleven Kimiko textures (CC0 notice included), selectable shape, duration, gravity, speed, twist and fade scaling. It retains bounded storage and batched rendering. This is not a complete port of Kimiko's collision simulation.
- FakePlayer updates vanilla limb animation after the full movement tick. It remains client-local, not a server entity.
- Marker notification has a divider and extra detail-row clearance.
- Added Ambience: client clock time and clear/rain/thunder presentation. Native 26.2 ClientClockManager is used for timeline-driven sky; server clock state is not changed.

356 tests passed; full build and source bundle passed. Native method descriptors and asset packaging checked offline. No game/launcher started; live appearance, multiplayer acceptance and performance remain unverified. Previous persistent FPS-loss investigation remains open.
