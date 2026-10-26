# Renderer optimization review — Minecraft 26.2

Reference: ZakoOpt 1.0.1, supplied source archive; author Zako, https://t.me/StarikZako.

The original 1.0.1 review produced an independent cache for redundant OpenGL texture-buffer attachments. A subsequent partial integration of supplied ZakoOpt 1.0.3 sources is described below; adapted code is LGPL-3.0-only with notices and source included in the JAR.

## Native capabilities

The resolved Minecraft 26.2 sources already provide persistent-mapped transient memory when supported by the device. `GlFence` refers to a submit index; `GlCommandEncoder.submit()` owns the actual GPU fence. Adding another deferred fence layer would duplicate this mechanism and could wait on the wrong submit.

## Vela attachment cache

Only an identical texture/format/buffer tuple in the same render pass may skip `glTexBuffer`. New passes clear the cache. It stores one tuple and allocates no objects per draw. Ordinary texture binding still runs. Sodium, ImmediatelyFast, Iris, and ZakoOpt disable this cache to avoid overlapping renderer ownership. `-Dvela.disableTextureBufferCache=true` disables it for comparisons or driver troubleshooting.

Sodium and ImmediatelyFast are not prerequisites for this implementation. Their chunk renderer, batching, and UI integration are not embedded. This does not reproduce the full ZakoOpt feature set or its published performance results.

## ZakoOptimization module (1.0.3 integration)

Utilities now contains an opt-in master switch with the standard Vela settings UI and configuration persistence. Sections: entities, particles, render. Included controls: distant entity layers (8–96 blocks), player layers, one dropped-stack model, spawner display culling (4–64 blocks), per-tick particle light, simplified particle collision, early vanilla block-entity distance rejection, and the OpenGL texture-buffer cache. Disabling the module restores the original paths; the pre-existing Vela visibility optimizations remain separate.

Entity LOD intentionally removes distant armour, held items, capes and other layers, not the entity itself. Particle collision simplification changes smoke and distant particles visually. Both are optional. No HUD framebuffer caching, reduced HUD refresh rate, parallel workers, skin atlas, shared FBO, or ring buffer is added in this partial integration.

Vulkan compatibility in the supplied upstream source is conditional disabling of OpenGL hooks when VulkanMod is present, not a bundled Vulkan renderer. Vela's new entity/particle/block hooks use native game APIs. The texture-buffer cache is disabled with VulkanMod. Live Vulkan compatibility and frame-time improvements have not been verified.

Compare the same scene with the module off/on using p95/p99 frame times before attributing any FPS improvement to it. Use separate tests for LOD quality and texture-buffer caching. In-game startup and visual tests are outstanding because launching/controlling the game is not authorized.
