package dev.sqwat.vela.mixin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.sounds.SoundEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
abstract class FakePlayerSoundMixin {
    @Inject(method="getEnchantedDamage",at=@At("HEAD"),cancellable=true)
    private void vela$localEnchantedDamage(net.minecraft.world.entity.Entity target,float damage,net.minecraft.world.damagesource.DamageSource source,CallbackInfoReturnable<Float> ci){
        var fake=dev.sqwat.vela.module.FakePlayer.current();
        if(fake!=null&&fake.attacking(target)){
            var player=(Player)(Object)this;
            ci.setReturnValue(damage+dev.sqwat.vela.module.LocalCombatRules.sharpness(dev.sqwat.vela.module.LocalCombatRules.level(player.getWeaponItem(),net.minecraft.world.item.enchantment.Enchantments.SHARPNESS)));
        }
    }
    @Inject(method="playServerSideSound",at=@At("HEAD"),cancellable=true)
    private void vela$localAttackSound(SoundEvent sound,CallbackInfo ci){
        var fake=dev.sqwat.vela.module.FakePlayer.current();
        if(fake!=null&&fake.playAttackSound((Player)(Object)this,sound))ci.cancel();
    }
}
