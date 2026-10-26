package dev.sqwat.vela.mixin.entity;
import dev.sqwat.vela.render.entity.EntityOcclusion;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Runs after vanilla frustum and section checks accepted the entity, so only on-screen candidates are traced. */
@Mixin(LevelExtractor.class)
abstract class EntityOcclusionMixin {
    @Inject(method="extract",at=@At("HEAD"))
    private void vela$beginOcclusion(DeltaTracker delta,Camera camera,float partial,CallbackInfo ci){EntityOcclusion.beginFrame(camera);}
    @Inject(method="isEntityVisible",at=@At("RETURN"),cancellable=true)
    private void vela$occlusion(Entity entity,Frustum frustum,double x,double y,double z,CallbackInfoReturnable<Boolean> cir){
        if(cir.getReturnValueZ()&&EntityOcclusion.decide(entity)==EntityOcclusion.Decision.HIDE)cir.setReturnValue(false);
    }
    @Inject(method="extractVisibleEntities",at=@At("TAIL"))
    private void vela$endOcclusion(CallbackInfo ci){EntityOcclusion.endEntities();}
}
