package dev.sqwat.vela.mixin.entity;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.sqwat.vela.module.visual.DeathReplay;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
/** Replay ghosts never tick on their own: their state comes only from the recording (no drifting or twitching on pause). */
@Mixin(ClientLevel.class)
abstract class DeathRecapTickMixin {
    @WrapOperation(method="tickNonPassenger",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/Entity;tick()V"))
    private void vela$freezeGhost(Entity entity,Operation<Void> original){if(!DeathReplay.isGhost(entity))original.call(entity);}
}
