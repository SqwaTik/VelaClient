package dev.sqwat.vela.render;

import com.mojang.blaze3d.textures.GpuTexture;

/** Native encoder clears are transfer writes, including depth clears. */
public final class ShaderTextureUsage {
    public static final int CLEARED_TARGET = GpuTexture.USAGE_RENDER_ATTACHMENT
        | GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_COPY_DST;
    private ShaderTextureUsage() {}
    /** JOML's no-argument Vector4f has w=1, which makes alpha masks opaque. */
    public static org.joml.Vector4f transparentBlack() { return new org.joml.Vector4f(0f); }
}
