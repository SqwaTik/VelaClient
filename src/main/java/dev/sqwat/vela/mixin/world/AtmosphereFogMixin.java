package dev.sqwat.vela.mixin.world;
import dev.sqwat.vela.render.shader.WorldShaderRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.client.DeltaTracker;
import org.joml.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(LevelRenderer.class)
abstract class AtmosphereFogMixin {
    @org.spongepowered.asm.mixin.Shadow @org.spongepowered.asm.mixin.Final private net.minecraft.client.renderer.state.level.LevelRenderState levelRenderState;
    @Inject(method="render",at=@At("HEAD"))
    private void vela$beginSky(GraphicsResourceAllocator allocator,DeltaTracker delta,boolean outline,CameraRenderState camera,Matrix4fc matrix,GpuBufferSlice fog,Vector4f color,boolean sky,CallbackInfo ci){dev.sqwat.vela.render.shader.SkyShaderRenderer.beginFrame(camera);}
    @Inject(method="render",at=@At("RETURN"))
    private void vela$fog(GraphicsResourceAllocator allocator,DeltaTracker delta,boolean outline,CameraRenderState camera,Matrix4fc matrix,GpuBufferSlice fog,Vector4f color,boolean sky,CallbackInfo ci){WorldShaderRenderer.render(camera,levelRenderState.skyRenderState);dev.sqwat.vela.render.effect.HitBubbleRenderer.render(camera);}
    @Inject(method={"close","resetLevelRenderData"},at=@At("HEAD"))
    private void vela$release(CallbackInfo ci){WorldShaderRenderer.close();dev.sqwat.vela.render.shader.HandShaderRenderer.close();dev.sqwat.vela.render.shader.SkyShaderRenderer.close();dev.sqwat.vela.render.effect.HitBubbleRenderer.close();}
}
