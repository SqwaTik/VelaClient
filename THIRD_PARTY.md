# Third-party components

Vela Client is licensed under the MIT License (see `LICENSE`). The components below keep their own licenses.
License texts and notices ship inside the jar under `META-INF/licenses` and next to the assets they cover.

## In-Game Account Switcher

In-Game Account Switcher 9.0.7 (tag `9.0.7`), by The_Fireplace and VidTu, is included as modified source code under LGPL-3.0-or-later in `src/main/java/ru/vidtu/ias`. Vela uses it for Microsoft sign-in, encrypted account storage and session replacement. Changes: the IAS screens, Forge/NeoForge/Mod Menu entrypoints and the multiplayer-screen mixin are removed (Vela's own screens replace them), and Error Prone annotations are stripped.

- Project: https://modrinth.com/mod/in-game-account-switcher
- Source: https://github.com/The-Fireplace-Minecraft-Mods/In-Game-Account-Switcher
- License texts: `META-INF/licenses/LGPL-3.0.md`, `META-INF/licenses/GPL-3.0.md`, notice `META-INF/licenses/IAS-NOTICE.txt`

## Bobby

Bobby 5.2.15+mc26.2 (tag `v5.2.15+mc26.2`), by johni0702, is included as modified source code under LGPL-3.0-or-later in `src/main/java/de/johni0702/minecraft/bobby`. Changes: commands, config file and config screens are removed; settings are fixed in code and the cache is controlled by Vela's Far module.

- Project and source: https://github.com/Johni0702/bobby
- License texts: `META-INF/licenses/LGPL-3.0.md`, `META-INF/licenses/GPL-3.0.md`

Far shows multiplayer terrain the player has already received, with a 4–32 chunk slider. It does not obtain unseen terrain or extend entity simulation. Bobby stores received terrain in `.bobby`; the cache can show outdated terrain until fresh server chunks arrive.

## Mod Menu

Mod Menu 20.0.3 (tag `v20.0.3`), by TerraformersMC, is included as modified source code under the MIT License in `src/main/java/com/terraformersmc/modmenu`. It provides the Mods screen opened from Vela's title screen. Changes: update checking, Quilt support, Text Placeholder API support and Mod Menu's own config entry are removed; it runs inside the `velaclient` mod container.

- Project and source: https://github.com/TerraformersMC/ModMenu
- License text: `META-INF/licenses/ModMenu-MIT.txt`

## Sounds

- Hit sounds `heavy.ogg`, `body1-3.ogg`: Mixkit (https://mixkit.co), Mixkit Sound Effects Free License; trimmed, faded and loudness-normalised. Notice: `META-INF/licenses/Hit-Sounds-NOTICE.txt`.
- Interface sounds `click_001`, `confirmation_001`, `open_002`, `switch_001`, `tick_001`: Kenney Interface Sounds (https://kenney.nl), CC0 1.0. License: `assets/velaclient/sounds/kenney-license.txt`.

## Font

Inter (https://github.com/rsms/inter), by The Inter Project Authors, SIL Open Font License 1.1. License: `assets/velaclient/font/inter_license.txt`. Vela bakes glyph atlases from the font files at runtime; the font files are unmodified.
