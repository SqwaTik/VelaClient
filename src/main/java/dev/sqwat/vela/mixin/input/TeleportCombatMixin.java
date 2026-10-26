package dev.sqwat.vela.mixin.input;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ClientPacketListener.class)
abstract class TeleportCombatMixin {
    @Inject(method="handleDamageEvent",at=@At("TAIL"))
    private void vela$combat(ClientboundDamageEventPacket packet,CallbackInfo ci){dev.sqwat.vela.module.utility.AutoTpAccept.damage(packet);}
}
