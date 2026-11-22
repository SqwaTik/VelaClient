package dev.sqwat.vela;

import com.mojang.renderpearl.api.textures.GpuTexture;
import dev.sqwat.vela.render.shader.ShaderTextureUsage;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 26.3 version: pipelines and command encoders moved to renderpearl, so only the usage flags are checked here. */
class ShaderTextureUsageTest {
    @Test void alphaMasksClearToTransparentRatherThanJomlDefault() {
        assertEquals(1f,new org.joml.Vector4f().w);
        assertEquals(new org.joml.Vector4f(0f),ShaderTextureUsage.transparentBlack());
    }
    @Test void clearedTargetsSupportTransferRenderAndSampling(){
        for(int flag:new int[]{GpuTexture.USAGE_COPY_DST,GpuTexture.USAGE_RENDER_ATTACHMENT,GpuTexture.USAGE_TEXTURE_BINDING})
            assertEquals(flag,ShaderTextureUsage.CLEARED_TARGET & flag);
    }
}
