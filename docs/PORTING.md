# Porting Vela to other Minecraft versions

`tools/port_mappings.py` takes the 26.2 build of Vela and finds every Minecraft class, field and method it uses. It looks each one up in another game version, renames what moved, and lists what has to be ported by hand.

Requirements: Python 3.10+ (stdlib only) and internet access the first time a version is used. Build Vela first, because the script reads the compiled classes:

```bash
JAVA_HOME=~/.gradle/jdks/jdk-25.0.4.1+1 ./gradlew classes
```

## Running it

```bash
python -I tools/port_mappings.py 1.21.11
```

```bash
python -I tools/port_mappings.py 1.21.11 --apply
```

| Option | Default | Meaning |
|---|---|---|
| `target` | — | Target game version (`26.3`, `1.21.11`, `1.21.5`, `1.21.4`, `1.20.1`, `1.19.4`, `1.16.5`, …) |
| `--from` | `minecraft_version` from `gradle.properties` | Version the sources are written for |
| `--classes` | `build/classes/java/main` | Compiled Vela classes (several allowed) |
| `--sources` | `src/main/java` | Sources to locate and rewrite |
| `--out` | `build/port/<target>` | Output folder |
| `--cache` | `~/.cache/vela-port` | Downloaded jars, mappings and the per-version model |
| `--apply` | off | Also write the rewritten sources to `<out>/src` |

The script downloads `client.jar`, the `com.mojang` libraries (authlib, brigadier, datafixerupper, …) and, for obfuscated versions (1.21.11 and older), Mojang's `client.txt` mappings from piston-meta. It checks each download against its SHA-1. It then stores a compact model of every class (supertypes, fields, methods) in Mojang names in the cache. Later runs take a few seconds.

The working tree is never changed. `--apply` writes a copy to `build/port/<target>/src`. Compare it with:

```bash
git diff --no-index --ignore-cr-at-eol src/main/java build/port/1.21.11/src/main/java
```

## What is scanned

- Every class, field and method reference in the constant pool, including descriptors, generic signatures, lambdas (`invokedynamic`), and methods that override Minecraft methods.
- Member references are resolved through the real owner, which can be a superclass or interface in the source version. Members Vela declares itself are skipped.
- Mixins:
  - `@Mixin` targets.
  - Injector `method = …` selectors.
  - `@At(target = …)`, including `NEW`.
  - `@Slice`.
  - MixinExtras `@Definition`.
  - `@Shadow` and `@Overwrite` members.
  - `@Accessor` and `@Invoker`, with names derived from `get`/`is`/`set`/`call`/`invoke`.

## How things are matched

Classes, in order:

1. A manual rename from `tools/port/<target>.renames`.
2. The same name exists in the target.
3. A nested class follows its renamed outer class. An outer class follows its nested classes when they all moved to the same place.
4. Exactly one class that is new in the target has the same simple name. Package moves are found this way, for example `com.mojang.blaze3d.*` → `com.mojang.renderpearl.*` in 26.3.
5. A new class shares most member names with the old one (Jaccard ≥ 0.6, ahead of the next candidate by ≥ 0.15). Examples: `GuiGraphicsExtractor` → `GuiGraphics` and `ContainerInput` → `ClickType`.

Members:

1. A manual rename.
2. The same name and descriptor exist in the target (through supertypes).
3. Exactly one member that is new in the class has the same descriptor, and the two names share a word: `mainCamera` → `getMainCamera`. A candidate without a shared word is only suggested in the report ("maybe `x`") and is not applied. Two old names that guess the same new one are both reported as missing.

## Output

`build/port/<target>/`:

- `report.md` lists every item that needs a person, with `file:line`:
  - **classes missing**: no match in the target. Port the feature, or drop it for that version.
  - **fields missing / methods missing**: with the kind of use (call, access, `@Inject method`, `@At`, `@Shadow`, lambda …) and sometimes a "maybe" hint.
  - **signatures changed**: the name still exists with another descriptor. Shows the target's descriptors, the constructors, or "now a field" / "now a method".
  - **renames not applied**: a file uses both the old name for a reference that stays and a renamed one. These files are left alone.
  - **classes renamed / members renamed**: what was applied automatically.
- `report.json` is the same in machine-readable form.
- `renames.txt` uses the `old.Class=new.Class` format of the multi-version build's rename filter.
- `src/` is written only with `--apply`.

## Manual renames

`tools/port/<target>.renames`, one `old=new` entry per line, with no spaces around `=`. Lines that start with `#` are comments:

```
# class
net.minecraft.client.renderer.state.level.SkyRenderState=net.minecraft.client.renderer.state.SkyRenderState
# member of one class (field or method), owner in source-version names
net.minecraft.client.gui.components.AbstractWidget#extractWidgetRenderState=renderWidget
# member of any class
*#extractRenderState=render
```

Manual class renames are trusted as written. A manual member rename is used only when the new name exists in the target; otherwise the item stays in the report. Run the script again after editing the file.

## Limits

- Only Minecraft and the `com.mojang` libraries are compared. Fabric API, Sodium, Iris and other dependencies are not checked; the compiler reports those.
- Rewriting is text-based: imports, simple names, fully qualified names (`.`, `$`, `/` forms), `name(`, `::name`, `.name` and names inside mixin strings. A new simple name can clash with another import. The compiler catches that.
- Java language level is not checked. 1.20.5–1.21.11 run on Java 21, 1.18–1.20.4 on Java 17 and 1.16.5 on Java 8. Build with the target's `--release` to find newer language and JDK APIs.
- Behaviour changes with the same signatures (render order, threading, shaders, resources, data components) are not visible to the script.

## Version modules (`versions/`)

`versions/` is a separate Gradle build with one module per game version (`versions/26.3`). Each module compiles Vela's own `src/`, rewritten for that version, so the shared code is written once:

```bash
cd versions
./gradlew :26.3:build       # needs JAVA_HOME at a JDK 25 for 26.x
./gradlew :26.3:runClient
```

A module folder holds:

- `gradle.properties`: `minecraft_version`, `minecraft_range` (written into `fabric.mod.json`), `java_release`, `loom` (`fabric-loom` on 26.x, `fabric-loom-remap` on 1.21.11 and older), the loader, Fabric API and optional Sodium/Iris/ImmediatelyFast versions.
- `port.txt`: the renames the report found (`renames.txt` of `build/port/<target>/`).
- `manual.txt`: hand-made changes, applied after `port.txt`.
- `drop.txt` (optional): globs of root files this version does not compile, e.g. `dev/sqwat/vela/render/vk/**`.
- `src/`: files that replace the root file at the same path (and new files, such as `dev/sqwat/vela/compat/` shims). They are copied as written; renames and patches do not touch them.

`portSources` (`tools/port_mappings.py rewrite`) writes the result to `build/ported/`. Mixin configs keep only the mixins whose class still exists, and the module's own `@Mixin` classes join the `client` list. Tests run in `build/testroot/`, which holds the ported tree under `src/`, so the tests that read source files and mixin lists check this version's sources.

`manual.txt` lines:

```
# class rename (Java names; `$` for nested classes)
class org.lwjgl.glfw.GLFW=dev.sqwat.vela.compat.GLFW
# member rename in one file
member dev/sqwat/vela/Foo.java methods oldName=newName
# a package that no longer exists: its `import a.*;` lines go
gone com.mojang.blaze3d.vulkan
# text replacement in one file; the build fails when <old> no longer matches
patch dev/sqwat/vela/opt/hud/HudGpu.java main.useDepth => main.hasDepth()
# the same as a Python regular expression (re.subn; \1 in the replacement)
patchre dev/sqwat/vela/Foo.java \.bar\((\w+)\) => .baz(\1)
# `*` instead of a path: every compiled file; fails only when no file matches
patch * InputConstants.Type.KEYSYM => InputConstants.Type.KEYBOARD
```

An empty replacement (`=> ` at the end of the line) deletes the text. Class renames also cover descriptors in mixin strings (`(ILa/b/Old;)V`), but method names in `method = "..."` change only through `member` or `patch` lines. Tests that check vanilla structure (the `*TargetsTest` and `*ContractTest` classes) catch most mixin targets that are gone, so update those tests together with the mixin.

### Mixin audit

`MixinTargetAuditTest` reads every `*mixins*.json` and checks each mixin against the game classes on the test classpath, so a version module learns about a broken target from `build`, not from a crash at launch:

- the target class, `@Shadow` fields and methods, and `@Accessor`/`@Invoker` targets exist;
- every `method = "..."` selector matches (with several selectors one match is enough; `*` and `/regex/` selectors are skipped, `require = 0` injectors too);
- `@At` `INVOKE`/`FIELD`/`NEW` targets are still in the method;
- an `@Inject` handler takes all target arguments or none, then the `CallbackInfo`. A bare name targets every overload (bridge methods aside), so it has to fit each one; give a descriptor when a version adds an overload;
- a `@WrapOperation` handler captures only arguments the target method has.

Only `net/minecraft` and `com/mojang` targets are reported missing; mixins into other mods stay optional.

### 26.3 notes

- 26.3 compiles shaders to SPIR-V. `compat/SpirvGlsl` (applied by `SpirvGlslMixin` to `velaclient:` and `velaopt:` shaders, files and the custom sky) adds `layout(location = N)` to every stage input and output and turns `gl_VertexID` into `gl_VertexIndex`. Varyings take their location from their name (`texCoord` is 0, as in vanilla `screenquad.vsh`); a new varying name goes into `SpirvGlsl.VARYINGS`.
- `drop.txt` drops `LevelRendererOutlineMixin`: 26.3 already skips the outline pass when nothing glows.

### 1.21.11 (work in progress)

`versions/1.21.11` holds `wip.txt`, so the build leaves it out unless `-Pwip` is given:

```bash
cd versions
./gradlew -Pwip :1.21.11:compileJava   # JDK 25 compiles it with --release 21
```

It builds with `fabric-loom-remap` and Mojang's mappings, and `manual.txt` already replaces the unnamed variables (`_`) that Java 21 lacks. The first full compile gives about 1080 errors. Most come from APIs that 26.x added:

- the GUI: `minecraft.gui.screen()`/`setScreen` (1.21.11: `minecraft.screen`/`minecraft.setScreen`), the `Hud` class, `GuiGraphics` `text`/`centeredText`/`item` (1.21.11: `drawString`/`drawCenteredString`/`renderItem`) and the `extractRenderState`/`extractBackground` screen methods (1.21.11: `render`/`renderBackground`);
- rendering: render states (`LightmapRenderState`, `gameRenderState()`), `ModelFeatureRenderer$Submit`, `BlendFactor`, `GpuFormat`, `ColorTargetState`, bind groups, the Vulkan classes, `GuiItemAtlas`, `Projection`, `FeatureFrameContext`;
- the friends service (`FriendsService`, `RemoteFriendListUpdateHandler`).

Files with the most errors: `opt/bench/Bench`, `opt/gl/OcclusionCull`, Mod Menu's `ModsScreen`, `opt/cpu/CorePin`, `opt/OptScreen`, the shader renderers and the HUD caches. Next steps: `patch *` lines for the GUI calls, `compat/` classes for the rest, and `drop.txt` for the GPU optimizations that need 26.x (frame buffer cache, Vulkan rings, GPU scale, HUD GPU cache).

### Layers (`base.txt`)

A module with a `base.txt` builds on the layers it names, one per line, lowest first (e.g. `26.3`, then `neoforge`). A layer is a folder like a module; it needs no `gradle.properties` (`versions/neoforge` has none, so Gradle does not build it on its own). The base layers' `port.txt`, `manual.txt` and `drop.txt` apply first, then the module's own. The base module's `src/` files are copied in with the upper layer's rules applied. A file in a higher layer's `src/` replaces the same path below, and a higher `drop.txt` drops files of either. Patches of a lower layer for a file that a higher layer replaces or drops are skipped. Every layer's own `@Mixin` classes join the mixin config of their package.

### NeoForge (`versions/26.2-neoforge`, `versions/26.3-neoforge`)

`loom=neoforge` in `gradle.properties` builds the module with ModDevGradle (`moddev_version` in `versions/gradle.properties`) against NeoForge `neoforge_version`. The NeoForge code is the shared layer `versions/neoforge`, written against Vela's own game version (26.2): `26.2-neoforge` builds on it alone, and `26.3-neoforge` builds on `26.3`, then on it. What differs per version sits in the module:

- `26.2-neoforge/manual.txt`: on NeoForge 26.2 Bobby's sky fog hook targets the `addSkyPass` overload with the model-view matrix.
- `26.3-neoforge/.../accesstransformer.cfg`: 26.3 moved `RenderPipeline` to `com.mojang.renderpearl`.

```bash
cd versions
./gradlew :26.2-neoforge:build :26.3-neoforge:build
./gradlew :26.3-neoforge:runClient
```

Jars are named `vela-client-<version>+<game>-<loader>.jar`.

- Vela's Fabric API calls stay in the shared code. `manual.txt` renames `net.fabricmc.` to `dev.sqwat.vela.neoforge.shim.` everywhere, and the shims there have the same names and signatures. Only the parts Vela uses exist.
- `neoforge/NeoMod` is the `@Mod` class. It registers the events and the configuration screen for the NeoForge mod list. NeoForge builds mods before the game client exists, so `mixin/neoforge/ClientStartMixin` runs the Fabric entrypoints (Vela, the bundled Bobby and IAS) where Fabric does, right after `Minecraft.instance` is set. That is before the options register keys and before mod loading finishes, where the payloads are registered.
- `StorageVisibilityMixin` targets the 5-argument `tryExtractRenderState` (with `Frustum`); the 4-argument one only delegates to it on NeoForge.
- `neoforge/NeoEvents` runs the shims' listeners from NeoForge events: ticks, screens, keys, HUD layers, chat, connection and payloads.
- `neoforge/ClientCommandBridge` keeps Vela's client commands in a dispatcher of their own. Each top-level name goes into NeoForge's dispatcher as a literal plus the rest of the line.
- `mixin/neoforge/ClientInteractionMixin` fires the attack, use-block and block-break callbacks where Fabric API fires them, before the packet is sent. NeoForge's `AttackEntityEvent` comes too late for friend protection.
- `META-INF/accesstransformer.cfg` opens what Fabric API's transitive access wideners open and Vela uses. Copying all of Fabric's list breaks NeoForge's game recompile, because protected methods made public clash with their overrides.
- NeoForge draws the hotbar group as separate GUI layers. `neoforge/NeoHud` moves each one by the hotbar's Drag Mode offset, and the menu transition wraps `ClientHooks.extractScreen`.
- Mod Menu, its mixins and `fabric.mod.json` are dropped. `META-INF/neoforge.mods.toml` lists the mixin configs.
