package dev.sqwat.vela.mixin.entity;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.sqwat.vela.render.entity.EntityOcclusion;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.List;
/** Runs after vanilla's frustum check accepted the entity, so only on-screen candidates are traced (1.21.5: LevelRenderer.collectVisibleEntities). */
@Mixin(LevelRenderer.class)
abstract class EntityOcclusionMixin {
    @Inject(method="collectVisibleEntities",at=@At("HEAD"))
    private void vela$beginOcclusion(Camera camera,Frustum frustum,List<Entity> list,CallbackInfoReturnable<Boolean> ci){EntityOcclusion.beginFrame(camera);}
    @WrapOperation(method="collectVisibleEntities",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;shouldRender(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/culling/Frustum;DDD)Z"))
    private boolean vela$occlusion(EntityRenderDispatcher dispatcher,Entity entity,Frustum frustum,double x,double y,double z,Operation<Boolean> original){
        return original.call(dispatcher,entity,frustum,x,y,z)&&EntityOcclusion.decide(entity)!=EntityOcclusion.Decision.HIDE;
    }
    @Inject(method="collectVisibleEntities",at=@At("TAIL"))
    private void vela$endOcclusion(CallbackInfoReturnable<Boolean> ci){EntityOcclusion.endEntities();}
}
