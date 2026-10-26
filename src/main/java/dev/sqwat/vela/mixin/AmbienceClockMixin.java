package dev.sqwat.vela.mixin;
import dev.sqwat.vela.module.Ambience;
import net.minecraft.client.ClientClockManager;
import net.minecraft.core.Holder;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.clock.WorldClocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ClientClockManager.class)
abstract class AmbienceClockMixin {
    @Inject(method="getTotalTicks",at=@At("RETURN"),cancellable=true)
    private void vela$time(Holder<WorldClock> clock,CallbackInfoReturnable<Long> cir){
        if(!clock.is(WorldClocks.OVERWORLD))return;
        var a=Ambience.current();if(a!=null)cir.setReturnValue(a.clock(cir.getReturnValue()));
    }
}
