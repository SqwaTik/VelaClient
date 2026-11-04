package dev.sqwat.vela.mixin.world;
import dev.sqwat.vela.compat.OverworldClock;
import dev.sqwat.vela.module.visual.Ambience;
import net.minecraft.client.ClientClockManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** 26.3: Ambience's fixed time for the overworld clock (see {@link AmbienceClockMixin}). */
@Mixin(ClientClockManager.ClientClockInstance.class)
abstract class AmbienceClockInstanceMixin implements OverworldClock {
    @Unique private boolean vela$overworld;
    @Override public void vela$overworld(boolean overworld){vela$overworld=overworld;}
    @Inject(method="totalTicks",at=@At("RETURN"),cancellable=true)
    private void vela$time(CallbackInfoReturnable<Long> cir){
        if(!vela$overworld)return;
        var a=Ambience.current();if(a!=null)cir.setReturnValue(a.clock(cir.getReturnValue()));
    }
}
