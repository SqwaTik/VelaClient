package dev.sqwat.vela.mixin.entity;
import dev.sqwat.vela.module.visual.DeathReplay;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Real copies of replayed entities stay hidden while their ghosts play. */
@Mixin(EntityRenderDispatcher.class)
abstract class DeathRecapVisibilityMixin {
    @Inject(method="shouldRender",at=@At("HEAD"),cancellable=true)
    private <E extends Entity> void vela$hideReplayed(E entity,Frustum frustum,double x,double y,double z,CallbackInfoReturnable<Boolean> cir){
        if(DeathReplay.active()&&DeathReplay.hides(entity))cir.setReturnValue(false);
    }
}
