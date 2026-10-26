package dev.sqwat.vela;

import com.mojang.blaze3d.textures.GpuTexture;
import dev.sqwat.vela.render.shader.ShaderTextureUsage;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ShaderTextureUsageTest {
    @Test void handOverlaysBlendWithExistingFrameButGlassReplacesMaskedPixels() throws Exception {
        for(String name:new String[]{"HOLOGRAM","PLAIN"}) {
            var field=dev.sqwat.vela.render.shader.HandShaderRenderer.class.getDeclaredField(name);
            field.setAccessible(true);
            var pipeline=(com.mojang.blaze3d.pipeline.RenderPipeline)field.get(null);
            assertTrue(pipeline.getColorTargetState().blendFunction().isPresent());
        }
        var glass=dev.sqwat.vela.render.shader.HandShaderRenderer.class.getDeclaredField("GLASS");
        glass.setAccessible(true);
        assertTrue(((com.mojang.blaze3d.pipeline.RenderPipeline)glass.get(null)).getColorTargetState().blendFunction().isEmpty());
    }
    @Test void alphaMasksClearToTransparentRatherThanJomlDefault() {
        assertEquals(1f,new org.joml.Vector4f().w);
        assertEquals(new org.joml.Vector4f(0f),ShaderTextureUsage.transparentBlack());
    }
    @Test void wetLightingPassMatchesFloatingPointAttachment() throws Exception {
        var field=dev.sqwat.vela.render.shader.WorldShaderRenderer.class.getDeclaredField("WET");
        field.setAccessible(true);
        var pipeline=(com.mojang.blaze3d.pipeline.RenderPipeline)field.get(null);
        assertEquals(com.mojang.blaze3d.GpuFormat.RGBA16_FLOAT,pipeline.getColorTargetState().format());
        var resolve=dev.sqwat.vela.render.shader.WorldShaderRenderer.class.getDeclaredField("WET_RESOLVE");
        resolve.setAccessible(true);
        assertEquals(com.mojang.blaze3d.GpuFormat.RGBA8_UNORM,((com.mojang.blaze3d.pipeline.RenderPipeline)resolve.get(null)).getColorTargetState().format());
    }
    private static GpuTexture texture(int usage,com.mojang.blaze3d.GpuFormat format){
        return new GpuTexture(usage,"test",format,16,16,1,1){
            @Override public void close(){}
            @Override public boolean isClosed(){return false;}
        };
    }
    @Test void nativeEncoderRejectsOldFlagsAndAcceptsFixedColorAndDepth(){
        var backend=(com.mojang.blaze3d.systems.CommandEncoderBackend)java.lang.reflect.Proxy.newProxyInstance(
            getClass().getClassLoader(),new Class<?>[]{com.mojang.blaze3d.systems.CommandEncoderBackend.class},(proxy,method,args)->null);
        var encoder=new com.mojang.blaze3d.systems.CommandEncoder(null,null,backend);
        int oldFlags=GpuTexture.USAGE_RENDER_ATTACHMENT|GpuTexture.USAGE_TEXTURE_BINDING;
        var oldColor=texture(oldFlags,com.mojang.blaze3d.GpuFormat.RGBA8_UNORM);
        assertTrue(assertThrows(IllegalStateException.class,()->encoder.clearColorTexture(oldColor,new org.joml.Vector4f())).getMessage().contains("USAGE_COPY_DST"));
        var color=texture(ShaderTextureUsage.CLEARED_TARGET,com.mojang.blaze3d.GpuFormat.RGBA8_UNORM);
        var depth=texture(ShaderTextureUsage.CLEARED_TARGET,com.mojang.blaze3d.GpuFormat.D32_FLOAT);
        assertDoesNotThrow(()->encoder.clearColorAndDepthTextures(color,new org.joml.Vector4f(),depth,0));
    }
    @Test void clearedTargetsSupportTransferRenderAndSampling(){
        for(int flag:new int[]{GpuTexture.USAGE_COPY_DST,GpuTexture.USAGE_RENDER_ATTACHMENT,GpuTexture.USAGE_TEXTURE_BINDING})
            assertEquals(flag,ShaderTextureUsage.CLEARED_TARGET & flag);
    }
}
