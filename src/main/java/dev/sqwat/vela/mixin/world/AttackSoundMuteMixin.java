package dev.sqwat.vela.mixin.world;
import dev.sqwat.vela.module.utility.HitSound;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Drops vanilla attack sounds the server plays at the local player while Hit Sound replaces them. */
@Mixin(ClientPacketListener.class)
abstract class AttackSoundMuteMixin {
    @Inject(method="handleSoundEvent",at=@At("HEAD"),cancellable=true)
    private void vela$muteAttack(ClientboundSoundPacket packet,CallbackInfo ci){
        if(HitSound.mutes(packet.getSound().value(),packet.getX(),packet.getY(),packet.getZ()))ci.cancel();
    }
    @Inject(method="handleSoundEntityEvent",at=@At("HEAD"),cancellable=true)
    private void vela$muteEntitySound(net.minecraft.network.protocol.game.ClientboundSoundEntityPacket packet,CallbackInfo ci){
        if(HitSound.mutesEntity(packet.getSound().value(),packet.getId()))ci.cancel();
    }
}
