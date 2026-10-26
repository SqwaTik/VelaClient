# Client foundations

- `ModuleRegistry` owns module discovery and immutable snapshots. Register HUD modules with `ModuleCategory.HUD`; the HUD editor discovers them automatically. Specialized rendering still belongs in `HudLayoutService`.
- `ClientModule` owns availability, enable/disable lifecycle and bindings. UI code must call its methods rather than keep another enabled flag.
- `BindInput` encodes keyboard and mouse inputs. `KeyLabels` formats them. Escape/Delete clear a binding while capturing; text fields retain normal editing behavior.
- `TextEdit` owns text, caret and selection. `TextInput` handles commands with injected clipboard/validation; `GameTextInput` adapts Minecraft events. `TextFieldDraw` renders the model.
- `UiControls` owns keyed animation state for buttons, checkboxes and sliders. Use stable semantic IDs per control, not screen coordinates. It respects reduced motion and the animation preference.
- `JsonFileStore` is the common atomic JSON persistence layer. Unchanged serialized data skips writing; mutations must still request saving through their owning service.
- `MarkerService` owns marker persistence and validation. `MarkerRuntime` handles in-world input/rendering; `MarkerPanel` edits service records without duplicating persistence logic.

Keep model/service tests independent of Minecraft where possible. Run `gradlew build` after changing shared behavior. A passing build does not replace in-game visual testing.

Legacy screen-specific layout and animation code remains; migrate it to these foundations when changing the corresponding screen. Do not introduce a second clipboard handler, key encoding or JSON writer.

# Package layout (since 2026-10-26)

| Package | Contents |
|---|---|
| `dev.sqwat.vela` | `VelaClient` entrypoint. |
| `core`, `config` | `VelaServices` wiring, profiles and JSON persistence. |
| `module` | Module framework: `ClientModule`, `ModuleRegistry`, `ModuleCategory`, `setting/`, `ZakoOptimization` (optimization page). |
| `module.visual` | Visual modules (Shaders, Crosshair, JumpCircle, KillEffect, …). |
| `module.utility` | Utility modules (Freelook, ShulkerPreview, RenderTweaks, HitSound, …). |
| `module.command` | Command automation modules and scheduling. |
| `render.shader` | GPU passes: world fog/wet, sky, hands, motion blur, custom GLSL sky. |
| `render.entity` | Entity visibility and labels: occlusion culling, name tag caches, friend tags. |
| `render.effect` | World-space effects: waves, particles, trails, trajectories. |
| `render.camera` | Zoom and camera motion. |
| `render.resource` | Resource pack cache, reload handling, texture decode cache. |
| `render.perf` | Profiling (`/vela perf`) and render-path micro-optimizations. |
| `ui` | Notifications, UI sounds, menu transitions. |
| `ui.hud` | `HudLayoutService` plus one class per self-contained panel (`WatermarkHud`, `NearArrowsHud`, `KeyStrokesHud`). New panels should follow that pattern. |
| `ui.input` | Binding and text-editing models. |
| `ui.screen`, `ui.render`, `ui.animation` | Screens, drawing primitives, animation. |
| `mixin.*` | Mixins only, grouped by what they hook: `hud`, `screen`, `world`, `entity`, `camera`, `input`, `resource`, `perf`. Register as `"group.Name"` in `velaclient.mixins.json`. |
| `zako` (separate source set) | Vendored ZakoOpt (LGPL-3.0); kept in its original layout. |
