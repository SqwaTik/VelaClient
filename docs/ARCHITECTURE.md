# Client foundations

- `ModuleRegistry` owns module discovery and immutable snapshots. Register HUD modules with `ModuleCategory.HUD`; the HUD editor discovers them automatically. Specialized rendering still belongs in `HudLayoutService`.
- `ClientModule` owns availability, enable/disable lifecycle and bindings. UI code must call its methods rather than keep another enabled flag.
- Mixins and render hooks reach modules through `Modules.active(X.class)` (enabled and available, else null) or `Modules.get(X.class)`. Do not copy `services().modules().byId("id") instanceof X` checks: the class is the key, so renaming an id cannot silently switch a feature off.
- `BindInput` encodes keyboard and mouse inputs. `KeyLabels` formats them. Escape/Delete clear a binding while capturing; text fields retain normal editing behavior.
- `TextEdit` owns text, caret and selection. `TextInput` handles commands with injected clipboard/validation; `GameTextInput` adapts Minecraft events. `TextFieldDraw` renders the model.
- `UiControls` owns keyed animation state for buttons, checkboxes and sliders. Use stable semantic IDs per control, not screen coordinates. It respects reduced motion and the animation preference.
- `JsonFileStore` is the common atomic JSON persistence layer. Unchanged serialized data skips writing; mutations must still request saving through their owning service.
- `MarkerService` owns marker persistence and validation. `MarkerRuntime` handles in-world input/rendering; `MarkerPanel` edits service records without duplicating persistence logic.

Keep model/service tests independent of Minecraft where possible. Run `gradlew build` after changing shared behavior. A passing build does not replace in-game visual testing.

Legacy screen-specific layout and animation code remains; migrate it to these foundations when changing the corresponding screen. Do not introduce a second clipboard handler, key encoding or JSON writer.

# Project layout (since 2026-10-26)

```
build.gradle, gradle/, libs/      build (libs: Bobby, IAS jars that are not on Maven)
docs/                              ARCHITECTURE (this file), SOURCE_BUILD, design prompt
tools/                             asset generators, profiling helpers, ZakoOpt update script
src/main/java/dev/sqwat/vela/      the mod (one source root)
src/main/java/ru/vidtu/ias/screen/ IAS bridge; must live in IAS's package for package-private access
src/main/resources/                assets, mixin configs, fabric.mod.json
src/test/                          JUnit tests; not packaged into the mod jar
run/                               local game directory for gradle runClient (ignored)
```

Dated design and performance notes moved to the workspace `notes/vela-client-history/`.

# Package layout

| Package | Contents |
|---|---|
| `dev.sqwat.vela` | `VelaClient` entrypoint. |
| `core` | `VelaServices` wiring; config profiles and JSON persistence; local `.` commands; Voice Chat plugin; generated sound clips. |
| `account`, `friend`, `marker` | Accounts/IAS, friends and local appearance, world markers. Each owns its service and persistence. |
| `module` | Module framework: `ClientModule`, `ModuleRegistry`, `Modules` (static typed lookup), `ModuleCategory`, `setting/`, `ZakoOptimization`. |
| `module.visual` | Visual and HUD-data modules (Shaders, Crosshair, Wasted, Item Pickups, Tab Ping, …). |
| `module.utility` | Utility modules (Chat, Container Tools, Death, Freelook, Hit Sound, command automation, …). |
| `render.shader` | GPU passes: world fog/wet, sky, hands, motion blur, custom GLSL sky. |
| `render.entity` | Entity visibility and labels: occlusion culling, name tag caches, friend tags. |
| `render.effect` | World-space effects: waves, particles, trails, trajectories. |
| `render.camera` | Zoom and camera motion. |
| `render.resource` | Resource pack cache, reload handling, texture decode cache. |
| `render.perf` | Profiling (`/vela perf`) and render-path micro-optimizations. |
| `ui` | Notifications, UI sounds, menu transitions. |
| `ui.hud` | `HudLayoutService` plus one class per self-contained panel (`WatermarkHud`, `NearArrowsHud`, `KeyStrokesHud`). New panels should follow that pattern. |
| `ui.input` | Binding and text-editing models. |
| `ui.screen`, `ui.render`, `ui.animation` | Screens (dashboard, container search box, editors), drawing primitives, animation. |
| `mixin.*` | Mixins only, grouped by what they hook: `hud`, `screen`, `world`, `entity`, `camera`, `input`, `perf` (render and resource-reload performance). Register as `"group.Name"` in `velaclient.mixins.json`. |
| `zako` | Vendored ZakoOpt (LGPL-3.0) in its upstream layout, so `tools/prepare-zako-update.cjs` can merge new releases; the jar ships these sources under `META-INF/zakoopt-source`. |
