package dev.sqwat.vela.mixin;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
abstract class FakePlayerKnockbackMixin {
    @Inject(method="getKnockback",at=@At("RETURN"),cancellable=true)
    private void vela$localEnchantedKnockback(Entity target,DamageSource source,CallbackInfoReturnable<Float> ci){
        var fake=dev.sqwat.vela.module.FakePlayer.current();
        if(fake!=null&&fake.attacking(target)){
            var attacker=(LivingEntity)(Object)this;
            int level=dev.sqwat.vela.module.LocalCombatRules.level(attacker.getWeaponItem(),net.minecraft.world.item.enchantment.Enchantments.KNOCKBACK);
            ci.setReturnValue(ci.getReturnValue()+level*.5f);
        }
    }
}
