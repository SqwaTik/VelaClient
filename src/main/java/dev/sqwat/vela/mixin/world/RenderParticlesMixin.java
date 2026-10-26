package dev.sqwat.vela.mixin.world;
import net.minecraft.client.particle.ParticleEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ParticleEngine.class)
public abstract class RenderParticlesMixin {
    @Inject(method="extract",at=@At("HEAD"),cancellable=true)
    private void vela$particles(CallbackInfo ci){if(dev.sqwat.vela.module.utility.RenderTweaks.hide("particles"))ci.cancel();}
}
