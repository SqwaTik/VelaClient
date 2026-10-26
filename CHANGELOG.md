# Changelog

Generated from `src/main/resources/assets/velaclient/changelog.json`, which is also shown in game under "What's new".

## 0.2.0 — 2026-10-27

**Death Recap, Storage Index and smart chat**

### New
- Death Recap: replay the last seconds before death from a free camera, your own eyes or the killer's, at 0.25×–1× speed, with a timeline of hits, totems, potions, armour and ping.
- Storage Index: search every chest, barrel and shulker you have opened, including shulkers inside chests; place a marker on any of them.
- Smart chat: coordinates become clickable markers, your name is highlighted with a soft chime, private messages fold into tabs.
- What's new: this changelog on the main menu.
- English interface, switchable to Russian in the settings.

### Improved
- Zipped resource packs load faster: entries are indexed once instead of scanned on every lookup.
- Loading screen: animation keeps going through reload stalls, fades out slower and reveals the already drawn menu.
- Shared interface components keep chips, cards and fields identical everywhere.

## 0.1.12 — 2026-10-26

**Wasted, Brew Viewer and a lot more**

### New
- Wasted: cinematic GTA-style death with its own sound.
- Brew Viewer, Item Pickups, Tab Ping, Food Info, Key Strokes, Array List, Custom Hotbar, Freelook.
- Hit Sound with built-in and custom sounds; vanilla hit sounds muted.
- Chat, Container Tools and Death modules; Auto Reconnect; Coordinates with portal maths and dimension icons.

### Improved
- Steady 800 FPS: cached HUD panels, batched GUI drawing, entity occlusion.
- Pinger animates, updates live and fades on its own.

### Fixed
- Mobs no longer twitch when hit.
- "?" instead of the minus sign in damage numbers.

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

### Fixed
- HUD panel fixes; unused map model preparation skipped for item frames.
