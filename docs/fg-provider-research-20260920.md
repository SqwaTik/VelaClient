# Minecraft frame-generation provider review

Reviewed 2026-09-20. Published feature descriptions are not local compatibility results.

## Preferred next integration test

Salt's Anti-Aliasing 1.0 targets Fabric Minecraft 26.2, matching Vela. Modrinth version `S30QlPdX` was downloaded to `work/.tools/fg-downloads/salts-anti-aliasing-1.0.jar` and its SHA-512 verified against the version API:

`a00299a07c984fff2c540c923e756b7facc17eff57c4c4cae075be9b082c928a4d54edc779333b51b2765ab557e7dfe68780735b3980de681f4563bfc319f506`

The archive contains the FidelityFX Vulkan runtime and JNI bridge, and requires Minecraft 26.2, Fabric Loader >=0.19.3 and Fabric API. Vela's configured loader 0.19.5 satisfies that declared constraint. No compatibility run has occurred.

The `fabric-26.2` source branch was inspected at commit `4f5e93ef407b0fe695fd1fb7e0c1643957d09d51`. Native code implements replacement swapchain creation, queue reservation, generated-present counters and shutdown. This provides a more direct path to an in-game trial than completing a new bridge from scratch. It is not proof that the published binary matches every source detail or works with Vela.

Licensing conflict: Modrinth labels the project MIT, but source `LICENSE.txt`, source metadata and published JAR metadata specify All Rights Reserved. Do not copy/rebrand source or bundle it into Vela pending clarification. An unchanged separately downloaded release is the proposed local test dependency.

The reviewed motion-vector shader reconstructs motion from scene depth and camera matrices. It does not supply per-entity motion in that pass. Moving players, combat, particles and disocclusion therefore need explicit visual testing. Documentation also requires Improved Transparency to be disabled for AA modes. Default mode is OFF and default sharpening is zero.

## Other candidates

- FrameBridge: Minecraft 26.2 Vulkan, but requires a legitimate separate Lossless Scaling installation; proprietary DLL not bundled. Experimental and All Rights Reserved. Not a self-contained free provider.
- Oslo-Render: advertises FSR3.1 FG on 26.2, but public claims alone do not establish correctness; All Rights Reserved. No local binary test.
- Radiance: replaces the world renderer and lists frame generation as TODO. Not a drop-in FG provider for this task.
- MC-DLSSFG: current primary target Forge 1.20.1; documented DLSS FG testing on supported NVIDIA hardware, generally RTX40+. Not the first candidate for Fabric26.2/RTX3050.

## Test gate

Use the unchanged Salt's release as a reversible optional dependency, initially OFF. Check launch, menus, Vela HUD and RP reload before enabling FSR3+FG. Confirm actual generated presents using provider telemetry, not Minecraft's FPS counter. Compare Native AA/FG off and Native AA/FG on at identical world, camera, resolution and frame cap. Separate upscaling gains from interpolation gains. Record original/presented frame rates, frame-time tails and latency. Test resize, backend fallback and disabling FG. Do not auto-enable based only on GPU model or API availability.

The existing 31ms standalone result is neither a compatibility verdict nor an in-game performance prediction. It remains useful as a warning requiring measurement.

Sources:
- https://modrinth.com/mod/salts-anti-aliasing/version/1.0
- https://github.com/Salt-and-Fries/Salt-s-Anti-Aliasing/tree/fabric-26.2
- https://www.curseforge.com/minecraft/mc-mods/framebridge-vulkan-frame-generation
- https://www.curseforge.com/minecraft/mc-mods/oslo-render
- https://github.com/Minecraft-Radiance/Radiance
- https://github.com/Tunanodra/MC-DLSSFG
