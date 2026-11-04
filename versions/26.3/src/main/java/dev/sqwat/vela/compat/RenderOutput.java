package dev.sqwat.vela.compat;

import com.mojang.renderpearl.api.textures.GpuTextureView;
import org.jspecify.annotations.Nullable;

/**
 * 26.3 removed {@code RenderSystem.outputColorTextureOverride}/{@code outputDepthTextureOverride}. Vela only used them
 * to draw the first-person hands into its own targets; the 26.3 HandShaderMixin opens the hand pass on these instead.
 */
public final class RenderOutput {
    private RenderOutput() {}
    public static @Nullable GpuTextureView color, depth;
}
