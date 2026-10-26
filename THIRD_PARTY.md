# Bundled terrain cache

## User-owned visual assets

The Energy project assets `images/esp/target.png`, `images/esp/glow.png`,
`images/esp/triangle.png` and `dashtrail/dashcubics/dashcubic1.png` through
`dashcubic21.png` were supplied by the project owner for integration into Vela.
The arrow is packaged as `textures/effects/near_arrow.png`. Their rendering code
is implemented for Vela's Minecraft 26.2 pipeline.

## Bobby

Bobby 5.2.15+mc26.2, by johni0702, is included as an unmodified nested JAR under LGPL-3.0-or-later.

- Project and source: https://github.com/Johni0702/bobby
- Exact release: https://modrinth.com/mod/bobby/version/KLiFVFju
- License: https://github.com/Johni0702/bobby/blob/master/LICENSE
- SHA-512: `25261b02b2db7f969de5c7013b7e223508a9e6005d61f287911af30c0fecd06c40df1e3cb4d6f7b714093f6b8ecffb5dfba25726c370ef3a0a74dcebf7711631`

Far controls display of cached multiplayer terrain with a 4–32 chunk slider. It does not obtain unseen terrain or extend entity simulation. Bobby records received terrain in `.bobby`; the cache can contain outdated terrain until fresh server chunks arrive. Dynamic world identification is enabled. Fake block entities are disabled and distant cache entries are unloaded without a delay. Far is disabled by default. The render-distance option is restored when Far is disabled.

Local frame measurements: `/vela perf start`, then `/vela perf stop`. Reports bounded samples of render frame intervals and frames within one second after an attack. This is not GPU timing, generated-frame counting, or a promise of zero stalls.
