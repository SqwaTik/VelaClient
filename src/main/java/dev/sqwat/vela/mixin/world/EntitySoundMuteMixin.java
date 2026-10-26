package dev.sqwat.vela.mixin.world;
import dev.sqwat.vela.module.utility.HitSound;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Entity-attached server sounds bypass ClientLevel.playSound; filter them here. */
@Mixin(ClientPacketListener.class)
abstract class EntitySoundMuteMixin {
    @Inject(method="handleSoundEntityEvent",at=@At("HEAD"),cancellable=true)
    private void vela$muteEntitySound(ClientboundSoundEntityPacket packet,CallbackInfo ci){
        if(HitSound.mutesEntity(packet.getSound().value(),packet.getId()))ci.cancel();
    }
}
