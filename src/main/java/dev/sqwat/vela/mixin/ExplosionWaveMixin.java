package dev.sqwat.vela.mixin;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
abstract class ExplosionWaveMixin {
    // TAIL runs after vanilla's client-thread handoff and explosion handling.
    @Inject(method="handleExplosion",at=@At("TAIL"))
    private void vela$wave(ClientboundExplodePacket packet,CallbackInfo ci){
        dev.sqwat.vela.module.ExplosionWave.exploded(packet.center());
    }
}
