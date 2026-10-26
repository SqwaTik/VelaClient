package dev.sqwat.vela.mixin.world;

import dev.sqwat.vela.module.visual.Ambience;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
abstract class AmbienceMixin {
    private Ambience vela$ambience(){return (Object)this==Minecraft.getInstance().level?Ambience.current():null;}
    @Inject(method="getRainLevel",at=@At("RETURN"),cancellable=true)
    private void vela$rain(float partial,CallbackInfoReturnable<Float> cir){var a=vela$ambience();if(a!=null)cir.setReturnValue(a.rain(cir.getReturnValue()));}
    @Inject(method="getThunderLevel",at=@At("RETURN"),cancellable=true)
    private void vela$thunder(float partial,CallbackInfoReturnable<Float> cir){var a=vela$ambience();if(a!=null)cir.setReturnValue(a.thunder(cir.getReturnValue()));}
}
