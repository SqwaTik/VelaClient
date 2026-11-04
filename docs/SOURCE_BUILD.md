# Building Vela Client

Requirements: JDK 25. Minecraft, Fabric Loader, Fabric API and Loom versions are pinned in `gradle.properties`; the Gradle Wrapper is included.

Build and test:

    ./gradlew test build        (gradlew.bat test build on Windows)

Output: `build/libs/vela-client-<version>.jar` and `build/libs/vela-client-<version>-sources.jar`.
`./gradlew sourceBundle` creates this source archive (`build/distributions/vela-client-source.zip`).

All bundled third-party code (In-Game Account Switcher, Bobby, Mod Menu) is included as source in `src/main/java`; no extra libraries are needed. Sodium and Iris are optional integrations whose compile-only dependencies Gradle downloads automatically. See `THIRD_PARTY.md` for licenses and the list of changes.

To use a modified build, replace the Vela jar in your mods folder with the one you built.

## Other versions and NeoForge

Minecraft 26.3, 1.21.11, 1.21.5 and 1.21.4 on Fabric, and every version on NeoForge, are built by the separate Gradle build in `versions/` (branch `port/multiversion`). Each module rewrites the shared `src/` for its version; see `docs/PORTING.md`. 26.x needs JDK 25, 1.21.x builds with Java 21 bytecode on the same JDK.

    ./gradlew -p versions assemble

Output: `versions/<module>/build/libs/vela-client-*.jar`, for example `versions/1.21.4-neoforge/build/libs/`.
