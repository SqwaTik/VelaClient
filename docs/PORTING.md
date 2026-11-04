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
