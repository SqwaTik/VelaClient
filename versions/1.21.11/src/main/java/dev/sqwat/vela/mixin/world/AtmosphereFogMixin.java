package dev.sqwat.vela.mixin.world;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import dev.sqwat.vela.render.shader.ShaderCamera;
import dev.sqwat.vela.render.shader.WorldShaderRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import org.joml.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** 1.21.11: renderLevel takes the camera and its view rotation; the shader passes read them through ShaderCamera. */
@Mixin(LevelRenderer.class)
abstract class AtmosphereFogMixin {
    @org.spongepowered.asm.mixin.Shadow @org.spongepowered.asm.mixin.Final private net.minecraft.client.renderer.state.LevelRenderState levelRenderState;
    @Inject(method="renderLevel",at=@At("HEAD"))
    private void vela$beginSky(GraphicsResourceAllocator allocator,DeltaTracker delta,boolean outline,Camera camera,Matrix4f view,Matrix4f projection,Matrix4f culling,GpuBufferSlice fog,Vector4f color,boolean sky,CallbackInfo ci){dev.sqwat.vela.render.shader.SkyShaderRenderer.beginFrame(ShaderCamera.capture(camera,view));}
    @Inject(method="renderLevel",at=@At("RETURN"))
    private void vela$fog(GraphicsResourceAllocator allocator,DeltaTracker delta,boolean outline,Camera camera,Matrix4f view,Matrix4f projection,Matrix4f culling,GpuBufferSlice fog,Vector4f color,boolean sky,CallbackInfo ci){var state=ShaderCamera.capture(camera,view);WorldShaderRenderer.render(state,levelRenderState.skyRenderState);dev.sqwat.vela.render.effect.HitBubbleRenderer.render(state);}
    @Inject(method={"close","setLevel"},at=@At("HEAD"))
    private void vela$release(CallbackInfo ci){WorldShaderRenderer.close();dev.sqwat.vela.render.shader.HandShaderRenderer.close();dev.sqwat.vela.render.shader.SkyShaderRenderer.close();dev.sqwat.vela.render.effect.HitBubbleRenderer.close();}
}
