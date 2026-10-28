package dev.sqwat.vela.mixin.entity;
import dev.sqwat.vela.module.visual.HitColor;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
abstract class HitColorMixin {
    private static boolean vela$hurt(HitColor h,LivingEntityRenderState state){return h.tints(state instanceof AvatarRenderState,state.hasRedOverlay,state.deathTime);}
    @Inject(method="getModelTint",at=@At("RETURN"),cancellable=true)
    private void vela$tint(LivingEntityRenderState state,CallbackInfoReturnable<Integer> cir){
        var h=HitColor.current();if(h!=null&&vela$hurt(h,state))cir.setReturnValue(net.minecraft.util.ARGB.multiply(cir.getReturnValue(),h.color()));
    }
    @Inject(method="getOverlayCoords",at=@At("RETURN"),cancellable=true)
    private static void vela$overlay(LivingEntityRenderState state,float white,CallbackInfoReturnable<Integer> cir){
        var h=HitColor.current();
        if(h!=null&&vela$hurt(h,state))cir.setReturnValue(OverlayTexture.pack(OverlayTexture.u(white),OverlayTexture.v(false)));
    }
}
