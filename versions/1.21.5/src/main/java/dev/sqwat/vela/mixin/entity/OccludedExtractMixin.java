package dev.sqwat.vela.mixin.entity;
import dev.sqwat.vela.render.entity.EntityOcclusion;
import dev.sqwat.vela.render.entity.OccludedRenderState;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** 1.21.5: each renderer reuses one state, so the flag is always assigned; the shadow radius is asked of the renderer. */
@Mixin(EntityRenderer.class)
abstract class OccludedExtractMixin {
    @Inject(method="createRenderState(Lnet/minecraft/world/entity/Entity;F)Lnet/minecraft/client/renderer/entity/state/EntityRenderState;",at=@At("RETURN"))
    private void vela$markOccluded(Entity entity,float partial,CallbackInfoReturnable<EntityRenderState> cir){
        var state=cir.getReturnValue();if(state==null)return;
        boolean hidden=EntityOcclusion.modelHidden(entity);
        ((OccludedRenderState)state).vela$occluded(hidden);
        if(hidden)state.displayFireAnimation=false;
    }
    @Inject(method="getShadowRadius",at=@At("HEAD"),cancellable=true)
    private void vela$noShadow(EntityRenderState state,CallbackInfoReturnable<Float> cir){if(((OccludedRenderState)state).vela$occluded())cir.setReturnValue(0f);}
}
