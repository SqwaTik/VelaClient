package dev.sqwat.vela.mixin.world;
import dev.sqwat.vela.compat.OverworldClock;
import net.minecraft.client.ClientClockManager;
import net.minecraft.core.Holder;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.clock.WorldClocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** 26.3 version: clock time is read from the instances, so the overworld's instance is marked here. */
@Mixin(ClientClockManager.class)
abstract class AmbienceClockMixin {
    @Inject(method="getInstance(Lnet/minecraft/core/Holder;)Lnet/minecraft/client/ClientClockManager$ClientClockInstance;",at=@At("RETURN"))
    private void vela$mark(Holder<WorldClock> clock,CallbackInfoReturnable<ClientClockManager.ClientClockInstance> cir){
        ((OverworldClock)cir.getReturnValue()).vela$overworld(clock.is(WorldClocks.OVERWORLD));
    }
}
