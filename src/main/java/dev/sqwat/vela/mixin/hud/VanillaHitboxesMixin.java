package dev.sqwat.vela.mixin.hud;

import net.minecraft.client.renderer.debug.EntityHitboxDebugRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** With the Hitboxes module on, F3+B shows Vela's boxes instead of these white ones. */
@Mixin(EntityHitboxDebugRenderer.class)
abstract class VanillaHitboxesMixin {
    @Inject(method="emitGizmos",at=@At("HEAD"),cancellable=true)
    private void vela$replaced(CallbackInfo ci){
        if(dev.sqwat.vela.module.visual.Hitboxes.current()!=null)ci.cancel();
    }
}
