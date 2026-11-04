package dev.sqwat.vela.mixin.compat;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import dev.sqwat.vela.compat.LegacyCamera;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Records the level's view rotation, projection and cull frustum for {@link LegacyCamera} (1.21.5 signatures). */
@Mixin(LevelRenderer.class)
abstract class LegacyCameraMixin {
    @Shadow private Frustum cullingFrustum;
    @Inject(method="renderLevel",at=@At("HEAD"))
    private void vela$camera(GraphicsResourceAllocator allocator,DeltaTracker delta,boolean outline,Camera camera,GameRenderer renderer,Matrix4f viewRotation,Matrix4f projection,CallbackInfo ci){
        LegacyCamera.capture(viewRotation,projection);
    }
    @Inject(method="prepareCullFrustum",at=@At("RETURN"))
    private void vela$frustum(net.minecraft.world.phys.Vec3 position,Matrix4f modelView,Matrix4f projection,CallbackInfo ci){LegacyCamera.captureFrustum(cullingFrustum);}
}
