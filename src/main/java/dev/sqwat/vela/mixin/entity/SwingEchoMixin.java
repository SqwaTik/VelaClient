package dev.sqwat.vela.mixin.entity;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Many servers send the player's own swing back. Vanilla then restarts the arm halfway through the swing; Swing
 * Animation ignores those echoes so its animation is not cut off.
 */
@Mixin(ClientPacketListener.class)
abstract class SwingEchoMixin {
    @Inject(method="handleAnimate",at=@At("HEAD"))
    private void vela$echoStart(ClientboundAnimatePacket packet,CallbackInfo ci){
        // The first call comes from the network thread and is bounced to the client thread; only that one counts.
        if(net.minecraft.client.Minecraft.getInstance().isSameThread())dev.sqwat.vela.module.visual.SwingAnimation.serverEcho=true;}
    @Inject(method="handleAnimate",at=@At("RETURN"))
    private void vela$echoEnd(ClientboundAnimatePacket packet,CallbackInfo ci){dev.sqwat.vela.module.visual.SwingAnimation.serverEcho=false;}
}
