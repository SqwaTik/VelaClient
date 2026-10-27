package dev.sqwat.vela.mixin.entity;
import dev.sqwat.vela.module.visual.DeathReplay;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Replay ghosts never tick on their own: their state comes only from the recording (no drifting or leg twitching on pause). */
@Mixin(ClientLevel.class)
abstract class DeathRecapTickMixin {
    @Inject(method="tickNonPassenger",at=@At("HEAD"),cancellable=true)
    private void vela$freezeGhost(Entity entity,CallbackInfo ci){if(DeathReplay.isGhost(entity))ci.cancel();}
}
