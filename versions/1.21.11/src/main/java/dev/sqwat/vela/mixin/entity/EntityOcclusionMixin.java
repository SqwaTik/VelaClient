package dev.sqwat.vela.mixin.entity;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.sqwat.vela.render.entity.EntityOcclusion;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.state.LevelRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Runs after vanilla's frustum check accepted the entity, so only on-screen candidates are traced (1.21.11: LevelRenderer.extractVisibleEntities). */
@Mixin(LevelRenderer.class)
abstract class EntityOcclusionMixin {
    @Inject(method="extractVisibleEntities",at=@At("HEAD"))
    private void vela$beginOcclusion(Camera camera,Frustum frustum,DeltaTracker delta,LevelRenderState state,CallbackInfo ci){EntityOcclusion.beginFrame(camera);}
    @WrapOperation(method="extractVisibleEntities",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;shouldRender(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/culling/Frustum;DDD)Z"))
    private boolean vela$occlusion(EntityRenderDispatcher dispatcher,Entity entity,Frustum frustum,double x,double y,double z,Operation<Boolean> original){
        return original.call(dispatcher,entity,frustum,x,y,z)&&EntityOcclusion.decide(entity)!=EntityOcclusion.Decision.HIDE;
    }
    @Inject(method="extractVisibleEntities",at=@At("TAIL"))
    private void vela$endOcclusion(CallbackInfo ci){EntityOcclusion.endEntities();}
}
