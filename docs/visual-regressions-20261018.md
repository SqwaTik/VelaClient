# Visual regression repair — 2026-10-18

## Implemented

- Near uses a shared circular radius and fixed icon scale. Crowded bearings are separated angularly instead of adding radial lanes. Radius follows population smoothly; appearance changes opacity, not individual radius.
- Friends visual selection reuses BooleanGroup and the same popup renderer/input routing as Trajectories. Existing per-effect configuration remains authoritative.
- Particles supports independently persisted hit, jump, landing, target death and totem flags, presented as a grouped event selector.
- Damage feedback observes client-thread hurt packets and accepted FakePlayer damage. Local totems also emit. No duplicate hurt-timer polling remains.
- RenderTweaks' native particle suppression no longer disables the custom visual. Effect lifetime and the 128-particle hard limit remain bounded; disable/world change clears transient data.

## Verification

- 352 tests passed, including circular packing, settings persistence and exact Minecraft packet-handler descriptors.
- Build and source bundle checked separately before deployment.
- No game or launcher was started. Runtime appearance, event visibility and frame times remain unverified.

## Remaining performance investigation

The reported persistent loss after switching configurations is not established as fixed. Available JFR captures predate this build (latest 2026-10-12); they cannot prove a regression introduced in the October 18 build. The current game log has no recorded ZakoOpt guard failure matching this report. A comparable frame-time capture is still needed to identify the dominant stage rather than disable unrelated optimizations speculatively.
