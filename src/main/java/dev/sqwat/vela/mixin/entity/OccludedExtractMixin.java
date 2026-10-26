package dev.sqwat.vela.mixin.entity;
import dev.sqwat.vela.render.entity.EntityOcclusion;
import dev.sqwat.vela.render.entity.OccludedRenderState;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Pooled states are rewritten every extraction, so the flag is always assigned, never only set. */
@Mixin(EntityRenderDispatcher.class)
abstract class OccludedExtractMixin {
    @Inject(method="extractEntity",at=@At("RETURN"))
    private void vela$markOccluded(Entity entity,float partial,CallbackInfoReturnable<EntityRenderState> cir){
        var state=cir.getReturnValue();if(state==null)return;
        boolean hidden=EntityOcclusion.modelHidden(entity);
        ((OccludedRenderState)state).vela$occluded(hidden);
        if(hidden){state.shadowRadius=0;state.shadowPieces.clear();state.displayFireAnimation=false;}
    }
}
