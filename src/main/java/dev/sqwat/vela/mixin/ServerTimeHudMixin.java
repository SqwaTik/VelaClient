package dev.sqwat.vela.mixin;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ClientPacketListener.class)
public abstract class ServerTimeHudMixin {
    @Inject(method="handlePongResponse",at=@At("TAIL"))
    private void vela$pong(net.minecraft.network.protocol.ping.ClientboundPongResponsePacket packet,CallbackInfo ci){
        dev.sqwat.vela.ui.HudTelemetry.pong((ClientPacketListener)(Object)this,packet.time());
    }
    @Inject(method="handleSetTime",at=@At("TAIL"))
    private void vela$time(ClientboundSetTimePacket packet,CallbackInfo ci){dev.sqwat.vela.ui.HudTelemetry.serverTime(packet.gameTime(),System.nanoTime());}
}
