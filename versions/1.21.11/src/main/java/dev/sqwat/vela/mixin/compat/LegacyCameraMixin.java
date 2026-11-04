package dev.sqwat.vela.mixin.compat;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import dev.sqwat.vela.compat.LegacyCamera;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Records the level's view rotation, projection and cull frustum for {@link LegacyCamera}. */
@Mixin(LevelRenderer.class)
abstract class LegacyCameraMixin {
    @Inject(method="renderLevel",at=@At("HEAD"))
    private void vela$camera(GraphicsResourceAllocator allocator,DeltaTracker delta,boolean outline,Camera camera,Matrix4f viewRotation,Matrix4f projection,Matrix4f cullProjection,GpuBufferSlice fog,Vector4f fogColor,boolean sky,CallbackInfo ci){
        LegacyCamera.capture(viewRotation,projection);
    }
    @Inject(method="prepareCullFrustum",at=@At("RETURN"))
    private void vela$frustum(Matrix4f modelView,Matrix4f projection,net.minecraft.world.phys.Vec3 position,CallbackInfoReturnable<Frustum> cir){LegacyCamera.captureFrustum(cir.getReturnValue());}
}
