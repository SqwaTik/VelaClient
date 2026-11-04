package dev.sqwat.vela.mixin.world;
import dev.sqwat.vela.module.visual.Ambience;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** 1.21.11 has no world clocks: the overworld's time of day is the client level's day time. */
@Mixin(ClientLevel.ClientLevelData.class)
abstract class AmbienceClockMixin {
    @Inject(method="getDayTime",at=@At("RETURN"),cancellable=true)
    private void vela$time(CallbackInfoReturnable<Long> cir){
        var level=Minecraft.getInstance().level;
        if(level==null||level.getLevelData()!=(Object)this||!level.dimension().equals(Level.OVERWORLD))return;
        var a=Ambience.current();if(a!=null)cir.setReturnValue(a.clock(cir.getReturnValue()));
    }
}
