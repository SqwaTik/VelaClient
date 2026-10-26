package dev.sqwat.vela.mixin;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
abstract class FakePlayerDamageMixin {
    @Inject(method="hurtOrSimulate",at=@At("HEAD"),cancellable=true)
    private void vela$localHealth(DamageSource source,float amount,CallbackInfoReturnable<Boolean> ci){
        var fake=dev.sqwat.vela.module.FakePlayer.current();
        if(fake!=null){
            Boolean accepted=fake.receiveLocalHit((Entity)(Object)this,source,amount);
            if(accepted!=null)ci.setReturnValue(accepted);
        }
    }
}
