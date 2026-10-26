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
