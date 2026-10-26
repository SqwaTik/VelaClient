# Frame generation prototype

## Implemented

`/vela fg inspect` performs an explicit read-only query against Minecraft's current GPU backend. On Vulkan it reports the physical device name, Vulkan API version, graphics/compute queue families and RG16F/RGBA16F storage-image support. It does not create a device, load a native library, submit GPU work or enable frame generation. Reported format support is not an SDK compatibility decision.

## Integration boundary

The interpolator must use Minecraft's physical device and a verified synchronization contract. Texture handles alone are insufficient: image layouts, ownership, completion semaphores, resource lifetime and swapchain recreation must be handled explicitly. Never reuse resources after reload, resize or device loss.

World color, depth and motion vectors must be captured before HUD composition. Camera-only vectors are insufficient for moving entities. The native implementation must present generated frames without multiplying game ticks or input events. OpenGL requires its own verified interop path; it cannot reuse Vulkan image handles.

## Acceptance gates

- Compare matching scenes with FG off/on after warm-up, not different worlds or menu FPS.
- Record native frame rate separately from actual presented frame rate, p95/p99 frame times, GPU time, VRAM and input latency.
- Test stationary/moving camera, combat, dense entities, transparent geometry, menus, chat, HUD and server resource packs.
- Validate resize, minimize, world changes, resource reload, backend fallback and repeated enable/disable.
- Keep automatic activation disabled until measured benefit and visual quality are established. Missing telemetry or a native error must not be treated as success.

## Selected prototype

FSR 3.1 on Vulkan was approved on 2026-09-19. The initial API reference is the official FidelityFX SDK v1.1.4, which contains the Vulkan frame-generation swapchain backend. Its swapchain context can destroy and replace the supplied swapchain; integration must therefore cover Minecraft's surface lifecycle, not merely call an interpolation dispatch. The locally compiled SDK has been loaded only in standalone probes, not Minecraft.

## Remaining

## Toolchain checkpoint

The official Vulkan SDK was installed in `work/.tools/vulkan-sdk` with `copy_only=1`; its installer signature was valid (LunarG). FidelityFX SDK v1.1.4 was fetched from the official repository at commit `c6efa6bf7f2027b3ec94f28578bb5965eabb9e55`, retaining its license. Visual Studio 2026 Build Tools and MSVC 19.51 successfully compiled the Vulkan SDK backend and both probes on 2026-09-19. `native/build-fsr.ps1` checks the upstream revision and the required local descriptor-pool correction.

`vela-fg-probe` successfully enumerated the frame-interpolation provider (internal version 1.1.3). `vela-fg-device-probe` reports hardware features and formats. With an explicit absolute DLL path and device index it creates a dedicated Vulkan 1.3 device and a 640x360 frame-generation context, then destroys both. NVIDIA RTX 3050 6GB Laptop passed this lifecycle test with create/destroy return codes 0. The final run with `VK_LAYER_KHRONOS_validation` emitted no warnings or errors. Intel was enumerated only; its FG context was not tested.

Two issues were found during the smoke test:

- FidelityFX resolves `vkGetBufferMemoryRequirements2KHR`; the standalone device must explicitly enable the KHR memory extensions. Core Vulkan 1.3 support alone did not prevent the initial test-process failure.
- Upstream `sdk/src/backends/vk/ffx_vk.cpp` declares six descriptor-pool entries but sets `poolSizeCount = 5`, omitting storage buffers. The local one-line correction derives the count from `sizeof(poolSizes) / sizeof(poolSizes[0])`. This eliminated the validation warning. Preserve this correction when refreshing the SDK checkout.

The test enables supported core features on its own device; it does not prove Minecraft enabled them. The prototype has not been installed over the working client.

## Compute/readback checkpoint (2026-09-20)

`dispatch_probe.cpp` now allocates color/depth/motion/output images, runs configure/prepare/frame-generation dispatch, submits GPU commands and checks every output RGB pixel against a constant-color reference (tolerance 3/255). Forty frames passed, including the initial reset and subsequent history frames, at both 640x360 and 1920x1080. Vulkan validation emitted no warnings/errors on the final runs. This proves the static compute path, not moving-object interpolation or presentation quality.

GPU timestamps bracket prepare and generation, excluding readback and game rendering. Eight warm-up frames are discarded; 32 samples remain. These short synthetic measurements are not a Minecraft benchmark:

| Resolution | Validation | Median GPU ms | p95 GPU ms |
| --- | --- | --- | --- |
| 640x360 | enabled | 0.524288 | 1.89952 |
| 1920x1080 | enabled | 33.708 | 47.6897 |
| 1920x1080 | not requested | 30.702 | 36.5681 |

The Full HD cost is unacceptable for automatic activation at high native FPS. A nearby NVIDIA telemetry snapshot showed P8, 210 MHz core and 405 MHz memory; this snapshot does not establish clock state throughout the timed dispatches. Repeat with synchronized telemetry and representative sustained rendering before attributing the cost to the algorithm or deciding compatibility. No power/clock/driver settings were modified.

Remaining gates: patterned/moving synthetic inputs and motion-vector verification, native Minecraft bridge, HUD separation, swapchain synchronization, then matched in-game off/on measurements including latency and p95/p99. No frames have been presented by the prototype and no in-game benefit has been measured. Automatic activation remains disabled.

References:
- https://github.com/NVIDIA-RTX/Streamline/blob/main/docs/ProgrammingGuideDLSS_G.md
- https://gpuopen.com/manuals/fidelityfx_sdk/techniques/super-resolution-interpolation/
- https://github.com/sdli1995/dlssg_for_sm86/blob/main/README.en.md
