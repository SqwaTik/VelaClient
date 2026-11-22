package dev.sqwat.vela.mixin.entity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 1.21.5: Player.attack plays its sounds through Level.playSound directly (no playServerSideSound). */
@Mixin(Player.class)
abstract class FakePlayerSoundMixin {
    @Inject(method="getEnchantedDamage",at=@At("HEAD"),cancellable=true)
    private void vela$localEnchantedDamage(Entity target,float damage,net.minecraft.world.damagesource.DamageSource source,CallbackInfoReturnable<Float> ci){
        var fake=dev.sqwat.vela.module.utility.FakePlayer.current();
        if(fake!=null&&fake.attacking(target)){
            var player=(Player)(Object)this;
            ci.setReturnValue(damage+dev.sqwat.vela.module.utility.LocalCombatRules.sharpness(dev.sqwat.vela.module.utility.LocalCombatRules.level(player.getWeaponItem(),net.minecraft.world.item.enchantment.Enchantments.SHARPNESS)));
        }
    }
    @WrapOperation(method="attack",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/Level;playSound(Lnet/minecraft/world/entity/Entity;DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V"))
    private void vela$localAttackSound(Level level,Entity source,double x,double y,double z,SoundEvent sound,SoundSource category,float volume,float pitch,Operation<Void> original){
        var fake=dev.sqwat.vela.module.utility.FakePlayer.current();
        if(fake==null||!fake.playAttackSound((Player)(Object)this,sound))original.call(level,source,x,y,z,sound,category,volume,pitch);
    }
    @WrapOperation(method="attack",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/Level;playSound(Lnet/minecraft/world/entity/Entity;DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;)V"))
    private void vela$localAttackSoundShort(Level level,Entity source,double x,double y,double z,SoundEvent sound,SoundSource category,Operation<Void> original){
        var fake=dev.sqwat.vela.module.utility.FakePlayer.current();
        if(fake==null||!fake.playAttackSound((Player)(Object)this,sound))original.call(level,source,x,y,z,sound,category);
    }
}
