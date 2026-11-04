# Changelog

Generated from `src/main/resources/assets/velaclient/changelog.json`, which is also shown in game under "What's new".

## 0.2.1 — 2026-11-03

**Render optimization and skin fix**

### New
- Render optimization: a new set of render optimizations (fewer GPU waits and framebuffer switches, a vertex ring buffer, HUD at the monitor rate, LOD for distant items, mobs, players and particles, caches for text, signs and block entities, multithreaded particles and models) with its own settings screen, an in-game FPS panel (F6) and automatic tuning for your device. Open it from Settings > Interface > Render optimization. With VulkanMod the OpenGL-only parts stay off.
- Fast model building: mob and player models are written into the vertex buffer a whole cube at a time instead of vertex by vertex, which takes noticeable load off the render thread on Vulkan. It can be turned off in the optimization settings.
- Minecraft 26.3 build: the same Vela for 26.3, built from this code by a port layer (versions/26.3). Shaders, the first-person hands, the sky and the HUD cache are adapted to the new renderer; a test checks every mixin target against the game before it starts.
- NeoForge builds for Minecraft 26.2, 26.3 and 1.21.11 (versions/26.2-neoforge, versions/26.3-neoforge, versions/1.21.11-neoforge, sharing versions/neoforge): the same Vela, its Fabric API calls answered by NeoForge events, with a configuration screen in the NeoForge mod list. The hotbar's Drag Mode offset and the menu transition follow NeoForge's GUI layers.
- Minecraft 1.21.11 build (Fabric, versions/1.21.11): the same Vela on 1.21.11, ported by the port layer. The HUD, screens, camera, zoom, Freelook, Death Recap, Wasted, the bundled Mod Menu, Bobby and IAS, the velaopt optimizations and the shader sky, fog, hand and hit bubble effects work. Left out: the parts of velaopt written for the 26.x renderer (Vulkan and GPU caches, the HUD cache, the GUI item atlas, parallel models) and the wet surface effect of the shader fog. The Mod Menu button in the pause menu is always the full-width one.

### Fixed
- Offline accounts and players on offline-mode servers no longer turn into Steve or Alex: Vela loads the Mojang skin that belongs to the nickname. A skin set in Vela Launcher still comes first.
- The loading screen, the menu header and the farewell screen show the Minecraft version that is running instead of always 26.2, and the mod description in the mod list names the right version.

## 0.2.0 — 2026-10-27

**Death Recap, Storage Index and smart chat**

### New
- Death Recap: watch the last seconds before death from a free camera, your own eyes or the killer's, slowed down, with a timeline of hits, totems, potions, armour and ping.
- Save replays with your own camera, speed and length to the vela-replays folder.
- Recordings: every saved replay in one list, opened from the death screen, the module settings or its own key.
- Storage Index: search everything in the chests, barrels and shulkers you have opened and mark the right one.
- Smart chat: clickable coordinates, mentions of your name, private messages in tabs.
- Key combinations such as Ctrl+F for every bind, the menu keys in the settings included.
- Chat privacy: a button beside Dragmode above the chat blurs what you type after /l, /login, /reg and /register; hovering either button opens both with a caption.
- What's new: this list of changes in the main menu.
- English interface, with Russian available in the settings.
- Mace Smash: a mace smash attack cracks the ground or sends out a shockwave.
- Loot Memory: searched chests, broken spawners and opened trial vaults are marked, so you never check the same spot twice.
- Vela players are marked with the Vela logo in the tab list.
- Fog culling: terrain fully hidden by fog, as in the Nether, is not drawn.
- Hit Sound: recorded impact sounds.
- Explosion Wave: the blast runs along the ground, flows around walls and merges with nearby blasts; a gust of wind as it passes and a short stun up close.
- Item Highlight: add any item with your own color.
- Hand Swap: swap the main hand with your own key.
- Render Tweaks: hands no longer bob while walking.
- Interface sounds: the new Rhythm set — clicks in Vela and on vanilla buttons play with a varied pitch.
- Shaders: a Sky quality setting (low, medium, high).
- Music HUD: whatever is playing on the PC — a browser tab (YouTube, Twitch, YouTube Music…), Yandex Music, Spotify and other players: the cover with the service's icon, a spectrum that follows the music, a progress bar and playback buttons in the chat. Lyrics from LRCLIB and NetEase are highlighted word by word; where a source has no word timing, the highlight is paced by syllables. Covers and lyrics are cached on disk.
- Swing Animation: a new set of first-person swings (Swing, Shift, Lunge, Spear, Down, Jelly, Slap, Bonk, Circle, Realism) with strength and duration; replaces Swing Animations.
- Music HUD: Mini mode — a notch at the top edge of the screen with the cover and rhythm bars that opens up to show the current line.
- Music HUD: the colour of the sung lyrics — the client's colour or any other from the colour wheel.
- Item Highlight: Custom color — one colour of your choice from the colour wheel for every category.
- Module menu: a panel layout — modules as a list, the selected module's settings in their own pane; turned on in the settings (Interface → Panel module menu).
- Music HUD: click the progress bar to jump to that point in the track.
- Mods button in the main menu: the list of installed mods with their settings (Mod Menu, built in).
- Optimization: Monitor-based limit (off by default): with Unlimited FPS, frames are capped at twice the monitor's refresh rate so the graphics card runs cooler.
- Server rules: on ReallyWorld, FunTime and HolyWorld the functions their rules forbid are switched off automatically, with a notification saying which; they come back when you leave.
- Server opt-out: any server can switch off competitive modules (or a listed set) by sending the velaclient:disable packet; they come back when you leave.
- Licences: a plain mod description, MIT for Vela with the full text and author, LGPL-3.0-or-later for the bundled parts, and every third-party sound and font credited in THIRD_PARTY.md.
- HUD cache, always on: the HUD is built at the monitor's refresh rate instead of every frame; the crosshair, vignette and in-world effects stay live.

### Improved
- GLSL editor: rebuilt with code colours, line numbers, error lines, the Vela font and larger text; the insert chips and the example button are gone.
- Modules are sorted A–Z (favourites stay on top).
- Reflection quality now changes the reflection resolution, steps and distance.
- The menu remembers the tab and scroll position after closing.
- Module categories reviewed: Brew Viewer, Tab Ping and Item Highlight are in Utilities; Motion Blur and Block Overlay are in Visuals.
- Settings scroll smoothly: text no longer gets laid out again on every frame while scrolling.
- HUD editor: the middle-click and right-click menus are redesigned — more compact, every setting has a description, colour settings open a colour wheel, and the size is picked with a switch.
- Trajectories: the throw preview is a smooth glowing curve from the hand with a ring where it lands.
- The main menu and the world and server lists run at the frame limit from the options instead of a fixed 60 FPS.
- A new Vela logo.
- In-Game Account Switcher and Mod Menu are built into Vela; separate copies are no longer needed.
- Fabric Loader 0.20.1; Vela runs on Fabric Loader 0.19.2 and newer.
- Every HUD panel uses the same background as the Music HUD.
- Trajectories stay still while walking, whether view bobbing is on or off.
- Hologram hiding checks the entity type first, which takes less time per frame.
- Vela takes half the space: the jar is down from 23 MB to about 11 MB.
- Config import keys are several times shorter; older keys still work.
- Key Strokes: no background behind the keys.
- Account switching, the Mods screen and the Far terrain cache are now built into Vela itself instead of bundled mods: fewer files to load and less to start up.
- Frame Generation and the Upscaler are removed along with their models, so they no longer cost memory or frame time.
- Interface fonts are baked from the font files on first launch and cached, instead of 57 prebuilt glyph atlases; the fonts keep only English, Russian and the interface symbols (other scripts use Minecraft's font). The mod is about 6 MB smaller.
- Profiler: a continuous mode that keeps the last three minutes and saves them on its own after a stutter, plus a text report next to every profile (slow frames, GC pauses, hottest methods, allocations).
- Name tags: each entity's name is built once per tick instead of every frame, and its text layout is found without re-reading the text.
- HUD: panels that are not animating are drawn in shared batches instead of one set of draw calls per panel.
- Minecraft telemetry and the Realms notification polling on the vanilla title screen are switched off.
- Holding Tab on a crowded server no longer drops FPS: the player list is sorted a few times a second instead of every frame, and the interface layers find overlaps through a grid instead of checking every element against every other.
- Fewer stutters: settings are read without waiting for a save to disk, the music panel and its brand detection no longer allocate every frame, and the HUD no longer copies the player list every tick.
- Armour of far players: when the server strips equipment from distant players (anti-ESP), the armour seen up close in the last 10 minutes stays on them.
- Lyrics in time: copies of one song's text are often timed differently (by ear, or to another release), from a few hundred milliseconds to seconds. The text whose timing most of the other copies agree with is now shown, instead of simply the one closest in length.
- More lyrics: QQ Music is a fourth source, with a large catalogue that includes Russian rap.
- Music HUD covers: when the player sends no cover (Yandex Music) and Deezer and iTunes do not have the song, the cover is taken from NetEase.
- More lyrics: when no source has a song's text timed, the plain text is shown and paced through the song, instead of nothing (for example Baby Cute).
- More lyrics: entries where the source swapped the artist and the title are found too.
- In-world effect modules are looked up once instead of on every frame.
- Launcher: faster game start through the Java 25 AOT cache, recorded on the first launch and reused afterwards.

### Fixed
- Hit Sound: removed the pause at the end of the Heavy hit sound.
- Shaders: stripes on wet surfaces and a milky floor at night.
- Japanese, Chinese and Korean text (song titles, chat, names) shows up instead of question marks.
- Key Strokes shows W A S D on any keyboard layout, not the letters of the current layout.
- Reset module binds also resets the menu and search keys in the settings.
- Joining servers with a bot check such as mc.magicworld.pw: the ping display waits 20 seconds after entering a world and saved chunks appear after 10 seconds, so the check no longer kicks you.
- The automatic FPS profile keeps recording after a world change instead of stopping.
- The middle-click menu in HUD editing marks experimental settings with EXPERIMENTAL, as the module settings do.
- Music HUD covers: a song by several artists ("MellSher, 5opka") is found when the catalogue lists it under any of them, not only the first; the title alone is also tried, still checked against the artists.
- Music HUD covers: a song with no cover found is looked up again after a few minutes instead of never until restart, and a failed lookup waits 20 seconds instead of being repeated every tick (which got NetEase to refuse requests).
- NetEase's "too many requests" answer is no longer taken for "no such song" (covers and lyrics).
- Removed the "Invalid path in mod resource-pack" error in the log on every resource reload (a notice file sat among the sounds).
- Music HUD covers that only NetEase has (for example Imbalance) no longer go missing when NetEase turns away requests made at the same moment as the lyrics search: requests are spaced out and retried once, and a failure is written to the log.
- Russian lyrics stored with a broken encoding (garbled letters) are repaired before they are shown.

## 0.1.12 — 2026-10-26

**Wasted, Brew Viewer and more**

### New
- Wasted: a cinematic GTA-style death with its own sound.
- Brew Viewer, Item Pickups, Tab Ping, Food Info, Key Strokes, Array List, Custom Hotbar and Freelook.
- Hit Sound with built-in and custom sounds.
- Chat, Container Tools and Death modules, Auto Reconnect, Coordinates with portal maths and dimension icons.

### Improved
- Pinger warnings are animated and update live.

## 0.1.11 — 2026-09-25

**Editors and Click Pearl**

### Improved
- Macros and Interval Commands editors: shared controls, smooth scrolling, no switch delay.
- Friend name tags use the tab list colour.

### New
- Click Pearl: throw a pearl from the hotbar with its own key and return to the slot.

## 0.1.10 — 2026-09-24

**Command editor**

### Improved
- Explicit create and edit modes; the form clears after adding.

## 0.1.9 — 2026-09-24

**Gamma and command automation**

### New
- Gamma adapting to local light; Macros and Interval Commands.

## 0.1.8 — 2026-09-21

**HUD fixes**

### Improved
- HUD panels refined.
