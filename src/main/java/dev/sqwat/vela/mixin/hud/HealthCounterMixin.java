package dev.sqwat.vela.mixin.hud;

import dev.sqwat.vela.module.visual.HpCounter;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.syncher.EntityDataAccessor;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
abstract class HealthCounterMixin {
    @Inject(method={"setHealth","setAbsorptionAmount"},at=@At("HEAD"))
    private void vela$beforeHealth(float value,CallbackInfo ci){
        if((Object)this instanceof Player player)HpCounter.baseline(player);
    }
    @Inject(method="onSyncedDataUpdated",at=@At("TAIL"))
    private void vela$health(EntityDataAccessor<?> key,CallbackInfo ci){
        if((Object)this instanceof Player player)HpCounter.changed(player);
    }
}
