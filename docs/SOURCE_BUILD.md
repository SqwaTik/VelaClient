# Building Vela Client 26.2

Requirements: JDK 25. Minecraft, Fabric Loader, Fabric API and Loom versions are pinned in `gradle.properties`; the Gradle Wrapper is included.

Build and test:

    ./gradlew test build        (gradlew.bat test build on Windows)

Output: `build/libs/vela-client-<version>.jar` and `build/libs/vela-client-<version>-sources.jar`.
`./gradlew sourceBundle` creates this source archive (`build/distributions/vela-client-source.zip`).

All bundled third-party code (In-Game Account Switcher, Bobby, Mod Menu) is included as source in `src/main/java`; no extra libraries are needed. Sodium and Iris are optional integrations whose compile-only dependencies Gradle downloads automatically. See `THIRD_PARTY.md` for licenses and the list of changes.

To use a modified build, replace the Vela jar in your mods folder with the one you built.
