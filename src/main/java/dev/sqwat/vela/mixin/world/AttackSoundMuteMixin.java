package dev.sqwat.vela.mixin.world;
import dev.sqwat.vela.module.utility.HitSound;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Positional sounds, whether sent by the server or played locally, all end in ClientLevel.playSound. */
@Mixin(ClientLevel.class)
abstract class AttackSoundMuteMixin {
    @Inject(method="playSound(DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FFZJ)V",at=@At("HEAD"),cancellable=true)
    private void vela$muteAttack(double x,double y,double z,SoundEvent sound,SoundSource source,float volume,float pitch,boolean distanceDelay,long seed,CallbackInfo ci){
        if(HitSound.mutes(sound,x,y,z))ci.cancel();
    }
}
